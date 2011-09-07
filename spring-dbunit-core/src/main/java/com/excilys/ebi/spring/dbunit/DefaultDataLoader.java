/*
 * Copyright 2010-2011 eBusiness Information, Groupe Excilys (www.excilys.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.excilys.ebi.spring.dbunit;

import java.sql.Connection;

import javax.sql.DataSource;

import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.operation.DatabaseOperation;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.datasource.DataSourceUtils;

import com.excilys.ebi.spring.dbunit.config.DataSetConfiguration;
import com.excilys.ebi.spring.dbunit.config.DatabaseConnectionConfigurer;
import com.excilys.ebi.spring.dbunit.config.Phase;

/**
 * @author <a href="mailto:slandelle@excilys.com">Stephane LANDELLE</a>
 */
public class DefaultDataLoader implements DataLoader {

	public void execute(ApplicationContext context, DataSetConfiguration dataSetConfiguration, Phase phase) throws Exception {

		if (dataSetConfiguration != null) {
			DataSource dataSource = lookUpDataSource(context, dataSetConfiguration);
			DatabaseOperation operation = phase.getOperation(dataSetConfiguration);
			IDataSet dataSet = dataSetConfiguration.getDataSet();
			executeOperation(operation, dataSet, dataSetConfiguration, dataSource);
		}
	}

	/**
	 * @param testContext
	 *            the testContext
	 * @return the DataSource used for loading the DataSet. If a name is
	 *         specified in the configuration, use it, otherwise, expect one and
	 *         only one DataSource in the ApplicationContext
	 */
	private DataSource lookUpDataSource(ApplicationContext applicationContext, DataSetConfiguration configuration) {
		return configuration.getDataSourceSpringName() != null ? applicationContext.getBean(configuration.getDataSourceSpringName(), DataSource.class) : applicationContext
				.getBean(DataSource.class);
	}

	/**
	 * Execute a DBUbit operation
	 */
	private void executeOperation(DatabaseOperation operation, IDataSet dataSet, DatabaseConnectionConfigurer databaseConnectionConfigurer, DataSource dataSource) throws Exception {

		Connection connection = null;

		try {
			connection = DataSourceUtils.getConnection(dataSource);

			DatabaseConnection databaseConnection = getDatabaseConnection(connection, databaseConnectionConfigurer);

			operation.execute(databaseConnection, dataSet);

		} finally {
			if (connection != null && !DataSourceUtils.isConnectionTransactional(connection, dataSource)) {
				// if the connection is transactional, closing it. Otherwise,
				// expects that the framework will do it
				DataSourceUtils.releaseConnection(connection, dataSource);
			}
		}
	}

	private DatabaseConnection getDatabaseConnection(Connection connection, DatabaseConnectionConfigurer databaseConnectionConfigurer) throws DatabaseUnitException {

		DatabaseConnection databaseConnection = new DatabaseConnection(connection);
		DatabaseConfig databaseConfig = databaseConnection.getConfig();
		databaseConnectionConfigurer.configure(databaseConfig);

		return databaseConnection;
	}
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.integration;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.extension.db.integration.DbTestUtil.selectData;
import static org.mule.extension.db.integration.TestRecordUtil.assertRecords;
import org.mule.extension.db.integration.model.AbstractTestDatabase;
import org.mule.extension.db.integration.model.Field;
import org.mule.extension.db.integration.model.Record;
import org.mule.extension.db.internal.DbConnector;
import org.mule.extension.db.internal.domain.connection.DbConnectionProvider;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

//@RunnerDelegateTo(Parameterized.class)
//@ArtifactClassLoaderRunnerConfig(exportClasses = {DbConnectionProvider.class})
@RunWith(Parameterized.class)
public abstract class AbstractDbIntegrationTestCase extends ExtensionFunctionalTestCase {

  private final String dataSourceConfigResource;
  protected final AbstractTestDatabase testDatabase;

  public AbstractDbIntegrationTestCase(String dataSourceConfigResource, AbstractTestDatabase testDatabase) {
    this.dataSourceConfigResource = dataSourceConfigResource;
    this.testDatabase = testDatabase;
  }

  @Before
  public void configDB() throws SQLException {
    testDatabase.createDefaultDatabaseConfig(getDefaultDataSource());
  }

  @Override protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[]{DbConnector.class};
  }

  protected DataSource getDefaultDataSource() {
    try {
      ConfigurationProvider configurationProvider = muleContext.getRegistry().get("dbConfig");
      DbConnectionProvider connectionProvider =
          (DbConnectionProvider) configurationProvider.get(getTestEvent("")).getConnectionProvider().get();
      return connectionProvider.getDataSource();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }

  @Override
  protected final String[] getConfigFiles() {
    StringBuilder builder = new StringBuilder();

    builder.append(getDatasourceConfigurationResource());

    for (String resource : getFlowConfigurationResources()) {
      if (builder.length() != 0) {
        builder.append(",");
      }

      builder.append(resource);
    }

    return builder.toString().split(",");
  }

  protected final String getDatasourceConfigurationResource() {
    return dataSourceConfigResource;
  }

  protected abstract String[] getFlowConfigurationResources();

  protected void assertPlanetRecordsFromQuery(String... names) throws SQLException {
    if (names.length == 0) {
      throw new IllegalArgumentException("Must provide at least a name to query on the DB");
    }

    StringBuilder conditionBuilder = new StringBuilder();
    List<Record> records = new ArrayList<>(names.length);

    for (String name : names) {
      if (conditionBuilder.length() != 0) {
        conditionBuilder.append(",");
      }
      conditionBuilder.append("'").append(name).append("'");
      records.add(new Record(new Field("NAME", name)));
    }

    List<Map<String, String>> result =
        selectData(String.format("select * from PLANET where name in (%s)", conditionBuilder.toString()), getDefaultDataSource());

    assertRecords(result, records.toArray(new Record[0]));
  }

  protected void assertDeletedPlanetRecords(String... names) throws SQLException {
    if (names.length == 0) {
      throw new IllegalArgumentException("Must provide at least a name to query on the DB");
    }

    StringBuilder conditionBuilder = new StringBuilder();

    for (String name : names) {
      if (conditionBuilder.length() != 0) {
        conditionBuilder.append(",");
      }
      conditionBuilder.append("'").append(name).append("'");
    }

    List<Map<String, String>> result =
        selectData(String.format("select * from PLANET where name in (%s)", conditionBuilder.toString()), getDefaultDataSource());
    assertThat(result.size(), equalTo(0));
  }

}

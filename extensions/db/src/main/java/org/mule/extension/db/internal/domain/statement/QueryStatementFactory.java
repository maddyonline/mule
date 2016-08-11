/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.domain.statement;

import static org.mule.extension.db.internal.domain.query.QueryType.STORE_PROCEDURE_CALL;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.extension.db.internal.domain.autogeneratedkey.AutoGeneratedKeyStrategy;
import org.mule.extension.db.internal.domain.autogeneratedkey.NoAutoGeneratedKeyStrategy;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.domain.query.QueryTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates {@link Statement} based on the type and parameters of a given query:
 *
 * _CallableStatement for stored p rocedure queries
 * _PreparedStatement for queries with input parameters
 * _Standard Statement otherwise
 */
public class QueryStatementFactory implements ConfigurableStatementFactory {

  private int maxRows;

  /**
   * There is no int value to indicate that the property was not set, so
   * using null to indicate that
   */
  private Integer fetchSize;

  private int queryTimeout = 0;

  @Override
  public Statement create(DbConnection connection, QueryTemplate queryTemplate) throws SQLException {
    return create(connection, queryTemplate, new NoAutoGeneratedKeyStrategy());
  }

  @Override
  public Statement create(DbConnection connection, QueryTemplate queryTemplate, AutoGeneratedKeyStrategy autoGeneratedKeyStrategy)
      throws SQLException {
    Statement result;

    if (queryTemplate.getType().equals(STORE_PROCEDURE_CALL)) {
      result = connection.getJdbcConnection().prepareCall(queryTemplate.getSqlText(), ResultSet.TYPE_FORWARD_ONLY,
                                                          ResultSet.CONCUR_READ_ONLY);
    } else {
      boolean hasInputParams = queryTemplate.getInputParams().size() > 0;

      if (hasInputParams) {
        result = autoGeneratedKeyStrategy.prepareStatement(connection, queryTemplate);
      } else {
        result = connection.getJdbcConnection().createStatement();
      }
    }

    if (maxRows > 0) {
      result.setMaxRows(maxRows);
    }

    if (fetchSize != null) {
      result.setFetchSize(fetchSize);
    }

    if (queryTimeout != 0) {
      result.setQueryTimeout(queryTimeout);
    }

    return result;
  }

  @Override
  public void setMaxRows(int max) {
    this.maxRows = max;
  }

  @Override
  public void setFetchSize(int size) {
    this.fetchSize = size;
  }

  @Override
  public void setQueryTimeout(int queryTimeout) {
    checkArgument(queryTimeout >= 0, "Query timeout must be positive");
    this.queryTimeout = queryTimeout;
  }
}

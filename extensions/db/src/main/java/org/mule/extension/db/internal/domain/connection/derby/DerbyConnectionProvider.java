/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.domain.connection.derby;

import org.mule.extension.db.internal.domain.connection.AbstractVendorConnectionProvider;
import org.mule.runtime.extension.api.annotation.Alias;

import java.sql.SQLException;

import javax.sql.DataSource;

@Alias("derby")
public class DerbyConnectionProvider extends AbstractVendorConnectionProvider {

  private static final String DRIVER_CLASS_NAME = "org.apache.derby.jdbc.EmbeddedDriver";

  @Override
  protected DataSource createDataSource() throws SQLException {
    connectionParameters.getDataSourceConfig().setDriverClassName(DRIVER_CLASS_NAME);
    return super.createDataSource();
  }
}

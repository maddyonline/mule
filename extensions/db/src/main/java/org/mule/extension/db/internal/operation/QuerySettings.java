/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.operation;

import static java.util.concurrent.TimeUnit.SECONDS;
import org.mule.extension.db.api.param.QueryDefinition;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.util.concurrent.TimeUnit;

public class QuerySettings {


  /**
   * Indicates the minimum amount of time before the JDBC driver attempts to cancel a running statement.
   * No timeout is used by default.
   */
  @Parameter
  @Optional(defaultValue = "0")
  private int queryTimeout = 0;

  /**
   * A {@link TimeUnit} which qualifies the {@link #queryTimeout}
   */
  @Parameter
  @Optional(defaultValue = "SECONDS")
  private TimeUnit queryTimeoutUnit = SECONDS;

  @Parameter
  @Optional
  @XmlHints(allowInlineDefinition = false)
  private QueryDefinition template;

  public int getQueryTimeout() {
    return queryTimeout;
  }

  public TimeUnit getQueryTimeoutUnit() {
    return queryTimeoutUnit;
  }

  public QueryDefinition getTemplate() {
    return template;
  }
}

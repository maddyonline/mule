/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api.param;

import static com.google.common.collect.ImmutableList.copyOf;
import static org.mule.extension.db.api.param.QueryType.PARAMETERIZED;
import org.mule.extension.db.internal.domain.param.InputQueryParam;
import org.mule.extension.db.internal.operation.QuerySettings;
import org.mule.runtime.core.util.collection.ImmutableMapCollector;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.Text;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Alias("query")
public class QueryDefinition {

  @Parameter
  @Optional
  @Text
  private String sql;

  @Parameter
  @Optional(defaultValue = "PARAMETERIZED")
  private QueryType queryType = PARAMETERIZED;

  @Parameter
  @Optional
  private List<QueryParameter> parameters = new LinkedList<>();

  @ParameterGroup
  private QuerySettings settings = new QuerySettings();

  public QueryDefinition copy() {
    QueryDefinition copy = new QueryDefinition();
    copy.sql = sql;
    copy.parameters = new LinkedList<>(parameters);
    copy.queryType = queryType;

    return copy;
  }

  public String getSql() {
    return sql;
  }

  public QueryType getQueryType() {
    return queryType;
  }

  public List<QueryParameter> getParameters() {
    return copyOf(parameters);
  }

  public QuerySettings getSettings() {
    return settings;
  }

  public Map<String, Object> getParameterValues() {
    return parameters.stream()
        .filter(p -> p instanceof InputQueryParam)
        .collect(new ImmutableMapCollector<>(QueryParameter::getName, p -> ((InputQueryParam) p).getValue()));
  }

  public void setSql(String sql) {
    this.sql = sql;
  }

}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api.param;

import static com.google.common.collect.ImmutableList.copyOf;
import org.mule.extension.db.internal.domain.param.InputQueryParam;
import org.mule.runtime.core.util.collection.ImmutableMapCollector;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Alias("parameterized-query")
public class ParameterizedQueryDefinition extends QueryDefinition {

  @Parameter
  @Optional
  private List<QueryParameter> parameters = new LinkedList<>();

  public List<QueryParameter> getParameters() {
    return copyOf(parameters);
  }

  @Override
  public QueryDefinition resolveFromTemplate() {
    ParameterizedQueryDefinition resolvedDefinition = (ParameterizedQueryDefinition) super.resolveFromTemplate();
    final ParameterizedQueryDefinition template = (ParameterizedQueryDefinition) getTemplate();

    Map<String, Object> templateParamValues = null;

    if (template != null) {
      templateParamValues = template.getParameterValues();
    }

    Map<String, Object> resolvedParameterValues = new HashMap<>();
    if (templateParamValues != null) {
      resolvedParameterValues.putAll(templateParamValues);
    }

    resolvedParameterValues.putAll(getParameterValues());

    return resolvedDefinition;
  }

  @Override
  protected QueryDefinition copy() {
    ParameterizedQueryDefinition copy = (ParameterizedQueryDefinition) super.copy();
    copy.parameters = new LinkedList<>(parameters);

    return copy;
  }

  public Map<String, Object> getParameterValues() {
    return parameters.stream()
        .filter(p -> p instanceof InputQueryParam)
        .collect(new ImmutableMapCollector<>(QueryParameter::getParamName, p -> ((InputQueryParam) p).getValue()));
  }
}

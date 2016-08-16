/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.api.param;

import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.extension.api.introspection.parameter.ExpressionSupport.LITERAL;
import org.mule.extension.db.internal.operation.QuerySettings;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.ParameterGroup;
import org.mule.runtime.extension.api.annotation.dsl.xml.XmlHints;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.display.Text;

public abstract class QueryDefinition {

  @Parameter
  @Optional
  @Expression(LITERAL)
  @Text
  private String sql;

  @ParameterGroup
  private QuerySettings settings;

  @Parameter
  @Optional
  @XmlHints(allowInlineDefinition = false)
  private QueryDefinition template;

  public QueryDefinition resolveFromTemplate() {
    final QueryDefinition template = getTemplate();

    if (template == null) {
      return this;
    }

    if (!getClass().equals(template.getClass())) {
      throw new IllegalArgumentException(
                                         format("Invalid template specified. Cannot apply template of type '%s' on a definition of type '%s'",
                                                template.getClass().getName(), getClass().getName()));
    }

    QueryDefinition resolvedDefinition = copy();

    if (isBlank(resolvedDefinition.getSql())) {
      resolvedDefinition.setSql(template.getSql());
    }

    return resolvedDefinition;
  }

  protected QueryDefinition copy() {
    QueryDefinition copy;
    try {
      copy = getClass().newInstance();
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create instance of " + getClass().getName()), e);
    }

    copy.sql = sql;

    return copy;
  }

  public String getSql() {
    return sql;
  }

  public QuerySettings getSettings() {
    return settings;
  }

  public void setSql(String sql) {
    this.sql = sql;
  }

  public QueryDefinition getTemplate() {
    return template;
  }

}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.db.internal.resolver.query;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import org.mule.extension.db.api.param.InputParameter;
import org.mule.extension.db.api.param.ParameterizedQueryDefinition;
import org.mule.extension.db.api.param.QueryDefinition;
import org.mule.extension.db.internal.DbConnector;
import org.mule.extension.db.internal.domain.connection.DbConnection;
import org.mule.extension.db.internal.domain.param.DefaultInOutQueryParam;
import org.mule.extension.db.internal.domain.param.DefaultInputQueryParam;
import org.mule.extension.db.internal.domain.param.DefaultOutputQueryParam;
import org.mule.extension.db.internal.domain.param.InOutQueryParam;
import org.mule.extension.db.internal.domain.param.InputQueryParam;
import org.mule.extension.db.internal.domain.param.OutputQueryParam;
import org.mule.extension.db.internal.domain.param.QueryParam;
import org.mule.extension.db.internal.domain.query.Query;
import org.mule.extension.db.internal.domain.query.QueryParamValue;
import org.mule.extension.db.internal.domain.query.QueryTemplate;
import org.mule.extension.db.internal.domain.type.CompositeDbTypeManager;
import org.mule.extension.db.internal.domain.type.DbType;
import org.mule.extension.db.internal.domain.type.DbTypeManager;
import org.mule.extension.db.internal.domain.type.DynamicDbType;
import org.mule.extension.db.internal.domain.type.StaticDbTypeManager;
import org.mule.extension.db.internal.domain.type.UnknownDbType;
import org.mule.extension.db.internal.parser.QueryTemplateParser;
import org.mule.extension.db.internal.parser.SimpleQueryTemplateParser;
import org.mule.extension.db.internal.resolver.param.GenericParamTypeResolverFactory;
import org.mule.extension.db.internal.resolver.param.ParamTypeResolverFactory;

import com.google.common.collect.ImmutableList;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class DefaultQueryResolver implements QueryResolver {

  private QueryTemplateParser queryTemplateParser = new SimpleQueryTemplateParser();

  @Override
  public Query resolve(QueryDefinition queryDefinition, DbConnector connector, DbConnection connection) {

    queryDefinition = queryDefinition.resolveFromTemplate();

    checkArgument(!isBlank(queryDefinition.getSql()), "sql query cannot be blank");

    QueryTemplate queryTemplate = queryTemplateParser.parse(queryDefinition.getSql());

    if (needsParamTypeResolution(queryTemplate)) {
      Map<Integer, DbType> paramTypes = getParameterTypes(connector, connection, queryTemplate);
      queryTemplate = resolveQueryTemplate(queryTemplate, paramTypes);
    }

    return new Query(queryTemplate, paramValuesFrom(queryDefinition));
  }

  private QueryTemplate resolveQueryTemplate(QueryTemplate queryTemplate, Map<Integer, DbType> paramTypes) {
    List<QueryParam> newParams = new ArrayList<>();

    for (QueryParam originalParam : queryTemplate.getParams()) {
      DbType type = paramTypes.get(originalParam.getIndex());
      QueryParam newParam;

      if (originalParam instanceof InOutQueryParam) {
        newParam = new DefaultInOutQueryParam(originalParam.getIndex(), type, originalParam.getName(),
                                              ((InOutQueryParam) originalParam).getValue());
      } else if (originalParam instanceof InputQueryParam) {
        newParam =
            new DefaultInputQueryParam(originalParam.getIndex(), type, ((InputQueryParam) originalParam).getValue(),
                                       originalParam.getName());
      } else if (originalParam instanceof OutputQueryParam) {
        newParam = new DefaultOutputQueryParam(originalParam.getIndex(), type, originalParam.getName());
      } else {
        throw new IllegalArgumentException("Unknown parameter type: " + originalParam.getClass().getName());

      }

      newParams.add(newParam);
    }

    return new QueryTemplate(queryTemplate.getSqlText(), queryTemplate.getType(), newParams);
  }

  private Map<Integer, DbType> getParameterTypes(DbConnector connector, DbConnection connection, QueryTemplate queryTemplate) {
    ParamTypeResolverFactory paramTypeResolverFactory =
        new GenericParamTypeResolverFactory(createTypeManager(connector, connection));

    try {
      return paramTypeResolverFactory.create(queryTemplate).getParameterTypes(connection, queryTemplate);
    } catch (SQLException e) {
      throw new QueryResolutionException("Cannot resolve parameter types", e);
    }
  }

  private boolean needsParamTypeResolution(QueryTemplate template) {
    return template.getParams().stream()
        .map(QueryParam::getType)
        .anyMatch(type -> type == UnknownDbType.getInstance() || type instanceof DynamicDbType);
  }

  private DbTypeManager createTypeManager(DbConnector connector, DbConnection connection) {
    final DbTypeManager baseTypeManager = connector.getTypeManager();
    List<DbType> vendorDataTypes = connection.getVendorDataTypes();
    if (vendorDataTypes.size() > 0) {
      return new CompositeDbTypeManager(asList(baseTypeManager, new StaticDbTypeManager(connection.getVendorDataTypes())));
    }

    return baseTypeManager;
  }

  private List<QueryParamValue> paramValuesFrom(QueryDefinition definition) {
    if (definition instanceof ParameterizedQueryDefinition) {
      return ((ParameterizedQueryDefinition) definition).getParameters().stream()
          .filter(p -> p instanceof InputParameter)
          .map(p -> new QueryParamValue(p.getParamName(), ((InputParameter) p).getValue()))
          .collect(toList());
    }

    return ImmutableList.of();
  }
}

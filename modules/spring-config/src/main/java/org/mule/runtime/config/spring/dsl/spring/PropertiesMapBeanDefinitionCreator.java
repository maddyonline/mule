/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.spring;

import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.PROPERTIES_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.SPRING_ENTRY_IDENTIFIER;

import java.util.HashMap;

import org.mule.runtime.config.spring.dsl.model.ComponentModel;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;

public class PropertiesMapBeanDefinitionCreator extends BeanDefinitionCreator {

  @Override
  boolean handleRequest(CreateBeanDefinitionRequest createBeanDefinitionRequest) {
    ComponentModel componentModel = createBeanDefinitionRequest.getComponentModel();
    if (componentModel.getIdentifier().equals(SPRING_ENTRY_IDENTIFIER)) {
      return true;
    }
    if (!componentModel.getIdentifier().equals(PROPERTIES_IDENTIFIER)) {
      return false;
    }
    ManagedMap<Object, Object> managedMap = new ManagedMap<>();
    BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(HashMap.class);
    for (ComponentModel innerComponent : componentModel.getInnerComponents()) {
      Object key = resolveValue(innerComponent.getParameters().get("key"), innerComponent.getParameters().get("key-ref"));
      Object value = resolveValue(innerComponent.getParameters().get("value"), innerComponent.getParameters().get("value-ref"));
      managedMap.put(key, value);
    }
    componentModel.setBeanDefinition(beanDefinitionBuilder
        .addConstructorArgValue(managedMap)
        .getBeanDefinition());
    return true;
  }

  private Object resolveValue(String value, String reference) {
    if (value != null) {
      return value;
    }
    return new RuntimeBeanReference(reference);
  }
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.spring;

import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.MULE_PROPERTY_IDENTIFIER;
import static org.mule.runtime.config.spring.dsl.model.ApplicationModel.MULE_PROPERTIES_IDENTIFIER;
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
    if (componentModel.getIdentifier().equals(MULE_PROPERTIES_IDENTIFIER)
        || componentModel.getIdentifier().equals(MULE_PROPERTY_IDENTIFIER)) {
      ManagedMap<Object, Object> managedMap = new ManagedMap<>();
      BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.rootBeanDefinition(HashMap.class);
      if (componentModel.getIdentifier().equals(MULE_PROPERTIES_IDENTIFIER)) {
        for (ComponentModel innerComponent : componentModel.getInnerComponents()) {
          processAndAddMapProperty(innerComponent, managedMap);
        }
      } else {
        processAndAddMapProperty(componentModel, managedMap);
      }
      componentModel.setBeanDefinition(beanDefinitionBuilder
          .addConstructorArgValue(managedMap)
          .getBeanDefinition());
      return true;
    }
    return false;
  }

  private void processAndAddMapProperty(ComponentModel componentModel, ManagedMap<Object, Object> managedMap) {
    Object key = resolveValue(componentModel.getParameters().get("key"), componentModel.getParameters().get("key-ref"));
    Object value = resolveValue(componentModel.getParameters().get("value"), componentModel.getParameters().get("value-ref"));
    if (value == null) {
      value = resolveValueFromChild(componentModel.getInnerComponents().get(0));
    }
    managedMap.put(key, value);
  }

  private Object resolveValueFromChild(ComponentModel componentModel) {
    if (componentModel.getIdentifier().equals("list")) {
      return null;
    } else {
      return null;
    }
  }

  private Object resolveValue(String value, String reference) {
    if (reference != null) {
      return new RuntimeBeanReference(reference);
    }
    return value;
  }
}

/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.schema.builder;

import org.mule.runtime.extension.api.ExtensionManager;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.xml.dsl.api.resolver.DslResolvingContext;

import java.util.Optional;

/**
 * //TODO
 */
public class DefaultDslContext implements DslResolvingContext {


  private final ExtensionManager extensionManager;

  public DefaultDslContext(ExtensionManager extensionManager) {
    this.extensionManager = extensionManager;
  }

  @Override
  public Optional<ExtensionModel> getExtension(String name) {
    return extensionManager.getExtension(name).map(e -> e);
  }
}

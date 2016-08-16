/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.processor;

import org.mule.runtime.config.spring.dsl.api.ObjectFactory;
import org.mule.runtime.core.api.model.EntryPointResolver;
import org.mule.runtime.core.model.resolvers.ExplicitMethodEntryPointResolver;

import java.util.ArrayList;
import java.util.List;

public class ExplicitMethodEntryPointResolverObjectFactory implements ObjectFactory<EntryPointResolver>
{
    private boolean acceptVoidMethods;
    private List<MethodEntryPoint> methodEntryPoints = new ArrayList<>();

    @Override
    public EntryPointResolver getObject() throws Exception
    {
        ExplicitMethodEntryPointResolver explicitMethodEntryPointResolver = new ExplicitMethodEntryPointResolver();
        explicitMethodEntryPointResolver.setAcceptVoidMethods(acceptVoidMethods);
        for (MethodEntryPoint methodEntryPoint : methodEntryPoints)
        {
            explicitMethodEntryPointResolver.addMethod(methodEntryPoint.getMethod());
        }
        return explicitMethodEntryPointResolver;
    }

    public void setAcceptVoidMethods(boolean acceptVoidMethods)
    {
        this.acceptVoidMethods = acceptVoidMethods;
    }

    public void setMethodEntryPoints(List<MethodEntryPoint> methodEntryPoints)
    {
        this.methodEntryPoints = methodEntryPoints;
    }
}

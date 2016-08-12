/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.spring;

import org.mule.runtime.config.spring.dsl.api.ObjectFactory;
import org.mule.runtime.core.api.component.Component;
import org.mule.runtime.core.api.model.EntryPointResolver;
import org.mule.runtime.core.api.model.EntryPointResolverSet;
import org.mule.runtime.core.component.DefaultJavaComponent;
import org.mule.runtime.core.model.resolvers.DefaultEntryPointResolverSet;
import org.mule.runtime.core.object.PrototypeObjectFactory;

public class ComponentObjectFactory implements ObjectFactory<Component>
{
    private Class clazz;
    private org.mule.runtime.core.api.object.ObjectFactory objectFactory = new PrototypeObjectFactory();
    private EntryPointResolverSet entryPointResolverSet = new DefaultEntryPointResolverSet();
    private EntryPointResolver entryPointResolver;

    @Override
    public Component getObject() throws Exception
    {
        if (clazz !=  null)
        {
            objectFactory = new PrototypeObjectFactory(clazz);
        }
        if (entryPointResolver != null)
        {
            entryPointResolverSet.addEntryPointResolver(entryPointResolver);
        }
        if (objectFactory != null)
        {
            return new DefaultJavaComponent(objectFactory, entryPointResolverSet);
        }
        return new DefaultJavaComponent();
    }

    public void setClazz(Class clazz)
    {
        this.clazz = clazz;
    }

    public void setObjectFactory(org.mule.runtime.core.api.object.ObjectFactory objectFactory)
    {
        this.objectFactory = objectFactory;
    }

    public void setEntryPointResolverSet(EntryPointResolverSet entryPointResolverSet)
    {
        this.entryPointResolverSet = entryPointResolverSet;
    }

    public void setEntryPointResolver(EntryPointResolver entryPointResolver)
    {
        this.entryPointResolver = entryPointResolver;
    }
}

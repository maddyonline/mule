/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig.parsers.specific;

import org.mule.module.springconfig.parsers.generic.ChildDefinitionParser;
import org.mule.routing.AbstractAggregator;

/**
 * Generic router definition parser for parsing all Router elements.
 */
public class RouterDefinitionParser extends ChildDefinitionParser
{

    public static final String ROUTER = "messageProcessor";

    public RouterDefinitionParser(Class clazz)
    {
        super(ROUTER, clazz);
    }

    // specifically for subclasses of AbstractCorrelationAggregator (requires a "class=..." in the config)
    public RouterDefinitionParser()
    {
        super(ROUTER, null, AbstractAggregator.class, true);
    }

}
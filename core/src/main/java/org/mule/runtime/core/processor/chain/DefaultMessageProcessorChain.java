/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.chain;

import static org.mule.runtime.core.execution.MessageProcessorExecutionTemplate.createExecutionTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.execution.MessageProcessorExecutionTemplate;

public class DefaultMessageProcessorChain extends AbstractMessageProcessorChain {

  protected MessageProcessorExecutionTemplate messageProcessorExecutionTemplate = createExecutionTemplate();

  protected DefaultMessageProcessorChain(List<MessageProcessor> processors) {
    super(null, processors);
  }

  protected DefaultMessageProcessorChain(MessageProcessor... processors) {
    super(null, new ArrayList(Arrays.asList(processors)));
  }

  protected DefaultMessageProcessorChain(String name, List<MessageProcessor> processors) {
    super(name, processors);
  }

  protected DefaultMessageProcessorChain(String name, MessageProcessor... processors) {
    super(name, Arrays.asList(processors));
  }

  public static MessageProcessorChain from(MuleContext muleContext, MessageProcessor messageProcessor) {
    return new DefaultMessageProcessorChain(messageProcessor);
  }

  public static MessageProcessorChain from(MuleContext muleContext, MessageProcessor... messageProcessors) throws MuleException {
    return new DefaultMessageProcessorChainBuilder(muleContext).chain(messageProcessors).build();
  }

  public static MessageProcessorChain from(MuleContext muleContext, List<MessageProcessor> messageProcessors)
      throws MuleException {
    return new DefaultMessageProcessorChainBuilder(muleContext).chain(messageProcessors).build();
  }

  @Override
  protected MuleEvent doProcess(MuleEvent event) throws MuleException {
    return new ProcessorExecutorFactory().createProcessorExecutor(event, processors, messageProcessorExecutionTemplate, true)
        .execute();
  }

  @Override
  public void setMuleContext(MuleContext context) {
    super.setMuleContext(context);
    messageProcessorExecutionTemplate.setMuleContext(context);
  }
}

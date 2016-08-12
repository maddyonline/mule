/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.config.spring.dsl.model;

import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromChildCollectionConfiguration;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromChildConfiguration;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromFixedValue;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromReferenceObject;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromSimpleParameter;
import static org.mule.runtime.config.spring.dsl.api.AttributeDefinition.Builder.fromSimpleReferenceParameter;
import static org.mule.runtime.config.spring.dsl.api.TypeDefinition.fromConfigurationAttribute;
import static org.mule.runtime.config.spring.dsl.api.TypeDefinition.fromType;
import static org.mule.runtime.config.spring.dsl.processor.xml.CoreXmlNamespaceInfoProvider.CORE_NAMESPACE_NAME;
import static org.mule.runtime.core.retry.policies.SimpleRetryPolicyTemplate.RETRY_COUNT_FOREVER;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.mule.runtime.config.spring.MuleConfigurationConfigurator;
import org.mule.runtime.config.spring.NotificationConfig;
import org.mule.runtime.config.spring.ServerNotificationManagerConfigurator;
import org.mule.runtime.config.spring.dsl.api.AttributeDefinition;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinitionProvider;
import org.mule.runtime.config.spring.dsl.processor.ExpressionArgumentObjectFactory;
import org.mule.runtime.config.spring.dsl.processor.ExpressionTransformerObjectFactory;
import org.mule.runtime.config.spring.dsl.processor.IgnoreObjectMethodsObjectFactory;
import org.mule.runtime.config.spring.dsl.processor.MessageEnricherObjectFactory;
import org.mule.runtime.config.spring.dsl.processor.MessageProcessorWrapperObjectFactory;
import org.mule.runtime.config.spring.dsl.processor.RetryPolicyTemplateObjectFactory;
import org.mule.runtime.config.spring.dsl.processor.TransformerObjectFactory;
import org.mule.runtime.config.spring.factories.AsyncMessageProcessorsFactoryBean;
import org.mule.runtime.config.spring.factories.ChoiceRouterFactoryBean;
import org.mule.runtime.config.spring.factories.MessageProcessorChainFactoryBean;
import org.mule.runtime.config.spring.factories.MessageProcessorFilterPairFactoryBean;
import org.mule.runtime.config.spring.factories.PollingMessageSourceFactoryBean;
import org.mule.runtime.config.spring.factories.ResponseMessageProcessorsFactoryBean;
import org.mule.runtime.config.spring.factories.ScatterGatherRouterFactoryBean;
import org.mule.runtime.config.spring.factories.SubflowMessageProcessorChainFactoryBean;
import org.mule.runtime.config.spring.factories.TransactionalMessageProcessorsFactoryBean;
import org.mule.runtime.config.spring.factories.WatermarkFactoryBean;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationExtension;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.model.EntryPointResolver;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.api.schedule.SchedulerFactory;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.context.notification.ListenerSubscriptionPair;
import org.mule.runtime.core.context.notification.ServerNotificationManager;
import org.mule.runtime.core.enricher.MessageEnricher;
import org.mule.runtime.core.exception.CatchMessagingExceptionStrategy;
import org.mule.runtime.core.exception.ChoiceMessagingExceptionStrategy;
import org.mule.runtime.core.exception.DefaultMessagingExceptionStrategy;
import org.mule.runtime.core.exception.RedeliveryExceeded;
import org.mule.runtime.core.exception.RollbackMessagingExceptionStrategy;
import org.mule.runtime.core.expression.transformers.BeanBuilderTransformer;
import org.mule.runtime.core.expression.transformers.ExpressionArgument;
import org.mule.runtime.core.expression.transformers.ExpressionTransformer;
import org.mule.runtime.core.model.resolvers.ArrayEntryPointResolver;
import org.mule.runtime.core.model.resolvers.CallableEntryPointResolver;
import org.mule.runtime.core.model.resolvers.DefaultEntryPointResolverSet;
import org.mule.runtime.core.model.resolvers.ExplicitMethodEntryPointResolver;
import org.mule.runtime.core.model.resolvers.MethodHeaderPropertyEntryPointResolver;
import org.mule.runtime.core.model.resolvers.NoArgumentsEntryPointResolver;
import org.mule.runtime.core.model.resolvers.ReflectionEntryPointResolver;
import org.mule.runtime.core.processor.AsyncDelegateMessageProcessor;
import org.mule.runtime.core.processor.IdempotentRedeliveryPolicy;
import org.mule.runtime.core.processor.ResponseMessageProcessorAdapter;
import org.mule.runtime.core.processor.TransactionalMessageProcessor;
import org.mule.runtime.core.routing.AggregationStrategy;
import org.mule.runtime.core.routing.ChoiceRouter;
import org.mule.runtime.core.routing.FirstSuccessful;
import org.mule.runtime.core.routing.Foreach;
import org.mule.runtime.core.routing.MessageFilter;
import org.mule.runtime.core.routing.MessageProcessorFilterPair;
import org.mule.runtime.core.routing.RoundRobin;
import org.mule.runtime.core.routing.ScatterGatherRouter;
import org.mule.runtime.core.routing.UntilSuccessful;
import org.mule.runtime.core.routing.WireTap;
import org.mule.runtime.core.routing.filters.NotWildcardFilter;
import org.mule.runtime.core.routing.filters.WildcardFilter;
import org.mule.runtime.core.routing.outbound.MulticastingRouter;
import org.mule.runtime.core.routing.requestreply.SimpleAsyncRequestReplyRequester;
import org.mule.runtime.core.source.StartableCompositeMessageSource;
import org.mule.runtime.core.source.polling.MessageProcessorPollingOverride;
import org.mule.runtime.core.source.polling.PollingMessageSource;
import org.mule.runtime.core.source.polling.schedule.FixedFrequencySchedulerFactory;
import org.mule.runtime.core.source.polling.watermark.Watermark;
import org.mule.runtime.core.transaction.lookup.GenericTransactionManagerLookupFactory;
import org.mule.runtime.core.transaction.lookup.JBossTransactionManagerLookupFactory;
import org.mule.runtime.core.transaction.lookup.JRunTransactionManagerLookupFactory;
import org.mule.runtime.core.transaction.lookup.Resin3TransactionManagerLookupFactory;
import org.mule.runtime.core.transaction.lookup.WeblogicTransactionManagerLookupFactory;
import org.mule.runtime.core.transaction.lookup.WebsphereTransactionManagerLookupFactory;
import org.mule.runtime.core.transformer.AbstractTransformer;
import org.mule.runtime.core.transformer.codec.Base64Decoder;
import org.mule.runtime.core.transformer.codec.Base64Encoder;
import org.mule.runtime.core.transformer.codec.XmlEntityDecoder;
import org.mule.runtime.core.transformer.codec.XmlEntityEncoder;
import org.mule.runtime.core.transformer.compression.GZipCompressTransformer;
import org.mule.runtime.core.transformer.compression.GZipUncompressTransformer;
import org.mule.runtime.core.transformer.encryption.DecryptionTransformer;
import org.mule.runtime.core.transformer.encryption.EncryptionTransformer;
import org.mule.runtime.core.transformer.simple.AddFlowVariableTransformer;
import org.mule.runtime.core.transformer.simple.AddPropertyTransformer;
import org.mule.runtime.core.transformer.simple.AutoTransformer;
import org.mule.runtime.core.transformer.simple.BeanToMap;
import org.mule.runtime.core.transformer.simple.ByteArrayToHexString;
import org.mule.runtime.core.transformer.simple.ByteArrayToObject;
import org.mule.runtime.core.transformer.simple.ByteArrayToSerializable;
import org.mule.runtime.core.transformer.simple.CombineCollectionsTransformer;
import org.mule.runtime.core.transformer.simple.CopyPropertiesTransformer;
import org.mule.runtime.core.transformer.simple.HexStringToByteArray;
import org.mule.runtime.core.transformer.simple.MapToBean;
import org.mule.runtime.core.transformer.simple.ObjectToByteArray;
import org.mule.runtime.core.transformer.simple.ObjectToString;
import org.mule.runtime.core.transformer.simple.ParseTemplateTransformer;
import org.mule.runtime.core.transformer.simple.RemoveFlowVariableTransformer;
import org.mule.runtime.core.transformer.simple.RemovePropertyTransformer;
import org.mule.runtime.core.transformer.simple.SerializableToByteArray;
import org.mule.runtime.core.transformer.simple.SetPayloadMessageProcessor;
import org.mule.runtime.core.transformer.simple.StringAppendTransformer;

/**
 * {@link org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition} definitions for the components provided by the core
 * runtime.
 *
 * @since 4.0
 */
public class CoreComponentBuildingDefinitionProvider implements ComponentBuildingDefinitionProvider {

  private static final String MESSAGE_PROCESSORS = "messageProcessors";
  private static final String NAME = "name";
  private static final String EXCEPTION_STRATEGY = "exception-strategy";
  private static final String CATCH_EXCEPTION_STRATEGY = "catch-exception-strategy";
  private static final String WHEN = "when";
  private static final String ROLLBACK_EXCEPTION_STRATEGY = "rollback-exception-strategy";
  private static final String DEFAULT_EXCEPTION_STRATEGY = "default-exception-strategy";
  private static final String NAME_EXCEPTION_STRATEGY_ATTRIBUTE = "globalName";
  private static final String CUSTOM_EXCEPTION_STRATEGY = "custom-exception-strategy";
  private static final String CHOICE_EXCEPTION_STRATEGY = "choice-exception-strategy";
  private static final String SET_PAYLOAD = "set-payload";
  private static final String PROCESSOR_CHAIN = "processor-chain";
  private static final String PROCESSOR = "processor";
  private static final String TRANSFORMER = "transformer";
  private static final String FILTER = "filter";
  private static final String CUSTOM_PROCESSOR = "custom-processor";
  private static final String CLASS_ATTRIBUTE = "class";
  private static final String SUB_FLOW = "sub-flow";
  private static final String RESPONSE = "response";
  private static final String MESSAGE_FILTER = "message-filter";
  private static final String FLOW = "flow";
  private static final String EXCEPTION_LISTENER_ATTRIBUTE = "exceptionListener";
  private static final String SCATTER_GATHER = "scatter-gather";
  private static final String WIRE_TAP = "wire-tap";
  private static final String ENRICHER = "enricher";
  private static final String ASYNC = "async";
  private static final String TRANSACTIONAL = "transactional";
  private static final String UNTIL_SUCCESSFUL = "until-successful";
  private static final String FOREACH = "foreach";
  private static final String FIRST_SUCCESSFUL = "first-successful";
  private static final String ROUND_ROBIN = "round-robin";
  private static final String CHOICE = "choice";
  private static final String OTHERWISE = "otherwise";
  private static final String ALL = "all";
  private static final String POLL = "poll";
  private static final String REQUEST_REPLY = "request-reply";

  private static ComponentBuildingDefinition.Builder baseDefinition =
      new ComponentBuildingDefinition.Builder().withNamespace(CORE_NAMESPACE_NAME);
  private ComponentBuildingDefinition.Builder transactionManagerBaseDefinition;

  @Override
  public void init(MuleContext muleContext) {
    transactionManagerBaseDefinition = baseDefinition.copy();
  }

  @Override
  public List<ComponentBuildingDefinition> getComponentBuildingDefinitions() {

    LinkedList<ComponentBuildingDefinition> componentBuildingDefinitions = new LinkedList<>();

    AttributeDefinition messageProcessorListAttributeDefinition =
        fromChildCollectionConfiguration(MessageProcessor.class).build();
    ComponentBuildingDefinition.Builder exceptionStrategyBaseBuilder =
        baseDefinition.copy().withSetterParameterDefinition(MESSAGE_PROCESSORS, messageProcessorListAttributeDefinition)
            .withSetterParameterDefinition("globalName", fromSimpleParameter(NAME).build());
    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier(EXCEPTION_STRATEGY).withTypeDefinition(fromType(Object.class))
            .withConstructorParameterDefinition(fromSimpleReferenceParameter("ref").build()).build());
    componentBuildingDefinitions.add(exceptionStrategyBaseBuilder.copy().withIdentifier(CATCH_EXCEPTION_STRATEGY)
        .withTypeDefinition(fromType(CatchMessagingExceptionStrategy.class))
        .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(MessageProcessor.class).build())
        .withSetterParameterDefinition(WHEN, fromSimpleParameter(WHEN).build()).asPrototype().build());
    componentBuildingDefinitions.add(exceptionStrategyBaseBuilder.copy().withIdentifier(ROLLBACK_EXCEPTION_STRATEGY)
        .withTypeDefinition(fromType(RollbackMessagingExceptionStrategy.class))
        .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(MessageProcessor.class).build())
        .withSetterParameterDefinition(WHEN, fromSimpleParameter(WHEN).build())
        .withSetterParameterDefinition("maxRedeliveryAttempts", fromSimpleParameter("maxRedeliveryAttempts").build())
        .withSetterParameterDefinition("redeliveryExceeded", fromChildConfiguration(RedeliveryExceeded.class).build())
        .asPrototype().build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("on-redelivery-attempts-exceeded")
        .withTypeDefinition(fromType(RedeliveryExceeded.class))
        .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(MessageProcessor.class).build())
        .asScope().build());
    componentBuildingDefinitions.add(exceptionStrategyBaseBuilder.copy().withIdentifier(DEFAULT_EXCEPTION_STRATEGY)
        .withTypeDefinition(fromType(DefaultMessagingExceptionStrategy.class))
        .withSetterParameterDefinition(NAME_EXCEPTION_STRATEGY_ATTRIBUTE, fromSimpleParameter(NAME).build())
        .withSetterParameterDefinition("stopMessageProcessing", fromSimpleParameter("stopMessageProcessing").build())
        .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(MessageProcessor.class).build())
        .withSetterParameterDefinition("commitTxFilter", fromChildConfiguration(WildcardFilter.class).build())
        .withSetterParameterDefinition("rollbackTxFilter", fromChildConfiguration(WildcardFilter.class).build()).asPrototype()
        .build());
    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier("commit-transaction").withTypeDefinition(fromType(WildcardFilter.class))
            .withSetterParameterDefinition("pattern", fromSimpleParameter("exception-pattern").build()).build());
    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier("rollback-transaction").withTypeDefinition(fromType(NotWildcardFilter.class))
            .withSetterParameterDefinition("pattern", fromSimpleParameter("exception-pattern").build()).build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier(CUSTOM_EXCEPTION_STRATEGY)
        .withTypeDefinition(fromConfigurationAttribute(CLASS_ATTRIBUTE))
        .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(MessageProcessor.class).build())
        .asPrototype().build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier(CHOICE_EXCEPTION_STRATEGY)
        .withTypeDefinition(fromType(ChoiceMessagingExceptionStrategy.class))
        .withSetterParameterDefinition("globalName", fromSimpleParameter(NAME).build())
        .withSetterParameterDefinition("exceptionListeners",
                                       fromChildCollectionConfiguration(MessagingExceptionHandler.class).build())
        .build());
    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier(SET_PAYLOAD).withTypeDefinition(fromType(SetPayloadMessageProcessor.class))
            .withSetterParameterDefinition("value", fromSimpleParameter("value").build())
            .withSetterParameterDefinition("mimeType", fromSimpleParameter("mimeType").build())
            .withSetterParameterDefinition("encoding", fromSimpleParameter("encoding").build()).build());
    componentBuildingDefinitions
        // TODO add support for environment
        .add(createTransactionManagerDefinitionBuilder("jndi-transaction-manager", GenericTransactionManagerLookupFactory.class)
            .withSetterParameterDefinition("jndiName", fromSimpleParameter("jndiName").build()).build());
    componentBuildingDefinitions
        .add(createTransactionManagerDefinitionBuilder("weblogic-transaction-manager",
                                                       WeblogicTransactionManagerLookupFactory.class).build());
    componentBuildingDefinitions
        .add(createTransactionManagerDefinitionBuilder("jboss-transaction-manager", JBossTransactionManagerLookupFactory.class)
            .build());
    componentBuildingDefinitions
        .add(createTransactionManagerDefinitionBuilder("jrun-transaction-manager", JRunTransactionManagerLookupFactory.class)
            .build());
    componentBuildingDefinitions
        .add(createTransactionManagerDefinitionBuilder("resin-transaction-manager", Resin3TransactionManagerLookupFactory.class)
            .build());
    componentBuildingDefinitions
        .add(createTransactionManagerDefinitionBuilder("websphere-transaction-manager",
                                                       WebsphereTransactionManagerLookupFactory.class).build());
    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier(PROCESSOR).withTypeDefinition(fromType(Object.class)).build());
    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier(TRANSFORMER).withTypeDefinition(fromType(Object.class)).build());
    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier(FILTER).withTypeDefinition(fromType(Object.class)).build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier(CUSTOM_PROCESSOR)
        .withTypeDefinition(fromConfigurationAttribute(CLASS_ATTRIBUTE)).asPrototype().build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier(PROCESSOR_CHAIN)
        .withTypeDefinition(fromType(MessageProcessor.class)).withObjectFactoryType(MessageProcessorChainFactoryBean.class)
        .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(MessageProcessor.class).build())
        .asPrototype().build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier(SUB_FLOW)
        .withTypeDefinition(fromType(MessageProcessor.class)).withObjectFactoryType(SubflowMessageProcessorChainFactoryBean.class)
        .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(MessageProcessor.class).build())
        .withSetterParameterDefinition(NAME, fromSimpleParameter(NAME).build()).asPrototype().build());
    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier(RESPONSE).withTypeDefinition(fromType(ResponseMessageProcessorAdapter.class))
            .withObjectFactoryType(ResponseMessageProcessorsFactoryBean.class)
            .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(MessageProcessor.class).build())
            .build());
    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier(MESSAGE_FILTER).withTypeDefinition(fromType(MessageFilter.class))
            .withConstructorParameterDefinition(fromChildConfiguration(Filter.class).build())
            .withConstructorParameterDefinition(fromSimpleParameter("throwOnUnaccepted").withDefaultValue(false).build())
            .withConstructorParameterDefinition(fromSimpleReferenceParameter("onUnaccepted").build()).asPrototype().build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier(FLOW).withTypeDefinition(fromType(Flow.class))
        .withConstructorParameterDefinition(fromSimpleParameter(NAME).build())
        .withConstructorParameterDefinition(fromReferenceObject(MuleContext.class).build())
        .withSetterParameterDefinition("initialState", fromSimpleParameter("initialState").build())
        .withSetterParameterDefinition("messageSource", fromChildConfiguration(MessageSource.class).build())
        .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(MessageProcessor.class).build())
        .withSetterParameterDefinition(EXCEPTION_LISTENER_ATTRIBUTE,
                                       fromChildConfiguration(MessagingExceptionHandler.class).build())
        .withSetterParameterDefinition("processingStrategy", fromSimpleReferenceParameter("processingStrategy").build()).build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier(SCATTER_GATHER)
        .withTypeDefinition(fromType(ScatterGatherRouter.class)).withObjectFactoryType(ScatterGatherRouterFactoryBean.class)
        .withSetterParameterDefinition("timeout", fromSimpleParameter("timeout").build())
        .withSetterParameterDefinition("aggregationStrategy", fromChildConfiguration(AggregationStrategy.class).build())
        .withSetterParameterDefinition("threadingProfile", fromChildConfiguration(ThreadingProfile.class).build())
        .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(MessageProcessor.class).build())
        .asScope().build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier(WIRE_TAP).withTypeDefinition(fromType(WireTap.class))
        .withSetterParameterDefinition("tap", fromChildConfiguration(MessageProcessor.class).build())
        .withSetterParameterDefinition("filter", fromChildConfiguration(Filter.class).build()).asScope().build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier(ENRICHER)
        .withObjectFactoryType(MessageEnricherObjectFactory.class).withTypeDefinition(fromType(MessageEnricher.class))
        .withSetterParameterDefinition("messageProcessor", fromChildConfiguration(MessageProcessor.class).build())
        .withSetterParameterDefinition("enrichExpressionPairs",
                                       fromChildCollectionConfiguration(MessageEnricher.EnrichExpressionPair.class).build())
        .withSetterParameterDefinition("source", fromSimpleParameter("source").build())
        .withSetterParameterDefinition("target", fromSimpleParameter("target").build()).build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("enrich")
        .withTypeDefinition(fromType(MessageEnricher.EnrichExpressionPair.class))
        .withConstructorParameterDefinition(fromSimpleParameter("source").build())
        .withConstructorParameterDefinition(fromSimpleParameter("target").build()).build());
    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier(ASYNC).withTypeDefinition(fromType(AsyncDelegateMessageProcessor.class))
            .withObjectFactoryType(AsyncMessageProcessorsFactoryBean.class)
            .withSetterParameterDefinition("processingStrategy", fromSimpleReferenceParameter("processingStrategy").build())
            .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(MessageProcessor.class).build())
            .withSetterParameterDefinition(NAME, fromSimpleParameter(NAME).build()).build());
    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier(TRANSACTIONAL).withTypeDefinition(fromType(TransactionalMessageProcessor.class))
            .withObjectFactoryType(TransactionalMessageProcessorsFactoryBean.class)
            .withSetterParameterDefinition("exceptionListener", fromChildConfiguration(MessagingExceptionHandler.class).build())
            .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(MessageProcessor.class).build())
            .withSetterParameterDefinition("action", fromSimpleParameter("action").build()).build());

    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier(UNTIL_SUCCESSFUL).withTypeDefinition(fromType(UntilSuccessful.class))
            .withSetterParameterDefinition("objectStore", fromSimpleReferenceParameter("objectStore-ref").build())
            .withSetterParameterDefinition("deadLetterQueue", fromSimpleReferenceParameter("deadLetterQueue-ref").build())
            .withSetterParameterDefinition("maxRetries", fromSimpleParameter("maxRetries").build())
            .withSetterParameterDefinition("millisBetweenRetries", fromSimpleParameter("millisBetweenRetries").build())
            .withSetterParameterDefinition("secondsBetweenRetries", fromSimpleParameter("secondsBetweenRetries").build())
            .withSetterParameterDefinition("failureExpression", fromSimpleParameter("failureExpression").build())
            .withSetterParameterDefinition("ackExpression", fromSimpleParameter("ackExpression").build())
            .withSetterParameterDefinition("synchronous", fromSimpleParameter("synchronous").build())
            .withSetterParameterDefinition("threadingProfile", fromChildConfiguration(ThreadingProfile.class).build())
            .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(MessageProcessor.class).build())
            .build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier(FOREACH).withTypeDefinition(fromType(Foreach.class))
        .withSetterParameterDefinition("collectionExpression", fromSimpleParameter("collection").build())
        .withSetterParameterDefinition("batchSize", fromSimpleParameter("batchSize").build())
        .withSetterParameterDefinition("rootMessageVariableName", fromSimpleParameter("rootMessageVariableName").build())
        .withSetterParameterDefinition("counterVariableName", fromSimpleParameter("counterVariableName").build())
        .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(MessageProcessor.class).build())
        .build());
    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier(FIRST_SUCCESSFUL).withTypeDefinition(fromType(FirstSuccessful.class))
            .withSetterParameterDefinition("failureExpression", fromSimpleParameter("failureExpression").build())
            .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(MessageProcessor.class).build())
            .build());
    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier(ROUND_ROBIN).withTypeDefinition(fromType(RoundRobin.class))
            .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(MessageProcessor.class).build())
            .build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier(CHOICE).withTypeDefinition(fromType(ChoiceRouter.class))
        .withObjectFactoryType(ChoiceRouterFactoryBean.class)
        .withSetterParameterDefinition("routes", fromChildCollectionConfiguration(MessageProcessorFilterPair.class).build())
        .withSetterParameterDefinition("defaultRoute", fromChildConfiguration(MessageProcessorFilterPair.class).build()).build());
    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier(WHEN).withTypeDefinition(fromType(MessageProcessorFilterPair.class))
            .withObjectFactoryType(MessageProcessorFilterPairFactoryBean.class)
            .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(MessageProcessor.class).build())
            .withSetterParameterDefinition("expression", fromSimpleParameter("expression").build()).build());
    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier(OTHERWISE).withTypeDefinition(fromType(MessageProcessorFilterPair.class))
            .withObjectFactoryType(MessageProcessorFilterPairFactoryBean.class)
            .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(MessageProcessor.class).build())
            .withSetterParameterDefinition("expression", fromFixedValue("true").build()).build());
    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier(ALL).withTypeDefinition(fromType(MulticastingRouter.class))
            .withSetterParameterDefinition(MESSAGE_PROCESSORS, fromChildCollectionConfiguration(MessageProcessor.class).build())
            .build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier(REQUEST_REPLY)
        .withTypeDefinition(fromType(SimpleAsyncRequestReplyRequester.class))
        .withSetterParameterDefinition("messageProcessor", fromChildConfiguration(MessageProcessor.class).build())
        .withSetterParameterDefinition("messageSource", fromChildConfiguration(MessageSource.class).build())
        .withSetterParameterDefinition("timeout", fromSimpleParameter("timeout").build())
        .withSetterParameterDefinition("storePrefix", fromSimpleParameter("storePrefix").build()).build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier(POLL)
        .withTypeDefinition(fromType(PollingMessageSource.class)).withObjectFactoryType(PollingMessageSourceFactoryBean.class)
        .withSetterParameterDefinition("messageProcessor", fromChildConfiguration(MessageProcessor.class).build())
        .withSetterParameterDefinition("frequency", fromSimpleParameter("frequency").build())
        .withSetterParameterDefinition("override", fromChildConfiguration(MessageProcessorPollingOverride.class).build())
        .withSetterParameterDefinition("schedulerFactory", fromChildConfiguration(SchedulerFactory.class).build()).build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("fixed-frequency-scheduler")
        .withTypeDefinition(fromType(FixedFrequencySchedulerFactory.class))
        .withSetterParameterDefinition("frequency", fromSimpleParameter("frequency").build())
        .withSetterParameterDefinition("startDelay", fromSimpleParameter("startDelay").build())
        .withSetterParameterDefinition("timeUnit", fromSimpleParameter("timeUnit").build()).build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("watermark")
        .withSetterParameterDefinition("variable", fromSimpleParameter("variable").build())
        .withSetterParameterDefinition("defaultExpression", fromSimpleParameter("default-expression").build())
        .withSetterParameterDefinition("updateExpression", fromSimpleParameter("update-expression").build())
        .withSetterParameterDefinition("objectStore", fromSimpleReferenceParameter("object-store-ref").build())
        .withSetterParameterDefinition("selector", fromSimpleParameter("selector").build())
        .withSetterParameterDefinition("selectorExpression", fromSimpleParameter("selector-expression").build())
        .withTypeDefinition(fromType(Watermark.class)).withObjectFactoryType(WatermarkFactoryBean.class).build());

    ComponentBuildingDefinition.Builder baseReconnectDefinition = baseDefinition.copy()
        .withTypeDefinition(fromType(RetryPolicyTemplate.class)).withObjectFactoryType(RetryPolicyTemplateObjectFactory.class)
        .withSetterParameterDefinition("blocking", fromSimpleParameter("blocking").build())
        .withSetterParameterDefinition("frequency", fromSimpleParameter("frequency").build());

    componentBuildingDefinitions.add(baseReconnectDefinition.copy().withIdentifier("reconnect-forever")
        .withSetterParameterDefinition("count", fromFixedValue(RETRY_COUNT_FOREVER).build()).build());
    componentBuildingDefinitions.add(baseReconnectDefinition.copy().withIdentifier("reconnect")
        .withSetterParameterDefinition("count", fromSimpleParameter("count").build()).build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("idempotent-redelivery-policy")
        .withTypeDefinition(fromType(IdempotentRedeliveryPolicy.class))
        .withSetterParameterDefinition("useSecureHash", fromSimpleParameter("useSecureHash").build())
        .withSetterParameterDefinition("messageDigestAlgorithm", fromSimpleParameter("messageDigestAlgorithm").build())
        .withSetterParameterDefinition("maxRedeliveryCount", fromSimpleParameter("maxRedeliveryCount").build())
        .withSetterParameterDefinition("idExpression", fromSimpleParameter("idExpression").build())
        .withSetterParameterDefinition("idExpression", fromSimpleParameter("idExpression").build())
        .withSetterParameterDefinition("objectStore", fromSimpleReferenceParameter("object-store-ref").build())
        .withSetterParameterDefinition("messageProcessor", fromChildConfiguration(MessageProcessor.class).build()).build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("dead-letter-queue")
        .withTypeDefinition(fromType(MessageProcessor.class)).withObjectFactoryType(MessageProcessorWrapperObjectFactory.class)
        .withSetterParameterDefinition("messageProcessor", fromChildConfiguration(MessageProcessor.class).build()).build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("composite-source")
        .withTypeDefinition(fromType(StartableCompositeMessageSource.class))
        .withSetterParameterDefinition("messageSources", fromChildCollectionConfiguration(MessageSource.class).build())
        .withSetterParameterDefinition("muleContext", fromReferenceObject(MuleContext.class).build()).build());
    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("configuration")
        .withTypeDefinition(fromType(MuleConfiguration.class)).withObjectFactoryType(MuleConfigurationConfigurator.class)
        .withSetterParameterDefinition("defaultExceptionStrategyName",
                                       fromSimpleParameter("defaultExceptionStrategy-ref").build())
        .withSetterParameterDefinition("defaultProcessingStrategy",
                                       fromSimpleReferenceParameter("defaultProcessingStrategy").build())
        .withSetterParameterDefinition("defaultResponseTimeout", fromSimpleParameter("defaultResponseTimeout").build())
        .withSetterParameterDefinition("maxQueueTransactionFilesSize",
                                       fromSimpleParameter("maxQueueTransactionFilesSize").build())
        .withSetterParameterDefinition("defaultTransactionTimeout", fromSimpleParameter("defaultTransactionTimeout").build())
        .withSetterParameterDefinition("shutdownTimeout", fromSimpleParameter("shutdownTimeout").build())
        .withSetterParameterDefinition("defaultTransactionTimeout", fromSimpleParameter("defaultTransactionTimeout").build())
        .withSetterParameterDefinition("useExtendedTransformations", fromSimpleParameter("useExtendedTransformations").build())
        .withSetterParameterDefinition("flowEndingWithOneWayEndpointReturnsNull",
                                       fromSimpleParameter("flowEndingWithOneWayEndpointReturnsNull").build())
        .withSetterParameterDefinition("enricherPropagatesSessionVariableChanges",
                                       fromSimpleParameter("enricherPropagatesSessionVariableChanges").build())
        .withSetterParameterDefinition("extensions", fromChildCollectionConfiguration(Object.class).build())
        .withSetterParameterDefinition("defaultObjectSerializer",
                                       fromSimpleReferenceParameter("defaultObjectSerializer-ref").build())
        .withSetterParameterDefinition("extensions", fromChildCollectionConfiguration(ConfigurationExtension.class).build())
        .build());

    componentBuildingDefinitions
        .add(baseDefinition.copy().withIdentifier("notifications").withTypeDefinition(fromType(ServerNotificationManager.class))
            .withObjectFactoryType(ServerNotificationManagerConfigurator.class)
            .withSetterParameterDefinition("notificationDynamic", fromSimpleParameter("dynamic").build())
            .withSetterParameterDefinition("enabledNotifications",
                                           fromChildCollectionConfiguration(NotificationConfig.EnabledNotificationConfig.class)
                                               .build())
            .withSetterParameterDefinition("disabledNotifications",
                                           fromChildCollectionConfiguration(NotificationConfig.DisabledNotificationConfig.class)
                                               .build())
            .withSetterParameterDefinition("notificationListeners",
                                           fromChildCollectionConfiguration(ListenerSubscriptionPair.class).build())
            .build());

    ComponentBuildingDefinition.Builder baseNotificationDefinition =
        baseDefinition.copy().withSetterParameterDefinition("interfaseName", fromSimpleParameter("interface").build())
            .withSetterParameterDefinition("eventName", fromSimpleParameter("event").build())
            .withSetterParameterDefinition("interfaceClass", fromSimpleParameter("interface-class").build())
            .withSetterParameterDefinition("eventClass", fromSimpleParameter("event-class").build());

    componentBuildingDefinitions.add(baseNotificationDefinition.copy()
        .withTypeDefinition(fromType(NotificationConfig.EnabledNotificationConfig.class)).withIdentifier("notification").build());

    componentBuildingDefinitions
        .add(baseNotificationDefinition.copy().withTypeDefinition(fromType(NotificationConfig.DisabledNotificationConfig.class))
            .withIdentifier("disable-notification").build());

    componentBuildingDefinitions.add(baseDefinition.copy().withIdentifier("notification-listener")
        .withTypeDefinition(fromType(ListenerSubscriptionPair.class))
        .withSetterParameterDefinition("listener", fromSimpleReferenceParameter("ref").build())
        .withSetterParameterDefinition("subscription", fromSimpleParameter("subscription").build()).build());

    componentBuildingDefinitions.addAll(getTransformersBuildingDefinitions());
    componentBuildingDefinitions.addAll(getEntryPointResolversDefinitions());

    return componentBuildingDefinitions;
  }

  private List<ComponentBuildingDefinition> getEntryPointResolversDefinitions() {
    List<ComponentBuildingDefinition> buildingDefinitions = new ArrayList<>();
    buildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier("custom-entry-point-resolver-set")
        .withTypeDefinition(fromConfigurationAttribute(CLASS_ATTRIBUTE))
        .withSetterParameterDefinition("entryPointResolvers", fromChildCollectionConfiguration(EntryPointResolver.class).build())
        .build());
    buildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier("entry-point-resolver-set")
        .withTypeDefinition(fromType(DefaultEntryPointResolverSet.class))
        .withSetterParameterDefinition("entryPointResolvers", fromChildCollectionConfiguration(EntryPointResolver.class).build())
        .build());
    buildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier("custom-entry-point-resolver")
        .withTypeDefinition(fromConfigurationAttribute(CLASS_ATTRIBUTE))
        .build());
    buildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier("callable-entry-point-resolver")
        .withTypeDefinition(fromType(CallableEntryPointResolver.class))
        .build());
    buildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier("method-entry-point-resolver")
        .withTypeDefinition(fromType(ExplicitMethodEntryPointResolver.class))
        .withSetterParameterDefinition("methods", fromChildCollectionConfiguration(String.class).build())
        .build());
    buildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier("include-entry-point")
        .withTypeDefinition(fromType(String.class))
        .build());
    buildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier("exclude-entry-point")
        .withTypeDefinition(fromType(String.class))
        .build());
    buildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier("property-entry-point-resolver")
        .withTypeDefinition(fromType(MethodHeaderPropertyEntryPointResolver.class))
        .withSetterParameterDefinition("methodProperty", fromSimpleParameter("property").build())
        .build());
    buildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier("property-entry-point-resolver")
        .withTypeDefinition(fromType(MethodHeaderPropertyEntryPointResolver.class))
        .withSetterParameterDefinition("methodProperty", fromSimpleParameter("property").build())
        .build());
    buildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier("reflection-entry-point-resolver")
        .withTypeDefinition(fromType(ReflectionEntryPointResolver.class))
        .withSetterParameterDefinition("ignoredMethods",
                                       fromChildConfiguration(List.class).withIdentifier("exclude-object-methods").build())
        .withSetterParameterDefinition("ignoredMethods",
                                       fromChildConfiguration(List.class).withIdentifier("exclude-entry-point").build())
        .build());
    buildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier("exclude-object-methods")
        .withTypeDefinition(fromType(List.class))
        .withObjectFactoryType(IgnoreObjectMethodsObjectFactory.class)
        .build());
    buildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier("no-arguments-entry-point-resolver")
        .withTypeDefinition(fromType(NoArgumentsEntryPointResolver.class))
        .build());
    buildingDefinitions.add(baseDefinition
        .copy()
        .withIdentifier("array-entry-point-resolver")
        .withTypeDefinition(fromType(ArrayEntryPointResolver.class))
        .build());
    return buildingDefinitions;
  }

  private List<ComponentBuildingDefinition> getTransformersBuildingDefinitions() {
    List<ComponentBuildingDefinition> transformerComponentBuildingDefinitions = new ArrayList<>();
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder()
        .withIdentifier("base64-encoder-transformer")
        .withSetterParameterDefinition("transformerClass", fromFixedValue(Base64Encoder.class).build())
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder()
        .withIdentifier("base64-decoder-transformer")
        .withSetterParameterDefinition("transformerClass", fromFixedValue(Base64Decoder.class).build())
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder()
        .withIdentifier("xml-entity-encoder-transformer")
        .withSetterParameterDefinition("transformerClass", fromFixedValue(XmlEntityEncoder.class).build())
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder()
        .withIdentifier("xml-entity-decoder-transformer")
        .withSetterParameterDefinition("transformerClass", fromFixedValue(XmlEntityDecoder.class).build())
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder()
        .withIdentifier("gzip-compress-transformer")
        .withSetterParameterDefinition("transformerClass", fromFixedValue(GZipCompressTransformer.class).build())
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder()
        .withIdentifier("gzip-uncompress-transformer")
        .withSetterParameterDefinition("transformerClass", fromFixedValue(GZipUncompressTransformer.class).build())
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder()
        .withIdentifier("encrypt-transformer")
        .withSetterParameterDefinition("transformerClass", fromFixedValue(EncryptionTransformer.class).build())
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder()
        .withIdentifier("decrypt-transformer")
        .withSetterParameterDefinition("transformerClass", fromFixedValue(DecryptionTransformer.class).build())
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder()
        .withIdentifier("byte-array-to-hex-string-transformer")
        .withSetterParameterDefinition("transformerClass", fromFixedValue(ByteArrayToHexString.class).build())
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder()
        .withSetterParameterDefinition("transformerClass", fromFixedValue(Base64Decoder.class).build())
        .withIdentifier("hex-string-to-byte-array-transformer").withTypeDefinition(fromType(HexStringToByteArray.class))
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder()
        .withIdentifier("byte-array-to-object-transformer")
        .withSetterParameterDefinition("transformerClass", fromFixedValue(ByteArrayToObject.class).build())
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder()
        .withIdentifier("object-to-byte-array-transformer")
        .withSetterParameterDefinition("transformerClass", fromFixedValue(ObjectToByteArray.class).build())
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder()
        .withIdentifier("object-to-string-transformer")
        .withSetterParameterDefinition("transformerClass", fromFixedValue(ObjectToString.class).build())
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder()
        .withIdentifier("byte-array-to-serializable-transformer")
        .withSetterParameterDefinition("transformerClass", fromFixedValue(ByteArrayToSerializable.class).build())
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder()
        .withIdentifier("serializable-to-byte-array-transformer")
        .withSetterParameterDefinition("transformerClass", fromFixedValue(SerializableToByteArray.class).build())
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder()
        .withIdentifier("byte-array-to-string-transformer")
        .withSetterParameterDefinition("transformerClass", fromFixedValue(ObjectToString.class).build())
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder()
        .withIdentifier("string-to-byte-array-transformer")
        .withSetterParameterDefinition("transformerClass", fromFixedValue(ObjectToByteArray.class).build())
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder()
        .withIdentifier("parse-template")
        .withSetterParameterDefinition("transformerClass", fromFixedValue(ParseTemplateTransformer.class).build())
        .withSetterParameterDefinition("location", fromSimpleParameter("location").build())
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder()
        .withIdentifier("auto-transformer")
        .withSetterParameterDefinition("transformerClass", fromFixedValue(AutoTransformer.class).build())
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder()
        .withIdentifier("map-to-bean-transformer")
        .withSetterParameterDefinition("transformerClass", fromFixedValue(MapToBean.class).build())
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder()
        .withIdentifier("bean-to-map-transformer")
        .withSetterParameterDefinition("transformerClass", fromFixedValue(BeanToMap.class).build())
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder()
        .withIdentifier("combine-collections-transformer")
        .withSetterParameterDefinition("transformerClass", fromFixedValue(CombineCollectionsTransformer.class).build())
        .build());
    transformerComponentBuildingDefinitions.add(getMuleMessageTransformerBaseBuilder()
        .withIdentifier("append-string-transformer")
        .withTypeDefinition(fromType(StringAppendTransformer.class))
        .withSetterParameterDefinition("message", fromSimpleParameter("message").build())
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder()
        .withIdentifier("custom-transformer")
        .withSetterParameterDefinition("transformerClass", fromSimpleParameter("class").build())
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder()
        .withIdentifier("bean-builder-transformer")
        .withSetterParameterDefinition("transformerClass", fromFixedValue(BeanBuilderTransformer.class).build())
        .withSetterParameterDefinition("beanClass", fromSimpleParameter("beanClass").build())
        .withSetterParameterDefinition("beanFactory", fromSimpleParameter("beanFactory").build())
        .withSetterParameterDefinition("arguments", fromChildCollectionConfiguration(ExpressionArgument.class).build())
        .build());
    transformerComponentBuildingDefinitions.add(getMuleMessageTransformerBaseBuilder()
        .withIdentifier("set-property")
        .withTypeDefinition(fromType(AddPropertyTransformer.class))
        .withSetterParameterDefinition("identifier", fromSimpleParameter("propertyName").build())
        .withSetterParameterDefinition("value", fromSimpleParameter("value").build())
        .build());
    transformerComponentBuildingDefinitions.add(getMuleMessageTransformerBaseBuilder()
        .withIdentifier("set-variable")
        .withTypeDefinition(fromType(AddFlowVariableTransformer.class))
        .withSetterParameterDefinition("identifier", fromSimpleParameter("variableName").build())
        .withSetterParameterDefinition("value", fromSimpleParameter("value").build())
        .build());
    transformerComponentBuildingDefinitions.add(getMuleMessageTransformerBaseBuilder()
        .withIdentifier("remove-property")
        .withTypeDefinition(fromType(RemovePropertyTransformer.class))
        .withSetterParameterDefinition("identifier", fromSimpleParameter("propertyName").build())
        .build());
    transformerComponentBuildingDefinitions.add(getMuleMessageTransformerBaseBuilder()
        .withIdentifier("remove-variable")
        .withTypeDefinition(fromType(RemoveFlowVariableTransformer.class))
        .withSetterParameterDefinition("identifier", fromSimpleParameter("variableName").build())
        .build());
    transformerComponentBuildingDefinitions.add(getMuleMessageTransformerBaseBuilder()
        .withIdentifier("copy-properties")
        .withTypeDefinition(fromType(CopyPropertiesTransformer.class))
        .withSetterParameterDefinition("propertyName", fromSimpleParameter("propertyName").build())
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder()
        .withIdentifier("expression-transformer")
        .withTypeDefinition(fromType(ExpressionTransformer.class))
        .withObjectFactoryType(ExpressionTransformerObjectFactory.class)
        .withSetterParameterDefinition("returnSourceIfNull", fromSimpleParameter("returnSourceIfNull").build())
        .withSetterParameterDefinition("expression", fromSimpleParameter("expression").build())
        .withSetterParameterDefinition("arguments", fromChildCollectionConfiguration(ExpressionArgument.class).build())
        .build());
    transformerComponentBuildingDefinitions.add(getTransformerBaseBuilder()
        .withIdentifier("return-argument")
        .withTypeDefinition(fromType(ExpressionArgument.class))
        .withObjectFactoryType(ExpressionArgumentObjectFactory.class)
        .withSetterParameterDefinition("optional", fromSimpleParameter("optional").build())
        .withSetterParameterDefinition("expression", fromSimpleParameter("expression").build())
        .build());


    return transformerComponentBuildingDefinitions;
  }

  public static ComponentBuildingDefinition.Builder getTransformerBaseBuilder() {
    return baseDefinition.copy()
        .withTypeDefinition(fromType(Transformer.class))
        .withObjectFactoryType(TransformerObjectFactory.class)
        .withSetterParameterDefinition("name", fromSimpleParameter("name").build())
        .withSetterParameterDefinition("ignoreBadInput", fromSimpleParameter("ignoreBadInput").build())
        .withSetterParameterDefinition("encoding", fromSimpleParameter("encoding").build())
        .withSetterParameterDefinition("mimeType", fromSimpleParameter("mimeType").build())
        .withSetterParameterDefinition("returnClass", fromSimpleParameter("returnClass").build()).copy();
  }

  public static ComponentBuildingDefinition.Builder getMuleMessageTransformerBaseBuilder() {
    return baseDefinition.copy()
        .withSetterParameterDefinition("encoding", fromSimpleParameter("encoding").build())
        .withSetterParameterDefinition("mimeType", fromSimpleParameter("mimeType").build()).copy();
  }


  public static ComponentBuildingDefinition.Builder getTransformerBaseBuilderForClass(Class<? extends AbstractTransformer> transformerClass) {
    return getTransformerBaseBuilder().withSetterParameterDefinition("transformerClass", fromFixedValue(transformerClass).build())
        .withTypeDefinition(fromType(transformerClass));
  }

  private ComponentBuildingDefinition.Builder createTransactionManagerDefinitionBuilder(String transactionManagerName,
                                                                                        Class<?> transactionManagerClass) {
    return transactionManagerBaseDefinition.copy().withIdentifier(transactionManagerName)
        .withTypeDefinition(fromType(transactionManagerClass));
  }

}

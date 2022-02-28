package com.sequenceiq.cloudbreak.usage.strategy;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.telemetry.messagebroker.MessageBrokerConfiguration;
import com.sequenceiq.cloudbreak.usage.model.UsageContext;
import com.sequenceiq.cloudbreak.usage.http.EdhHttpConfiguration;

@Service
public class CompositeUsageProcessingStrategy implements UsageProcessingStrategy {

    private final HttpUsageProcessingStrategy httpUsageProcessingStrategy;

    private final LoggingUsageProcessingStrategy loggingUsageProcessingStrategy;

    private final MessageBrokerUsageStrategy messageBrokerUsageStrategy;

    private final EdhHttpConfiguration edhHttpConfiguration;

    private final MessageBrokerConfiguration messageBrokerConfiguration;

    private final AtomicBoolean useMessageBroker = new AtomicBoolean(false);

    private final AtomicBoolean useLogging = new AtomicBoolean(false);

    private final AtomicBoolean useHttpUsage = new AtomicBoolean(false);

    public CompositeUsageProcessingStrategy(LoggingUsageProcessingStrategy loggingUsageProcessingStrategy,
            HttpUsageProcessingStrategy httpUsageProcessingStrategy, MessageBrokerUsageStrategy messageBrokerUsageStrategy,
            EdhHttpConfiguration edhHttpConfiguration, MessageBrokerConfiguration messageBrokerConfiguration) {
        this.httpUsageProcessingStrategy = httpUsageProcessingStrategy;
        this.loggingUsageProcessingStrategy = loggingUsageProcessingStrategy;
        this.messageBrokerUsageStrategy = messageBrokerUsageStrategy;
        this.edhHttpConfiguration = edhHttpConfiguration;
        this.messageBrokerConfiguration = messageBrokerConfiguration;
    }

    @PostConstruct
    public void init() {
        useHttpUsage.set(initHttpUsage());
        useMessageBroker.set(initMessageBroker());
        useLogging.set(initLogging());
    }

    @Override
    public void processUsage(UsageProto.Event event, UsageContext context) {
        if (useLogging.get()) {
            loggingUsageProcessingStrategy.processUsage(event, context);
        }
        if (useHttpUsage.get()) {
            httpUsageProcessingStrategy.processUsage(event, context);
        }
        if (useMessageBroker.get()) {
            messageBrokerUsageStrategy.processUsage(event, context);
        }
    }

    private boolean initMessageBroker() {
        return messageBrokerUsageStrategy != null && messageBrokerConfiguration != null && messageBrokerConfiguration.isEnabled();
    }

    private boolean initHttpUsage() {
        return httpUsageProcessingStrategy != null && edhHttpConfiguration != null && edhHttpConfiguration.isEnabled();
    }

    private boolean initLogging() {
        return !useHttpUsage.get() && !useMessageBroker.get() ||
                (edhHttpConfiguration.isEnabled() && edhHttpConfiguration.isForceLogging());
    }
}

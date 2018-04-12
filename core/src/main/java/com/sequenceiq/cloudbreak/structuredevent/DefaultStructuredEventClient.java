package com.sequenceiq.cloudbreak.structuredevent;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.conf.KafkaSenderConfig;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.reactor.KafkaStructuredEventAsyncNotifier;

@Service
public class DefaultStructuredEventClient implements StructuredEventClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultStructuredEventClient.class);

    @Inject
    private KafkaSenderConfig kafkaSenderConfig;

    @Inject
    private StructuredEventService structuredEventService;

    @Inject
    private KafkaStructuredEventAsyncNotifier kafkaAsyncSender;

    @Override
    public void sendStructuredEvent(StructuredEvent structuredEvent) {
        structuredEventService.storeStructuredEvent(structuredEvent);
        if (kafkaSenderConfig.isKafkaConfigured()) {
            kafkaAsyncSender.sendEventLogMessage(structuredEvent);
        }
    }
}

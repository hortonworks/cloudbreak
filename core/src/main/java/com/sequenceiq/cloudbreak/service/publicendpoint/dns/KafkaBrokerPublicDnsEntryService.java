package com.sequenceiq.cloudbreak.service.publicendpoint.dns;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaRoles;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.service.blueprint.ComponentLocatorService;

public class KafkaBrokerPublicDnsEntryService extends BaseDnsEntryService {

    @Inject
    private ComponentLocatorService componentLocatorService;

    @Override
    protected Map<String, List<String>> getComponentLocation(StackDtoDelegate stack) {
        return componentLocatorService.getComponentLocation(stack, List.of(KafkaRoles.KAFKA_BROKER));
    }

    @Override
    protected String logName() {
        return "Kafka brokers";
    }
}

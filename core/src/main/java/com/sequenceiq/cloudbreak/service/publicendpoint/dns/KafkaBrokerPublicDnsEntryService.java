package com.sequenceiq.cloudbreak.service.publicendpoint.dns;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cmtemplate.configproviders.kafka.KafkaRoles;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.blueprint.ComponentLocatorService;

@Service
public class KafkaBrokerPublicDnsEntryService extends BaseDnsEntryService {

    @Inject
    private ComponentLocatorService componentLocatorService;

    @Override
    protected Map<String, List<String>> getComponentLocation(Stack stack) {
        return componentLocatorService.getComponentLocation(stack.getCluster(), List.of(KafkaRoles.KAFKA_BROKER));
    }

    @Override
    protected String logName() {
        return "Kafka brokers";
    }
}

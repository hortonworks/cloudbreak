package com.sequenceiq.cloudbreak.metering;

import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.MeteringEvent;
import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.MeteringEvent.PayloadCase.STATUSCHANGE;
import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.MeteringEvent.PayloadCase.SYNC;
import static com.cloudera.thunderhead.service.meteringv2.events.MeteringV2EventsProto.Resource;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class MeteringInfoProvider {

    public String getReducedInfo(MeteringEvent meteringEvent) {
        StringBuilder info = new StringBuilder("[id: ");
        info.append(meteringEvent.getId()).append(", type: ");
        if (meteringEvent.getPayloadCase() == SYNC) {
            info.append("sync");
        } else if (meteringEvent.getPayloadCase() == STATUSCHANGE) {
            info.append("statusChange, status: ").append(meteringEvent.getStatusChange().getStatus());
        } else {
            info.append("unknown");
        }
        info.append(", resources: ").append(calculateResources(meteringEvent)).append("]");
        return info.toString();
    }

    private Map<String, Long> calculateResources(MeteringEvent meteringEvent) {
        if (meteringEvent.getPayloadCase() == SYNC) {
            return calculateResources(meteringEvent.getSync().getResourcesList());
        } else if (meteringEvent.getPayloadCase() == STATUSCHANGE) {
            return calculateResources(meteringEvent.getStatusChange().getResourcesList());
        } else {
            return Map.of();
        }
    }

    private Map<String, Long> calculateResources(List<Resource> resources) {
        return resources.stream().collect(Collectors.groupingBy(resource -> resource.getInstanceResource().getInstanceType(), Collectors.counting()));
    }
}

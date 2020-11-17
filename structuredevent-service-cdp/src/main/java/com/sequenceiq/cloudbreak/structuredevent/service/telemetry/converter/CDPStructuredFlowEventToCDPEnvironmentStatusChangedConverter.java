package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.CDPEnvironmentStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.EnvironmentDetails;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Component
public class CDPStructuredFlowEventToCDPEnvironmentStatusChangedConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CDPStructuredFlowEventToCDPEnvironmentStatusChangedConverter.class);

    @Inject
    private CDPStructuredFlowEventToCDPOperationDetailsConverter operationDetailsConverter;

    public UsageProto.CDPEnvironmentStatusChanged convert(CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent,
            UsageProto.CDPEnvironmentStatus.Value status) {

        UsageProto.CDPEnvironmentStatusChanged.Builder cdpEnvironmentStatusChangedBuilder = UsageProto.CDPEnvironmentStatusChanged.newBuilder();

        cdpEnvironmentStatusChangedBuilder.setOperationDetails(operationDetailsConverter.convert(cdpStructuredFlowEvent));

        cdpEnvironmentStatusChangedBuilder.setFailureReason(cdpStructuredFlowEvent.getStatusReason());
        cdpEnvironmentStatusChangedBuilder.setNewStatus(status);

        UsageProto.CDPEnvironmentStatusChanged ret = cdpEnvironmentStatusChangedBuilder.build();
        LOGGER.debug("Converted telemetry event: {}", ret);
        return ret;
    }

    private UsageProto.CDPEnvironmentDetails convertEnvironmentDetails(CDPEnvironmentStructuredFlowEvent cdpEnvironmentStructuredFlowEvent) {
        UsageProto.CDPEnvironmentDetails.Builder cdpEnvironmentDetails = UsageProto.CDPEnvironmentDetails.newBuilder();
        EnvironmentDetails environmentDetails = cdpEnvironmentStructuredFlowEvent.getPayload();

        if (environmentDetails.getRegions() != null) {
            cdpEnvironmentDetails.setRegion(environmentDetails.getRegions().stream()
                    .map(Region::getName).sorted()
                    .collect(Collectors.joining(",")));
        }

        cdpEnvironmentDetails.setEnvironmentType(UsageProto.CDPEnvironmentsEnvironmentType.Value.valueOf(environmentDetails.getCloudPlatform()));

        NetworkDto network = environmentDetails.getNetwork();
        if (network != null && network.getSubnetMetas() != null) {
            List<String> availabilityZones = network.getSubnetMetas().values().stream().map(CloudSubnet::getAvailabilityZone)
                    .sorted().distinct().collect(Collectors.toUnmodifiableList());

            cdpEnvironmentDetails.setNumberOfAvailabilityZones(availabilityZones.size());
            cdpEnvironmentDetails.setAvailabilityZones(String.join(",", availabilityZones));
        }

        return cdpEnvironmentDetails.build();
    }

}

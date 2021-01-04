package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.CDPEnvironmentStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.EnvironmentDetails;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentFeatures;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Component
public class CDPStructuredFlowEventToCDPEnvironmentRequestedConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CDPStructuredFlowEventToCDPEnvironmentRequestedConverter.class);

    @Inject
    private CDPStructuredFlowEventToCDPOperationDetailsConverter operationDetailsConverter;

    public UsageProto.CDPEnvironmentRequested convert(CDPEnvironmentStructuredFlowEvent cdpStructuredFlowEvent) {
        if (cdpStructuredFlowEvent == null) {
            return null;
        }
        UsageProto.CDPEnvironmentRequested.Builder cdpEnvironmentRequestedBuilder = UsageProto.CDPEnvironmentRequested.newBuilder();
        cdpEnvironmentRequestedBuilder.setOperationDetails(operationDetailsConverter.convert(cdpStructuredFlowEvent));

        EnvironmentDetails environmentDetails = cdpStructuredFlowEvent.getPayload();
        cdpEnvironmentRequestedBuilder.setEnvironmentDetails(convertEnvironmentDetails(environmentDetails));
        cdpEnvironmentRequestedBuilder.setTelemetryFeatureDetails(convertTelemetryFeatureDetails(environmentDetails));

        UsageProto.CDPEnvironmentRequested ret = cdpEnvironmentRequestedBuilder.build();
        LOGGER.debug("Converted telemetry event: {}", ret);
        return ret;
    }

    private UsageProto.CDPEnvironmentDetails convertEnvironmentDetails(EnvironmentDetails srcEnvironmentDetails) {
        UsageProto.CDPEnvironmentDetails.Builder cdpEnvironmentDetails = UsageProto.CDPEnvironmentDetails.newBuilder();
        if (srcEnvironmentDetails != null) {
            if (srcEnvironmentDetails.getRegions() != null) {
                cdpEnvironmentDetails.setRegion(srcEnvironmentDetails.getRegions().stream()
                        .map(Region::getName).filter(Objects::nonNull).sorted().distinct()
                        .collect(Collectors.joining(",")));
            }

            if (srcEnvironmentDetails.getCloudPlatform() != null) {
                cdpEnvironmentDetails.setEnvironmentType(UsageProto.CDPEnvironmentsEnvironmentType
                        .Value.valueOf(srcEnvironmentDetails.getCloudPlatform()));
            }
            Tunnel tunnel = srcEnvironmentDetails.getTunnel();
            if (tunnel != null) {
                cdpEnvironmentDetails.setTunnelType(UsageProto.CDPEnvironmentsEnvironmentTunnel
                        .Value.valueOf(tunnel.name()));
            }
            NetworkDto network = srcEnvironmentDetails.getNetwork();
            if (network != null && network.getSubnetMetas() != null) {
                List<String> availabilityZones = network.getSubnetMetas().values().stream().map(CloudSubnet::getAvailabilityZone)
                        .filter(Objects::nonNull).sorted().distinct().collect(Collectors.toUnmodifiableList());

                cdpEnvironmentDetails.setNumberOfAvailabilityZones(availabilityZones.size());
                cdpEnvironmentDetails.setAvailabilityZones(String.join(",", availabilityZones));
            }
        }
        return cdpEnvironmentDetails.build();
    }

    private UsageProto.CDPEnvironmentTelemetryFeatureDetails convertTelemetryFeatureDetails(EnvironmentDetails environmentDetails) {
        UsageProto.CDPEnvironmentTelemetryFeatureDetails.Builder cdpTelemetryFeatureDetailsBuilder =
                UsageProto.CDPEnvironmentTelemetryFeatureDetails.newBuilder();

        if (environmentDetails != null && environmentDetails.getEnvironmentTelemetryFeatures() != null) {
            EnvironmentFeatures environmentFeatures = environmentDetails.getEnvironmentTelemetryFeatures();
            if (environmentFeatures.getWorkloadAnalytics() != null && environmentFeatures.getWorkloadAnalytics().isEnabled() != null) {
                cdpTelemetryFeatureDetailsBuilder.setWorkloadAnalytics(environmentFeatures.getWorkloadAnalytics().isEnabled().toString());
            }
            if (environmentFeatures.getClusterLogsCollection() != null && environmentFeatures.getClusterLogsCollection().isEnabled() != null) {
                cdpTelemetryFeatureDetailsBuilder.setClusterLogsCollection(environmentFeatures.getClusterLogsCollection().isEnabled().toString());
            }
        }

        return cdpTelemetryFeatureDetailsBuilder.build();
    }
}

package com.sequenceiq.cloudbreak.service.freeipa;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@Component
public class FreeIpaConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaConfigProvider.class);

    @Inject
    private FreeipaClientService freeipaClient;

    public Map<String, Object> createFreeIpaConfig(String environmentCrn) {
        Optional<DescribeFreeIpaResponse> freeIpaResponse = freeipaClient.findByEnvironmentCrn(environmentCrn);
        if (freeIpaResponse.isPresent()) {
            Optional<InstanceMetaDataResponse> instanceMetaDataResponse = selectInstance(freeIpaResponse.get());
            if (instanceMetaDataResponse.isEmpty()) {
                LOGGER.debug("No FreeIPA instance available");
                return Map.of();
            } else {
                LOGGER.debug("Setting [{}] FreeIPA FQDN as default FreeIPA server", instanceMetaDataResponse.get().getDiscoveryFQDN());
                return Map.of("host", instanceMetaDataResponse.get().getDiscoveryFQDN());
            }
        } else {
            LOGGER.info("FreeIPA describe didn't return result");
            return Map.of();
        }
    }

    private Optional<InstanceMetaDataResponse> selectInstance(DescribeFreeIpaResponse freeIpaResponse) {
        List<InstanceMetaDataResponse> metaDataResponses = freeIpaResponse.getInstanceGroups().stream()
                .flatMap(ig -> ig.getMetaData().stream()).collect(Collectors.toList());
        Optional<InstanceMetaDataResponse> pgwInstance = selectPgwInstance(metaDataResponses);
        Optional<InstanceMetaDataResponse> instanceMetaDataResponse =
                pgwInstance.or(() -> chooseFirstOrderedByFqdnAndByStateIfPresent(metaDataResponses, Optional.of(InstanceStatus.CREATED))
                        .or(() -> chooseFirstOrderedByFqdnAndByStateIfPresent(metaDataResponses, Optional.empty())));
        LOGGER.info("Chosen instance for default FreeIPA server: {}", instanceMetaDataResponse);
        return instanceMetaDataResponse;
    }

    private Optional<InstanceMetaDataResponse> selectPgwInstance(List<InstanceMetaDataResponse> metaDataResponses) {
        List<InstanceMetaDataResponse> pgwInstances = metaDataResponses.stream().
                filter(im -> InstanceMetadataType.GATEWAY_PRIMARY.equals(im.getInstanceType()))
                .collect(Collectors.toList());
        Optional<InstanceMetaDataResponse> pgwInstance = chooseFirstOrderedByFqdnAndByStateIfPresent(pgwInstances, Optional.of(InstanceStatus.CREATED));
        LOGGER.debug("Tried to select primary gateway instance [{}] with state [{}] from {}", pgwInstance, InstanceStatus.CREATED, pgwInstances);
        return pgwInstance;
    }

    private Optional<InstanceMetaDataResponse> chooseFirstOrderedByFqdnAndByStateIfPresent(List<InstanceMetaDataResponse> metaDataResponses,
            Optional<InstanceStatus> state) {
        LOGGER.debug("Look for instance with state [{}]", state);
        return metaDataResponses.stream()
                .filter(im -> state.isEmpty() || state.get() == im.getInstanceStatus())
                .min(Comparator.comparing(InstanceMetaDataResponse::getDiscoveryFQDN));
    }
}

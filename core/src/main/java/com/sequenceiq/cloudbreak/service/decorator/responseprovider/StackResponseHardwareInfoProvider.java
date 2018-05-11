package com.sequenceiq.cloudbreak.service.decorator.responseprovider;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.HardwareInfoResponse;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.host.HostMetadataResponse;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceMetaDataJson;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponse;
import com.sequenceiq.cloudbreak.api.model.stack.StackResponseEntries;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.HostMetadataRepository;

@Component
public class StackResponseHardwareInfoProvider implements ResponseProvider {

    @Inject
    private HostMetadataRepository hostMetadataRepository;

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Override
    public StackResponse providerEntriesToStackResponse(Stack stack, StackResponse stackResponse) {
        Set<HardwareInfoResponse> hardwareInfoResponses = new HashSet<>();
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            for (InstanceMetaData instanceMetaData : instanceGroup.getAllInstanceMetaData()) {
                HostMetadata hostMetadata = null;
                if (stack.getCluster() != null && instanceMetaData.getDiscoveryFQDN() != null) {
                    hostMetadata = hostMetadataRepository.findHostInClusterByName(stack.getCluster().getId(), instanceMetaData.getDiscoveryFQDN());
                }
                HardwareInfoResponse hardwareInfoResponse = new HardwareInfoResponse();
                hardwareInfoResponse.setInstanceMetaData(conversionService.convert(instanceMetaData, InstanceMetaDataJson.class));
                hardwareInfoResponse.setHostMetadata(conversionService.convert(hostMetadata, HostMetadataResponse.class));
                hardwareInfoResponses.add(hardwareInfoResponse);
            }
        }
        stackResponse.setHardwareInfos(hardwareInfoResponses);
        return stackResponse;
    }

    @Override
    public String type() {
        return StackResponseEntries.HARDWARE_INFO.getEntryName();
    }
}

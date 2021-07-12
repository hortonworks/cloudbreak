package com.sequenceiq.freeipa.service.multiaz;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Network;

@Service
public class MultiAzCalculatorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiAzCalculatorService.class);

    @Inject
    private MultiAzValidator multiAzValidator;

    public void calculateByRoundRobin(Network network, InstanceGroup instanceGroup) {
        String subnetId = getSubnetId(network);
        if (!Strings.isNullOrEmpty(subnetId) && multiAzValidator.supportedForInstanceMetadataGeneration(network.cloudPlatform())) {
            for (InstanceMetaData instanceMetaData : instanceGroup.getAllInstanceMetaData()) {
                if (Strings.isNullOrEmpty(instanceMetaData.getSubnetId())) {
                    instanceMetaData.setSubnetId(subnetId);
                    //instanceMetaData.setAvailabilityZone(subnetAzPairs.get(leastUsedSubnetId));
                }
            }
        }
    }

    private String getSubnetId(Network network) {
        String subnetId = null;
        Json attributes = network.getAttributes();
        if (attributes != null && attributes.getMap() != null) {
            subnetId = (String) attributes.getMap().get("subnetId");
        }
        return subnetId;
    }
}

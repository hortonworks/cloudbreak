package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.InstanceMetaDataJson;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;

@Component
public class MetaDataToJsonConverter extends AbstractConversionServiceAwareConverter<InstanceMetaData, InstanceMetaDataJson> {

    @Override
    public InstanceMetaDataJson convert(InstanceMetaData entity) {
        InstanceMetaDataJson metaDataJson = new InstanceMetaDataJson();
        metaDataJson.setPrivateIp(entity.getPrivateIp());
        metaDataJson.setPublicIp(entity.getPublicIp());
        metaDataJson.setVolumeCount(entity.getVolumeCount());
        metaDataJson.setAmbariServer(entity.getAmbariServer());
        metaDataJson.setInstanceId(entity.getInstanceId());
        metaDataJson.setDockerSubnet(entity.getDockerSubnet());
        metaDataJson.setDiscoveryFQDN(entity.getDiscoveryFQDN());
        metaDataJson.setContainerCount(entity.getContainerCount());
        metaDataJson.setInstanceGroup(entity.getInstanceGroup().getGroupName());
        return metaDataJson;
    }
}

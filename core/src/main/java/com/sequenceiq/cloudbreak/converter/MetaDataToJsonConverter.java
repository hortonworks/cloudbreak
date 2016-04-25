package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.InstanceMetaDataJson;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;

@Component
public class MetaDataToJsonConverter extends AbstractConversionServiceAwareConverter<InstanceMetaData, InstanceMetaDataJson> {

    @Override
    public InstanceMetaDataJson convert(InstanceMetaData entity) {
        InstanceMetaDataJson metaDataJson = new InstanceMetaDataJson();
        metaDataJson.setPrivateIp(entity.getPrivateIp());
        metaDataJson.setPublicIp(entity.getPublicIpWrapper());
        metaDataJson.setAmbariServer(entity.getAmbariServer());
        metaDataJson.setInstanceId(entity.getInstanceId());
        metaDataJson.setDiscoveryFQDN(entity.getDiscoveryFQDN());
        metaDataJson.setInstanceGroup(entity.getInstanceGroup().getGroupName());
        metaDataJson.setInstanceStatus(entity.getInstanceStatus());
        return metaDataJson;
    }
}

package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.InstanceMetaDataJson;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;

@Component
public class MetaDataToJsonConverter extends AbstractConversionServiceAwareConverter<InstanceMetaData, InstanceMetaDataJson> {

    private static final String NOT_AVAILABLE = "N/A";

    @Override
    public InstanceMetaDataJson convert(InstanceMetaData entity) {
        InstanceMetaDataJson metaDataJson = new InstanceMetaDataJson();
        metaDataJson.setPrivateIp(entity.getPrivateIp());
        if (entity.getPublicIp() != null) {
            metaDataJson.setPublicIp(entity.getPublicIp());
        } else if (entity.getPrivateIp() != null) {
            metaDataJson.setPublicIp(NOT_AVAILABLE);
        }
        metaDataJson.setSshPort(entity.getSshPort());
        metaDataJson.setAmbariServer(entity.getAmbariServer());
        metaDataJson.setInstanceId(entity.getInstanceId());
        metaDataJson.setDiscoveryFQDN(entity.getDiscoveryFQDN());
        metaDataJson.setInstanceGroup(entity.getInstanceGroup().getGroupName());
        metaDataJson.setInstanceStatus(entity.getInstanceStatus());
        return metaDataJson;
    }
}

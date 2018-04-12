package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.api.model.InstanceMetaDataJson;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import org.springframework.stereotype.Component;

@Component
public class InstanceMetaDataToInstanceMetaDataJsonConverter extends AbstractConversionServiceAwareConverter<InstanceMetaData, InstanceMetaDataJson> {

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
        metaDataJson.setInstanceType(entity.getInstanceMetadataType());
        return metaDataJson;
    }
}

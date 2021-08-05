package com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.instancemetadata.InstanceMetaDataV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

@Component
public class InstanceMetaDataToInstanceMetaDataV4ResponseConverter
        extends AbstractConversionServiceAwareConverter<InstanceMetaData, InstanceMetaDataV4Response> {

    private static final String NOT_AVAILABLE = "N/A";

    @Override
    public InstanceMetaDataV4Response convert(InstanceMetaData source) {
        InstanceMetaDataV4Response metaDataJson = new InstanceMetaDataV4Response();
        metaDataJson.setPrivateIp(source.getPrivateIp());
        if (source.getPublicIp() != null) {
            metaDataJson.setPublicIp(source.getPublicIp());
        } else if (source.getPrivateIp() != null) {
            metaDataJson.setPublicIp(NOT_AVAILABLE);
        }
        metaDataJson.setSshPort(source.getSshPort());
        metaDataJson.setAmbariServer(source.getAmbariServer());
        metaDataJson.setInstanceId(source.getInstanceId());
        metaDataJson.setDiscoveryFQDN(source.getDiscoveryFQDN());
        metaDataJson.setInstanceGroup(source.getInstanceGroup().getGroupName());
        metaDataJson.setSubnetId(source.getSubnetId());
        metaDataJson.setAvailabilityZone(source.getAvailabilityZone());
        metaDataJson.setRackId(source.getRackId());
        metaDataJson.setInstanceStatus(source.getInstanceStatus());
        metaDataJson.setInstanceType(source.getInstanceMetadataType());
        metaDataJson.setLifeCycle(source.getLifeCycle());
        return metaDataJson;
    }

}

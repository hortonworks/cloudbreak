package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.InstanceMetaDataJson;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;

@Component
public class MetaDataConverter extends AbstractConverter<InstanceMetaDataJson, InstanceMetaData> {

    @Override
    public InstanceMetaDataJson convert(InstanceMetaData entity) {
        InstanceMetaDataJson metaDataJson = new InstanceMetaDataJson();
        metaDataJson.setPrivateIp(entity.getPrivateIp());
        metaDataJson.setPublicIp(entity.getPublicIp());
        metaDataJson.setVolumeCount(entity.getVolumeCount());
        metaDataJson.setAmbariServer(entity.getAmbariServer());
        metaDataJson.setInstanceId(entity.getInstanceId());
        metaDataJson.setDockerSubnet(entity.getDockerSubnet());
        metaDataJson.setLongName(entity.getLongName());
        metaDataJson.setContainerCount(entity.getContainerCount());
        metaDataJson.setInstanceGroup(entity.getInstanceGroup().getGroupName());
        return metaDataJson;
    }

    @Override
    public InstanceMetaData convert(InstanceMetaDataJson json) {
        throw new UnsupportedOperationException("Instance metadata shouldn't be created from json.");
    }
}

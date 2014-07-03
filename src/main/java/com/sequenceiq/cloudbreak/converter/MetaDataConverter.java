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
        metaDataJson.setAmbariServer(entity.getAmbariServer());
        return metaDataJson;
    }

    @Override
    public InstanceMetaData convert(InstanceMetaDataJson json) {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setPrivateIp(json.getPrivateIp());
        instanceMetaData.setPublicIp(json.getPublicIp());
        instanceMetaData.setAmbariServer(json.getAmbariServer());
        return instanceMetaData;
    }
}

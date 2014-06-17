package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.MetaDataJson;
import com.sequenceiq.cloudbreak.domain.MetaData;

@Component
public class MetaDataConverter extends AbstractConverter<MetaDataJson, MetaData> {

    @Override
    public MetaDataJson convert(MetaData entity) {
        MetaDataJson metaDataJson = new MetaDataJson();
        metaDataJson.setPrivateIp(entity.getPrivateIp());
        metaDataJson.setPublicIp(entity.getPublicIp());
        return metaDataJson;
    }

    @Override
    public MetaData convert(MetaDataJson json) {
        MetaData metaData = new MetaData();
        metaData.setPrivateIp(json.getPrivateIp());
        metaData.setPublicIp(json.getPublicIp());
        return metaData;
    }
}

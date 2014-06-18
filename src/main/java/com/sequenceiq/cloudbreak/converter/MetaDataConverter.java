package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.MetadataJson;
import com.sequenceiq.cloudbreak.domain.Metadata;

@Component
public class MetaDataConverter extends AbstractConverter<MetadataJson, Metadata> {

    @Override
    public MetadataJson convert(Metadata entity) {
        MetadataJson metaDataJson = new MetadataJson();
        metaDataJson.setPrivateIp(entity.getPrivateIp());
        metaDataJson.setPublicIp(entity.getPublicIp());
        return metaDataJson;
    }

    @Override
    public Metadata convert(MetadataJson json) {
        Metadata metaData = new Metadata();
        metaData.setPrivateIp(json.getPrivateIp());
        metaData.setPublicIp(json.getPublicIp());
        return metaData;
    }
}

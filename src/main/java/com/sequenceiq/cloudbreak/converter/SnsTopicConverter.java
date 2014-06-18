package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.SnsTopicJson;
import com.sequenceiq.cloudbreak.domain.SnsTopic;

@Component
public class SnsTopicConverter extends AbstractConverter<SnsTopicJson, SnsTopic> {

    @Override
    public SnsTopicJson convert(SnsTopic entity) {
        SnsTopicJson snsTopicJson = new SnsTopicJson();
        snsTopicJson.setId(entity.getId());
        snsTopicJson.setName(entity.getName());
        snsTopicJson.setRegion(entity.getRegion());
        snsTopicJson.setTopicArn(entity.getTopicArn());
        return snsTopicJson;
    }

    @Override
    public SnsTopic convert(SnsTopicJson json) {
        return null;
    }

}

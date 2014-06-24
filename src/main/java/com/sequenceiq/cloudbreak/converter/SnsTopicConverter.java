package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.amazonaws.services.sqs.model.UnsupportedOperationException;
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
        snsTopicJson.setConfirmed(entity.isConfirmed());
        return snsTopicJson;
    }

    @Override
    public SnsTopic convert(SnsTopicJson json) {
        throw new UnsupportedOperationException("Sns topics shouldn't be created from Json.");
    }

}

package com.sequenceiq.periscope.rest.converter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.model.QueueSetup;
import com.sequenceiq.periscope.rest.json.QueueSetupJson;

@Component
public class QueueSetupConverter extends AbstractConverter<QueueSetupJson, QueueSetup> {

    @Autowired
    private QueueConverter queueConverter;

    @Override
    public QueueSetup convert(QueueSetupJson source) {
        return new QueueSetup(queueConverter.convertAllFromJson(source.getSetup()));
    }
}

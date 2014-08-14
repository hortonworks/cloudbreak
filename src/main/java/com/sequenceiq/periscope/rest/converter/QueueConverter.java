package com.sequenceiq.periscope.rest.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.periscope.model.Queue;
import com.sequenceiq.periscope.rest.json.QueueJson;

@Component
public class QueueConverter extends AbstractConverter<QueueJson, Queue> {

    @Override
    public Queue convert(QueueJson source) {
        return new Queue(source.getName(), source.getCapacity());
    }
}

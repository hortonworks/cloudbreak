package com.sequenceiq.cloudbreak.structuredevent.db.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.audit.StructuredEventResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Component
public class StructuredEventToStructuredEventResponseConverter extends AbstractConversionServiceAwareConverter<StructuredEvent, StructuredEventResponse> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StructuredEventToStructuredEventResponseConverter.class);

    @Override
    public StructuredEventResponse convert(StructuredEvent source) {
        StructuredEventResponse response = new StructuredEventResponse();
        response.setType(source.getType());
        response.setOperation(source.getOperation());
        response.setStatus(source.getStatus());
        response.setDuration(source.getDuration());
        try {
            response.setEventJson(JsonUtil.writeValueAsString(source));
        } catch (JsonProcessingException e) {
            LOGGER.error("Can't convert object", e);
        }
        return response;
    }
}

package com.sequenceiq.cloudbreak.structuredevent.db.converter;

import javax.inject.Inject;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@Component
public class StructuredEventToStructuredEventEntityConverter extends AbstractConversionServiceAwareConverter<StructuredEvent, StructuredEventEntity> {
    @Inject
    private StackService stackService;

    @Inject
    private ConversionService conversionService;

    @Override
    public StructuredEventEntity convert(StructuredEvent source) {
        try {
            return new StructuredEventEntity(new Json(source));
        } catch (JsonProcessingException e) {
            // TODO What should we do in case of json processing error
            return null;
        }
    }
}

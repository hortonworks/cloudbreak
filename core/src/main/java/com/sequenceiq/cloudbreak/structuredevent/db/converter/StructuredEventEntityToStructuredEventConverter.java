package com.sequenceiq.cloudbreak.structuredevent.db.converter;

import java.io.IOException;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.StructuredEventEntity;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Component
public class StructuredEventEntityToStructuredEventConverter extends AbstractConversionServiceAwareConverter<StructuredEventEntity, StructuredEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StructuredEventEntityToStructuredEventConverter.class);

    private Map<String, Class<? extends StructuredEvent>> classes = Maps.newHashMap();

    @PostConstruct
    public void init() {
        classes.put(StructuredFlowEvent.class.getSimpleName(), StructuredFlowEvent.class);
        classes.put(StructuredNotificationEvent.class.getSimpleName(), StructuredNotificationEvent.class);
    }

    @Override
    public StructuredEvent convert(StructuredEventEntity source) {
        try {
            JsonNode jsonNode = JsonUtil.readTree(source.getStructuredEventJson().getValue());
            String eventType = jsonNode.path(StructuredEvent.TYPE_FIELD).textValue();
            Class<? extends StructuredEvent> eventClass = classes.get(eventType);
            return JsonUtil.treeToValue(jsonNode, eventClass);

        } catch (IOException e) {
            LOGGER.error("Cannot convert structured event entity to structured event.", e);
            return null;
        }
    }
}

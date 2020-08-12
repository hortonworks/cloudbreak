package com.sequenceiq.cloudbreak.structuredevent.service.converter;

import java.io.IOException;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.structuredevent.domain.CDPStructuredEventEntity;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredRestCallEvent;

@Component
public class CDPStructuredEventEntityToCDPStructuredEventConverter extends AbstractConversionServiceAwareConverter<CDPStructuredEventEntity, CDPStructuredEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CDPStructuredEventEntityToCDPStructuredEventConverter.class);

    private final Map<String, Class<? extends CDPStructuredEvent>> classes = Maps.newHashMap();

    @PostConstruct
    public void init() {
        classes.put(CDPStructuredFlowEvent.class.getSimpleName(), CDPStructuredFlowEvent.class);
        classes.put(CDPStructuredNotificationEvent.class.getSimpleName(), CDPStructuredNotificationEvent.class);
        classes.put(CDPStructuredRestCallEvent.class.getSimpleName(), CDPStructuredRestCallEvent.class);
        //needed for backward compatibility
        classes.put("CDPStructuredFlowErrorEvent", CDPStructuredFlowEvent.class);
    }

    @Override
    public CDPStructuredEvent convert(CDPStructuredEventEntity source) {
        try {
            JsonNode jsonNode = JsonUtil.readTree(source.getStructuredEventJson().getValue());
            String eventType = jsonNode.path(CDPStructuredEvent.TYPE_FIELD).textValue();
            Class<? extends CDPStructuredEvent> eventClass = classes.get(eventType);
            return JsonUtil.treeToValue(jsonNode, eventClass);

        } catch (IOException e) {
            LOGGER.error("Cannot convert structured event entity to structured event.", e);
            return null;
        }
    }
}

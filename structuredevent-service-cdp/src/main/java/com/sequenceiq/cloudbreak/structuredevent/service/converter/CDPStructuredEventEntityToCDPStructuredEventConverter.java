package com.sequenceiq.cloudbreak.structuredevent.service.converter;

import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.structuredevent.domain.CDPStructuredEventEntity;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredRestCallEvent;

@Component
public class CDPStructuredEventEntityToCDPStructuredEventConverter {

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

    public CDPStructuredEvent convert(CDPStructuredEventEntity source) {
        try {
            JsonNode jsonNode = JsonUtil.readTree(source.getStructuredEventJson().getValue());
            String eventType = jsonNode.path(CDPStructuredEvent.TYPE_FIELD).textValue();
            Class<? extends CDPStructuredEvent> eventClass = classes.get(eventType);
            if (eventClass == null) {
                String msg = String.format("Cannot convert structured event entity to type: '%s (%s)' structured event.", source.getEventType(), eventType);
                LOGGER.info(msg);
                throw new CloudbreakServiceException(msg);
            }
            return JsonUtil.treeToValue(jsonNode, eventClass);
        } catch (CloudbreakServiceException e) {
            throw e;
        } catch (Exception e) {
            String msg = String.format("Error occurred during the structured event conversion: '%s (%s)'", source.getEventType(),
                    source.getClass().getSimpleName());
            LOGGER.error(msg, e);
            throw new CloudbreakServiceException(msg, e);
        }
    }
}

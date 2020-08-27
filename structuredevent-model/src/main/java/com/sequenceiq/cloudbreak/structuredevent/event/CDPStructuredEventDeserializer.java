package com.sequenceiq.cloudbreak.structuredevent.event;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredRestCallEvent;

public class CDPStructuredEventDeserializer extends StdDeserializer<CDPStructuredEvent> {

    private final Map<String, Class<? extends CDPStructuredEvent>> classes = new HashMap<>();

    public CDPStructuredEventDeserializer() {
        this(null);
        init();
    }

    public CDPStructuredEventDeserializer(Class<?> vc) {
        super(vc);
        init();
    }

    public void init() {
        classes.put(CDPStructuredFlowEvent.class.getSimpleName(), CDPStructuredFlowEvent.class);
        classes.put(CDPStructuredNotificationEvent.class.getSimpleName(), CDPStructuredNotificationEvent.class);
        classes.put(CDPStructuredRestCallEvent.class.getSimpleName(), CDPStructuredRestCallEvent.class);
        //needed for backward compatibility
        classes.put("CDPStructuredFlowErrorEvent", CDPStructuredFlowEvent.class);
    }

    @Override
    public CDPStructuredEvent deserialize(JsonParser jp, DeserializationContext ctxt) {
        try {
            JsonNode node = jp.getCodec().readTree(jp);
            String type = node.get(StructuredEvent.TYPE_FIELD).asText();
            Class<? extends CDPStructuredEvent> eventClass = classes.get(type);
            return treeToValue(node, eventClass);
        } catch (JsonSyntaxException | IOException e) {
            return null;
        }
    }

    private <T> T treeToValue(TreeNode n, Class<T> valueType) {
        // We don't want to use the deserializer jackson again (StackOverflow exception)
        return new Gson().fromJson(n.toString(), valueType);
    }
}

package com.sequenceiq.cloudbreak.structuredevent.event;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class StructuredEventDeserializer extends StdDeserializer<StructuredEvent> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final Map<String, Class<? extends StructuredEvent>> classes = new HashMap<>();

    public StructuredEventDeserializer() {
        this(null);
        init();
    }

    public StructuredEventDeserializer(Class<?> vc) {
        super(vc);
        init();
    }

    public void init() {
        classes.put(StructuredFlowEvent.class.getSimpleName(), StructuredFlowEvent.class);
        classes.put(StructuredNotificationEvent.class.getSimpleName(), StructuredNotificationEvent.class);
        classes.put(StructuredRestCallEvent.class.getSimpleName(), StructuredRestCallEvent.class);
        //needed for backward compatibility
        classes.put("StructuredFlowErrorEvent", StructuredFlowEvent.class);
    }

    @Override
    public StructuredEvent deserialize(JsonParser jp, DeserializationContext ctxt) {
        try {
            JsonNode node = jp.getCodec().readTree(jp);
            String type = node.get(StructuredEvent.TYPE_FIELD).asText();
            Class<? extends StructuredEvent> eventClass = classes.get(type);
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

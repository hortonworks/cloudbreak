package com.sequenceiq.freeipa.client.deserializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;

public class ListFlatteningDeserializer<T> extends JsonDeserializer<T> implements ContextualDeserializer {

    private static final BooleanDeserializationProblemHandler BOOLEAN_DESERIALIZATION_PROBLEM_HANDLER = new BooleanDeserializationProblemHandler();

    private Class<T> targetClass;

    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectCodec oc = p.getCodec();
        JsonNode node = oc.readTree(p);

        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        mapper.addHandler(BOOLEAN_DESERIALIZATION_PROBLEM_HANDLER);

        if (node.iterator().hasNext()) {
            JsonNode actual = node;
            while (actual.iterator().hasNext()) {
                actual = actual.iterator().next();
            }
            return mapper.convertValue(actual, targetClass);
        }

        return mapper.convertValue(node, targetClass);
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) {
        targetClass = (Class<T>) property.getType().getRawClass();
        return this;
    }
}

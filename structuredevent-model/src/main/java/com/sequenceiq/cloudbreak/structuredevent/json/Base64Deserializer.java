package com.sequenceiq.cloudbreak.structuredevent.json;

import java.io.IOException;
import java.util.Base64;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class Base64Deserializer extends JsonDeserializer<String> {
    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String str = p.getText();
        return str != null ? new String(Base64.getDecoder().decode(str)) : null;
    }
}

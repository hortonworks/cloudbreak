package com.sequenceiq.cloudbreak.structuredevent.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.sequenceiq.cloudbreak.common.base64.Base64Util;

public class Base64Deserializer extends JsonDeserializer<String> {
    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String str = p.getText();
        return str != null ? Base64Util.decode(str) : null;
    }
}

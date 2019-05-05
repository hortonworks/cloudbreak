package com.sequenceiq.cloudbreak.structuredevent.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.sequenceiq.cloudbreak.common.anonymizer.AnonymizerUtil;

public class AnonymizingBase64Serializer extends Base64Serializer {
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        String anonymizedValue = null;
        if (value != null) {
            anonymizedValue = AnonymizerUtil.anonymize(value);
        }
        super.serialize(anonymizedValue, gen, serializers);
    }
}

package com.sequenceiq.flow.rotation.serialization;

import static com.sequenceiq.cloudbreak.rotation.secret.serialization.SecretRotationEnumSerializationUtil.getEnum;
import static com.sequenceiq.cloudbreak.rotation.secret.serialization.SecretRotationEnumSerializationUtil.listStringToList;
import static com.sequenceiq.cloudbreak.rotation.secret.serialization.SecretRotationEnumSerializationUtil.mapStringToMap;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;

public class SecretTypeListDeserializer extends JsonDeserializer<List<Enum<? extends SecretType>>> {

    @Override
    public List<Enum<? extends SecretType>> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        try {
            List<String> mapAsStringList = listStringToList(text);
            List<Enum<? extends SecretType>> result = Lists.newArrayList();
            for (String mapAsString : mapAsStringList) {
                result.add(getEnum(mapStringToMap(mapAsString)));
            }
            return result;
        } catch (Exception e) {
            throw new IOException(String.format("Cannot deserialize from [%s] to an instance of enum list.", text), e);
        }
    }
}

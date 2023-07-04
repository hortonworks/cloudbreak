package com.sequenceiq.cloudbreak.rotation.serialization;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

public class SecretRotationEnumSerializationUtil {

    protected static final String CLASS_KEY = "clazz";

    protected static final String VALUE_KEY = "value";

    private static final String KEYVALUE_SEPARATOR = "=";

    private static final String FIELD_SEPARATOR = ",";

    private static final String OBJECT_SEPARATOR = ";";

    private SecretRotationEnumSerializationUtil() {

    }

    public static void serialize(Enum value, JsonGenerator gen) throws IOException {
        if (value != null) {
            gen.writeString(enumToMapString(value));
        } else {
            gen.writeNull();
        }
    }

    public static String enumToMapString(Enum value) {
        return mapToString(Map.of(CLASS_KEY, value.getClass().getName(), VALUE_KEY, value.name()));
    }

    public static Enum deserialize(String text) throws IOException {
        try {
            Map<String, String> enumMap = mapStringToMap(text);
            return getEnum(enumMap);
        } catch (Exception e) {
            throw new IOException(String.format("Cannot deserialize from [%s] to an instance of enum.", text), e);
        }
    }

    public static Enum getEnum(Map<String, String> rotationEnumMap) throws ClassNotFoundException {
        Class<Enum> enumClass = (Class<Enum>) Class.forName(rotationEnumMap.get(CLASS_KEY));
        String enumValue = rotationEnumMap.get(VALUE_KEY);
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(enumConstant -> StringUtils.equals(enumConstant.name(), enumValue))
                .findFirst()
                .orElseThrow(() -> new CloudbreakServiceException(String.format("There is no enum for value %s", enumValue)));
    }

    public static String mapToString(Map<String, String> map) {
        return Joiner.on(FIELD_SEPARATOR).withKeyValueSeparator(KEYVALUE_SEPARATOR).join(map);
    }

    public static Map<String, String> mapStringToMap(String mapAsString) {
        return Splitter.on(FIELD_SEPARATOR).withKeyValueSeparator(KEYVALUE_SEPARATOR).split(mapAsString);
    }

    public static String listToString(List<String> list) {
        return Joiner.on(OBJECT_SEPARATOR).join(list);
    }

    public static List<String> listStringToList(String listAsString) {
        return Lists.newArrayList(Splitter.on(OBJECT_SEPARATOR).split(listAsString));
    }
}

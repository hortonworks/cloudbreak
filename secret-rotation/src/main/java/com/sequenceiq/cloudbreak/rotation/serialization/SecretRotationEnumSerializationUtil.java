package com.sequenceiq.cloudbreak.rotation.serialization;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.rotation.SerializableRotationEnum;

public class SecretRotationEnumSerializationUtil {

    private static final String CLASS_VALUE_SEPARATOR = ".";

    private static final String LIST_SEPARATOR = ",";

    private SecretRotationEnumSerializationUtil() {

    }

    public static void serialize(SerializableRotationEnum value, JsonGenerator gen) throws IOException {
        if (value != null) {
            gen.writeString(serialize(value));
        } else {
            gen.writeNull();
        }
    }

    public static String serialize(SerializableRotationEnum value) {
        return value.getClass().getName() + CLASS_VALUE_SEPARATOR + value.value();
    }

    public static String serializeList(List<SerializableRotationEnum> value) {
        List<String> stringList = value.stream().map(SecretRotationEnumSerializationUtil::serialize).collect(Collectors.toList());
        return Joiner.on(LIST_SEPARATOR).join(stringList);
    }

    public static List<SerializableRotationEnum> deserializeList(String input) throws IOException {
        List<SerializableRotationEnum> result = Lists.newArrayList();
        for (String enumString : input.split(LIST_SEPARATOR)) {
            result.add(SecretRotationEnumSerializationUtil.deserialize(enumString));
        }
        return result;
    }

    public static SerializableRotationEnum deserialize(String text) throws IOException {
        try {
            String className = text.substring(0, text.lastIndexOf(CLASS_VALUE_SEPARATOR));
            String value = text.substring(text.lastIndexOf(CLASS_VALUE_SEPARATOR) + 1);
            return getEnum(className, value);
        } catch (Exception e) {
            throw new IOException(String.format("Cannot deserialize from [%s] to an instance of enum.", text), e);
        }
    }

    private static SerializableRotationEnum getEnum(String className, String value) throws ClassNotFoundException {
        Class<SerializableRotationEnum> enumClass = (Class<SerializableRotationEnum>) Class.forName(className);
        return Arrays.stream(enumClass.getEnumConstants())
                .filter(enumConstant -> StringUtils.equals(enumConstant.value(), value))
                .findFirst()
                .orElseThrow(() -> new CloudbreakServiceException(String.format("There is no enum for value %s", value)));
    }
}

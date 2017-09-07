package com.sequenceiq.cloudbreak.shell.util;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TagParser {
    private TagParser() {

    }

    public static Map<String, String> parseTagsIntoMap(String tagString) {
        try {
            if (tagString == null || tagString.isEmpty()) {
                return new HashMap<>();
            }
            return Stream.of(tagString.split(",")).map(kv -> kv.split("=")).collect(Collectors.toMap(kv -> kv[0].trim(), kv -> kv[1].trim()));
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Tags couldn't be parsed. Please use the proper format 'key1=value1,key2=value2'");
        }
    }
}

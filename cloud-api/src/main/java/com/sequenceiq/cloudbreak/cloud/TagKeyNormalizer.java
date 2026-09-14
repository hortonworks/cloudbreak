package com.sequenceiq.cloudbreak.cloud;

@FunctionalInterface
public interface TagKeyNormalizer {

    TagKeyNormalizer IDENTITY = key -> key;

    String normalize(String tagKey);
}

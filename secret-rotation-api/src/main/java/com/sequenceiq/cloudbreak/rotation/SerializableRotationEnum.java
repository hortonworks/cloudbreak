package com.sequenceiq.cloudbreak.rotation;

public interface SerializableRotationEnum {

    Class<? extends Enum<?>> getClazz();

    String value();
}

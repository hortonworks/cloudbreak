package com.sequenceiq.cloudbreak.controller.validation;

import com.amazonaws.services.ec2.model.InstanceType;
import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.domain.AwsVolumeType;

public enum AwsTemplateParam implements TemplateParam {

    INSTANCE_TYPE("instanceType", true, InstanceType.class, Optional.<String>absent()),
    SSH_LOCATION("sshLocation", false, String.class, Optional.of("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])/([01]?\\d\\d?|2[0-4]\\d|25[0-5])$")),
    VOLUME_TYPE("volumeType", true, AwsVolumeType.class, Optional.<String>absent()),
    SPOT_PRICE("spotPrice", false, Double.class, Optional.<String>absent());

    private final String paramName;
    private final Class clazz;
    private final boolean required;
    private final Optional<String> regex;

    private AwsTemplateParam(String paramName, Boolean required, Class clazz, Optional<String> regex) {
        this.paramName = paramName;
        this.required = required;
        this.clazz = clazz;
        this.regex = regex;
    }

    @Override
    public String getName() {
        return paramName;
    }

    @Override
    public Class getClazz() {
        return clazz;
    }

    @Override
    public Boolean getRequired() {
        return required;
    }

    @Override
    public Optional<String> getRegex() {
        return regex;
    }

}

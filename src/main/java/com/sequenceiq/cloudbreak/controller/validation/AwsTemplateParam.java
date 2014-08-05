package com.sequenceiq.cloudbreak.controller.validation;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.InstanceType;
import com.google.common.base.Optional;

public enum AwsTemplateParam implements TemplateParam {

    REGION("region", true, Regions.class, Optional.<String>absent()),
    AMI_ID("amiId", true, String.class, Optional.<String>absent()),
    INSTANCE_TYPE("instanceType", true, InstanceType.class, Optional.<String>absent()),
    SSH_LOCATION("sshLocation", false, String.class, Optional.of("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])/([01]?\\d\\d?|2[0-4]\\d|25[0-5])$"));

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

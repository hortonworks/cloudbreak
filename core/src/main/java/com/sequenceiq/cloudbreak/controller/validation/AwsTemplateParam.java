package com.sequenceiq.cloudbreak.controller.validation;

import com.google.common.base.Optional;

public enum AwsTemplateParam implements TemplateParam {

    INSTANCE_TYPE("instanceType", true, String.class, Optional.of("^(?:m3\\.medium|m3\\.large|m3\\.xlarge|m3\\.2xlarge|i2\\.xlarge|i2\\.2xlarge|"
            + "i2\\.4xlarge|i2\\.8xlarge|hi1\\.4xlarge|hs1\\.8xlarge|c3\\.large|c3\\.xlarge|c3\\.2xlarge|c3\\.4xlarge|c3\\.8xlarge|cc2\\.8xlarge|"
            + "cg1\\.4xlarge|cr1\\.8xlarge|g2 \\.2xlarge|r3\\.large|r3\\.xlarge|r3\\.2xlarge|r3\\.4xlarge|r3\\.8xlarge|d2\\.xlarge|d2\\.2xlarge|"
            + "d2\\.4xlarge|d2\\.8xlarge)")),
    SSH_LOCATION("sshLocation", false, String.class, Optional.of("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\."
            + "([01]?\\d\\d?|2[0-4]\\d|25[0-5])/([01]?\\d\\d?|2[0-4]\\d|25[0-5])$")),
    VOLUME_TYPE("volumeType", true, String.class, Optional.of("(?:ephemeral|standard|gp2)")),
    ENCRYPTED("encrypted", false, Boolean.class, Optional.<String>absent()),
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

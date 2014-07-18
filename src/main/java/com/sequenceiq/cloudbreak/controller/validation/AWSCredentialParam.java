package com.sequenceiq.cloudbreak.controller.validation;

import java.util.Set;

import com.google.common.base.Optional;

public enum AWSCredentialParam implements TemplateParam {

    ROLE_ARN("roleArn", true, String.class, Optional.<String>absent()),
    SNS_TOPICS("snsTopics", false, Set.class, Optional.<String>absent()),
    SSH_KEY_NAME("sshKeyName", true, String.class, Optional.<String>absent());

    private final String paramName;
    private final Class clazz;
    private final boolean required;
    private final Optional<String> regex;

    private AWSCredentialParam(String paramName, Boolean required, Class clazz, Optional<String> regex) {
        this.paramName = paramName;
        this.clazz = clazz;
        this.required = required;
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

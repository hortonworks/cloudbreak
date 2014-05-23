package com.sequenceiq.provisioning.controller.validation;

public enum RequiredAWSCredentialParam implements TemplateParam {

    ROLE_ARN("roleArn", true, String.class);

    private final String paramName;
    private final Class clazz;
    private final boolean required;
    private final Optional<String> regex;

    private RequiredAWSCredentialParam(String paramName, Boolean required, Class clazz) {
        this.paramName = paramName;
        this.clazz = clazz;
        this.required = required;
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
    public String getRegex() {

    }

}

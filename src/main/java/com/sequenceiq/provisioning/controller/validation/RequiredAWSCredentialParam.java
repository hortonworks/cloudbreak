package com.sequenceiq.provisioning.controller.validation;

public enum RequiredAWSCredentialParam implements TemplateParam {

    ROLE_ARN("roleArn", String.class);

    private final String paramName;
    private final Class clazz;

    private RequiredAWSCredentialParam(String paramName, Class clazz) {
        this.paramName = paramName;
        this.clazz = clazz;
    }

    @Override
    public String getName() {
        return paramName;
    }

    @Override
    public Class getClazz() {
        return clazz;
    }

}

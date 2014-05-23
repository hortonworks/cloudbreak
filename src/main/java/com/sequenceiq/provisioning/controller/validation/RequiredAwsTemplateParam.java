package com.sequenceiq.provisioning.controller.validation;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.InstanceType;

public enum RequiredAwsTemplateParam implements TemplateParam {

    KEY_NAME("keyName", true, String.class),
    REGION("region", true, Regions.class),
    AMI_ID("amiId", true, String.class),
    INSTANCE_TYPE("instanceType", true, InstanceType.class),
    SSH_LOCATION("sshLocation", false, String.class);

    private final String paramName;
    private final Class clazz;
    private final boolean required;

    private RequiredAwsTemplateParam(String paramName, Boolean required, Class clazz) {
        this.paramName = paramName;
        this.required = required;
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

    @Override
    public Boolean getRequired() {
        return required;
    }
}

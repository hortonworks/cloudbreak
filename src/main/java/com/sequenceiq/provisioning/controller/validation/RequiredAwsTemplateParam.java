package com.sequenceiq.provisioning.controller.validation;

import com.amazonaws.regions.Region;
import com.amazonaws.services.ec2.model.InstanceType;

public enum RequiredAwsTemplateParam implements TemplateParam {

    KEY_NAME("keyName", String.class),
    REGION("region", Region.class),
    AMI_ID("amiId", String.class),
    INSTANCE_TYPE("instanceType", InstanceType.class);

    private final String paramName;
    private final Class clazz;

    private RequiredAwsTemplateParam(String paramName, Class clazz) {
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

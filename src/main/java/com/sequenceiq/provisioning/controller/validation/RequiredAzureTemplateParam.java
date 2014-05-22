package com.sequenceiq.provisioning.controller.validation;

import com.sequenceiq.provisioning.service.azure.AzureLocation;
import com.sequenceiq.provisioning.service.azure.AzureVmType;

public enum RequiredAzureTemplateParam implements TemplateParam {

    NAME("name", String.class),
    LOCATION("location", AzureLocation.class),
    DESCRIPTION("description", String.class),
    ADDRESSPREFIX("addressPrefix", String.class),
    DEPLOYMENTSLOT("deploymentSlot", String.class),
    IMAGENAME("imageName", String.class),
    USERNAME("username", String.class),
    PASSWORD("password", String.class),
    DISABLESSHPASSWORDAUTHENTICATION("disableSshPasswordAuthentication", String.class),
    VMTYPE("vmType", AzureVmType.class);

    private final String paramName;
    private final Class clazz;

    private RequiredAzureTemplateParam(String paramName, Class clazz) {
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

    // TODO: add other required params

}

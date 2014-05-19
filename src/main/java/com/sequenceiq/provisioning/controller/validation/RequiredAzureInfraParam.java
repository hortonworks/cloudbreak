package com.sequenceiq.provisioning.controller.validation;

public enum RequiredAzureInfraParam {

    NAME("name"),
    LOCATION("location"),
    DESCRIPTION("description"),
    ADDRESSPREFIX("addressPrefix"),
    DEPLOYMENTSLOT("deploymentSlot"),
    IMAGENAME("imageName"),
    USERNAME("username"),
    PASSWORD("password"),
    DISABLESSHPASSWORDAUTHENTICATION("disableSshPasswordAuthentication"),
    VMTYPE("vmType");

    private final String paramName;

    private RequiredAzureInfraParam(String paramName) {
        this.paramName = paramName;
    }

    public String getName() {
        return paramName;
    }

    // TODO: add other required params

}

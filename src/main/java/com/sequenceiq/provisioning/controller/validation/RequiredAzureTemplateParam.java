package com.sequenceiq.provisioning.controller.validation;

import com.sequenceiq.provisioning.service.azure.AzureLocation;
import com.sequenceiq.provisioning.service.azure.AzureVmType;

public enum RequiredAzureTemplateParam implements TemplateParam {

    LOCATION("location", true, AzureLocation.class),
    DESCRIPTION("description", true, String.class),
    ADDRESSPREFIX("addressPrefix", true, String.class),
    SUBNETADDRESSPREFIX("subnetAddressPrefix", true, String.class),
    DEPLOYMENTSLOT("deploymentSlot", true, String.class),
    IMAGENAME("imageName", true, String.class),
    USERNAME("username", true, String.class),
    VMTYPE("vmType", true, AzureVmType.class),

    SSH_PUBLIC_KEY_FINGERPRINT("sshPublicKeyFingerprint", false, String.class),
    PASSWORD("password", false, String.class),
    SSH_PUBLIC_KEY_PATH("sshPublicKeyPath", false, String.class);

    private final String paramName;
    private final Class clazz;
    private final boolean required;

    private RequiredAzureTemplateParam(String paramName, Boolean required, Class clazz) {
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

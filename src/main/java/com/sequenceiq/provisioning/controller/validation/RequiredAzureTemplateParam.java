package com.sequenceiq.provisioning.controller.validation;

import com.google.common.base.Optional;
import com.sequenceiq.provisioning.service.azure.AzureLocation;
import com.sequenceiq.provisioning.service.azure.AzureVmType;

public enum RequiredAzureTemplateParam implements TemplateParam {

    LOCATION("location", true, AzureLocation.class, Optional.<String>absent()),
    DESCRIPTION("description", true, String.class, Optional.<String>absent()),
    ADDRESSPREFIX("addressPrefix", true, String.class, Optional.<String>absent()),
    SUBNETADDRESSPREFIX("subnetAddressPrefix", true, String.class, Optional.<String>absent()),
    DEPLOYMENTSLOT("deploymentSlot", true, String.class, Optional.<String>absent()),
    IMAGENAME("imageName", true, String.class, Optional.<String>absent()),
    USERNAME("username", true, String.class, Optional.<String>absent()),
    VMTYPE("vmType", true, AzureVmType.class, Optional.<String>absent()),

    SSH_PUBLIC_KEY_FINGERPRINT("sshPublicKeyFingerprint", false, String.class, Optional.<String>absent()),
    PASSWORD("password", false, String.class, Optional.<String>absent()),
    SSH_PUBLIC_KEY_PATH("sshPublicKeyPath", false, String.class, Optional.<String>absent());

    private final String paramName;
    private final Class clazz;
    private final boolean required;
    private final Optional<String> regex;

    private RequiredAzureTemplateParam(String paramName, Boolean required, Class clazz, Optional<String> regex) {
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

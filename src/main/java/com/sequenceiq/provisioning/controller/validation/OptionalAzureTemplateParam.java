package com.sequenceiq.provisioning.controller.validation;

public enum OptionalAzureTemplateParam implements TemplateParam {

    SSH_PUBLIC_KEY_FINGERPRINT("sshPublicKeyFingerprint", String.class),
    PASSWORD("password", String.class),
    SSH_PUBLIC_KEY_PATH("sshPublicKeyPath", String.class);

    private final String paramName;
    private final Class clazz;

    private OptionalAzureTemplateParam(String paramName, Class clazz) {
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
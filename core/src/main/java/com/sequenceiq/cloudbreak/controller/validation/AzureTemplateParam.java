package com.sequenceiq.cloudbreak.controller.validation;

import com.google.common.base.Optional;

public enum AzureTemplateParam implements TemplateParam {

    VMTYPE("instanceType", true, String.class,
            Optional.of("^(?:Standard_(?:A[56789]|A10|A11|G[12345]|D[1234]|D11|D12|D13|D14|D1_v2|D2_v2|D3_v2|D4_v2|D5_v2|D11_v2|D12_v2|D13_v2|D14_v2))$"));

    private final String paramName;
    private final Class clazz;
    private final boolean required;
    private final Optional<String> regex;

    private AzureTemplateParam(String paramName, Boolean required, Class clazz, Optional<String> regex) {
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

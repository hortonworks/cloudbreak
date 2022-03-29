package com.sequenceiq.common.api.type;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

public enum LoadBalancerSku {
    BASIC("Basic"),
    STANDARD("Standard"),
    NONE("None");

    private final String templateName;

    LoadBalancerSku(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateName() {
        return templateName;
    }

    public static LoadBalancerSku getDefault() {
        return STANDARD;
    }

    public static LoadBalancerSku getValueOrDefault(LoadBalancerSku sku) {
        return sku == null ? getDefault() : sku;
    }

    public static LoadBalancerSku getValueOrDefault(String name) {
        return StringUtils.isEmpty(name) || !EnumUtils.isValidEnum(LoadBalancerSku.class, name) ?
                getDefault() : valueOf(name);
    }
}

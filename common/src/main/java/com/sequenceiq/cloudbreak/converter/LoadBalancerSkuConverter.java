package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.common.api.type.LoadBalancerSku;

public class LoadBalancerSkuConverter extends DefaultEnumConverter<LoadBalancerSku> {

    @Override
    public LoadBalancerSku getDefault() {
        return LoadBalancerSku.getDefault();
    }
}

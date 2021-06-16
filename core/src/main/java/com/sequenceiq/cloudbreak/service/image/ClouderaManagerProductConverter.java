package com.sequenceiq.cloudbreak.service.image;

import static com.sequenceiq.cloudbreak.common.type.ComponentType.CDH_PRODUCT_DETAILS;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

/**
 * Converts between ClouderaManagerProduct and Component, where Component can be persisted to Component table in cbdb
 */
@org.springframework.stereotype.Component
public class ClouderaManagerProductConverter {

    public Set<Component> clouderaManagerProductListToComponent(List<ClouderaManagerProduct> clouderaManagerProductList, Stack stack) {
        return clouderaManagerProductList.stream()
                .map(cmp -> clouderaManagerProductToComponent(cmp, stack))
                .collect(Collectors.toSet());
    }

    private Component clouderaManagerProductToComponent(ClouderaManagerProduct clouderaManagerProduct, Stack stack) {
        if ("CDH".equals(clouderaManagerProduct.getName())) {
            return new Component(CDH_PRODUCT_DETAILS, CDH_PRODUCT_DETAILS.name(), new Json(clouderaManagerProduct), stack);
        }
        return new Component(CDH_PRODUCT_DETAILS, clouderaManagerProduct.getName(), new Json(clouderaManagerProduct), stack);
    }

}

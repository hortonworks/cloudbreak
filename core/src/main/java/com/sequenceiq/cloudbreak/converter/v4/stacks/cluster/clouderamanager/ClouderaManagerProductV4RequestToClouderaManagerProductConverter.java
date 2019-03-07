package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.clouderamanager;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;

public final class ClouderaManagerProductV4RequestToClouderaManagerProductConverter {

    private ClouderaManagerProductV4RequestToClouderaManagerProductConverter() {
    }

    public static ClouderaManagerProduct convert(ClouderaManagerProductV4Request request) {
        return new ClouderaManagerProduct()
                .withName(request.getName())
                .withParcel(request.getParcel())
                .withVersion(request.getVersion());
    }
}
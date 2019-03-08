package com.sequenceiq.cloudbreak.converter.v4.stacks.cli.cm;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;

public final class ClouderaManagerProductToClouderaManagerProductV4Request {

    private ClouderaManagerProductToClouderaManagerProductV4Request() {
    }

    public static ClouderaManagerProductV4Request convert(ClouderaManagerProduct clouderaManagerProduct) {
        return new ClouderaManagerProductV4Request()
                .withName(clouderaManagerProduct.getName())
                .withParcel(clouderaManagerProduct.getParcel())
                .withVersion(clouderaManagerProduct.getVersion());
    }
}

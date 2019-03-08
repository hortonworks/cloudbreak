package com.sequenceiq.cloudbreak.converter.v4.stacks.cli.cm;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.clouderamanager.ClouderaManagerProductV4Response;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;

public final class ClouderaManagerProductToClouderaManagerProductV4Response {

    private ClouderaManagerProductToClouderaManagerProductV4Response() {
    }

    public static ClouderaManagerProductV4Response convert(ClouderaManagerProduct clouderaManagerProduct) {
        return new ClouderaManagerProductV4Response()
                .withName(clouderaManagerProduct.getName())
                .withParcel(clouderaManagerProduct.getParcel())
                .withVersion(clouderaManagerProduct.getVersion());
    }
}

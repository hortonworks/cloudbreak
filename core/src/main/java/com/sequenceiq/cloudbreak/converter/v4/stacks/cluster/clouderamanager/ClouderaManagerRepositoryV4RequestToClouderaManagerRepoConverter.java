package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.clouderamanager;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class ClouderaManagerRepositoryV4RequestToClouderaManagerRepoConverter
        extends AbstractConversionServiceAwareConverter<ClouderaManagerProductV4Request, ClouderaManagerProduct> {

    @Override
    public ClouderaManagerProduct convert(ClouderaManagerProductV4Request request) {
        return new ClouderaManagerProduct()
                .withName(request.getName())
                .withParcel(request.getParcel())
                .withVersion(request.getVersion());
    }
}
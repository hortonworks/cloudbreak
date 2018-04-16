package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.mpack.ClusterResponseMpackDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.ManagementPackComponent;

@Component
public class ManagementPackComponentToClusterResponseMpackDetails extends
        AbstractConversionServiceAwareConverter<ManagementPackComponent, ClusterResponseMpackDetails> {
    @Override
    public ClusterResponseMpackDetails convert(ManagementPackComponent source) {
        ClusterResponseMpackDetails mpack = new ClusterResponseMpackDetails();
        mpack.setName(source.getName());
        return mpack;
    }
}

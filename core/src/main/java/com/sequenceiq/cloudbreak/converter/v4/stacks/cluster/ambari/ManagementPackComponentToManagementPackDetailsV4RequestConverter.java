package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.ambari;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.stackrepository.mpack.ManagementPackDetailsV4Request;
import com.sequenceiq.cloudbreak.cloud.model.component.ManagementPackComponent;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class ManagementPackComponentToManagementPackDetailsV4RequestConverter extends
        AbstractConversionServiceAwareConverter<ManagementPackComponent, ManagementPackDetailsV4Request> {
    @Override
    public ManagementPackDetailsV4Request convert(ManagementPackComponent source) {
        ManagementPackDetailsV4Request mpack = new ManagementPackDetailsV4Request();
        mpack.setName(source.getName());
        return mpack;
    }
}

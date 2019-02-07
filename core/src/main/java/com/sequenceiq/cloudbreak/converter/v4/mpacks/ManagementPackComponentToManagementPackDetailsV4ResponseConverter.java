package com.sequenceiq.cloudbreak.converter.v4.mpacks;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ambari.stackrepository.mpack.ManagementPackDetailsV4Response;
import com.sequenceiq.cloudbreak.cloud.model.component.ManagementPackComponent;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class ManagementPackComponentToManagementPackDetailsV4ResponseConverter
        extends AbstractConversionServiceAwareConverter<ManagementPackComponent, ManagementPackDetailsV4Response> {

    @Override
    public ManagementPackDetailsV4Response convert(ManagementPackComponent source) {
        var response = new ManagementPackDetailsV4Response();
        response.setName(source.getName());
        response.setPreInstalled(source.isPreInstalled());
        return response;
    }

}

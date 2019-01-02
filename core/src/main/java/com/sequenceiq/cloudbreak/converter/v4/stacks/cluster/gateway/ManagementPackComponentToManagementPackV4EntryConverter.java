package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster.gateway;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ManagementPackV4Entry;
import com.sequenceiq.cloudbreak.cloud.model.component.ManagementPackComponent;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class ManagementPackComponentToManagementPackV4EntryConverter extends AbstractConversionServiceAwareConverter<ManagementPackComponent,
        ManagementPackV4Entry> {

    @Override
    public ManagementPackV4Entry convert(ManagementPackComponent source) {
        ManagementPackV4Entry managementPackV4Entry = new ManagementPackV4Entry();
        managementPackV4Entry.setMpackUrl(source.getMpackUrl());
        return managementPackV4Entry;
    }

}

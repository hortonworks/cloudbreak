package com.sequenceiq.cloudbreak.converter.stack;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.stack.cluster.HostGroupViewResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.view.HostGroupView;

@Component
public class HostGroupViewToHostGroupViewResponseConverter extends AbstractConversionServiceAwareConverter<HostGroupView, HostGroupViewResponse> {

    @Override
    public HostGroupViewResponse convert(HostGroupView source) {
        HostGroupViewResponse hostGroupViewResponse = new HostGroupViewResponse();
        hostGroupViewResponse.setId(source.getId());
        hostGroupViewResponse.setName(source.getName());
        return hostGroupViewResponse;
    }

}

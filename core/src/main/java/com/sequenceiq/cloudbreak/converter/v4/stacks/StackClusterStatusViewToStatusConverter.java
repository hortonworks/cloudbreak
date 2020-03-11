package com.sequenceiq.cloudbreak.converter.v4.stacks;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.projection.StackClusterStatusView;

@Component
public class StackClusterStatusViewToStatusConverter extends AbstractConversionServiceAwareConverter<StackClusterStatusView, StackStatusV4Response> {

    @Override
    public StackStatusV4Response convert(StackClusterStatusView source) {
        StackStatusV4Response response = new StackStatusV4Response();
        response.setId(source.getId());
        response.setCrn(source.getCrn());
        response.setStatus(source.getStatus());
        response.setStatusReason(source.getStatusReason());
        response.setClusterStatus(source.getClusterStatus());
        response.setClusterStatusReason(source.getClusterStatusReason());
        return response;
    }
}

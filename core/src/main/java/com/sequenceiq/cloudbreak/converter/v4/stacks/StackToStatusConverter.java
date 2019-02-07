package com.sequenceiq.cloudbreak.converter.v4.stacks;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@Component
public class StackToStatusConverter extends AbstractConversionServiceAwareConverter<Stack, StackStatusV4Response> {

    @Override
    public StackStatusV4Response convert(Stack source) {
        StackStatusV4Response response = new StackStatusV4Response();
        response.setId(source.getId());
        response.setStatus(source.getStatus());
        response.setStatusReason(source.getStatusReason());
        response.setClusterStatus(source.getCluster().getStatus());
        response.setClusterStatusReason(source.getCluster().getStatusReason());
        return response;
    }
}

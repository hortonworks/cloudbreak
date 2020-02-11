package com.sequenceiq.cloudbreak.converter.v4.stacks;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

@Component
public class StackToStatusConverter extends AbstractConversionServiceAwareConverter<Stack, StackStatusV4Response> {

    @Override
    public StackStatusV4Response convert(Stack source) {
        StackStatusV4Response response = new StackStatusV4Response();
        response.setId(source.getId());
        response.setStatus(source.getStatus());
        response.setStatusReason(source.getStatusReason());
        Optional<Cluster> cluster = Optional.ofNullable(source.getCluster());
        cluster.ifPresent(c -> response.setClusterStatus(c.getStatus()));
        cluster.ifPresent(c -> response.setClusterStatusReason(c.getStatusReason()));
        response.setCrn(source.getResourceCrn());
        return response;
    }
}

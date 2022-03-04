package com.sequenceiq.cloudbreak.converter.v4.stacks;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Responses;
import com.sequenceiq.cloudbreak.domain.projection.StackClusterStatusView;

@Component
public class StackClusterStatusViewToStatusConverter {

    public StackStatusV4Response convert(StackClusterStatusView source) {
        StackStatusV4Response response = new StackStatusV4Response();
        response.setId(source.getId());
        response.setCrn(source.getCrn());
        response.setStatus(source.getStatus());
        response.setStatusReason(source.getStatusReason());
        response.setClusterStatus(source.getClusterStatus());
        response.setClusterStatusReason(source.getClusterStatusReason());
        response.setCertExpirationState(source.getCertExpirationState());
        return response;
    }

    public StackStatusV4Responses convert(Iterable<StackClusterStatusView> sources) {
        Set<StackStatusV4Response> jsons = new HashSet<>();
        for (StackClusterStatusView source : sources) {
            jsons.add(convert(source));
        }
        return new StackStatusV4Responses(jsons);
    }
}

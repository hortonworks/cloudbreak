package com.sequenceiq.cloudbreak.service.decorator;

import java.util.Collection;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.decorator.responseprovider.ResponseProvider;
import com.sequenceiq.cloudbreak.service.decorator.responseprovider.ResponseProviders;
import com.sequenceiq.cloudbreak.service.stack.ShowTerminatedClusterConfigService;

@Service
public class StackResponseDecorator {
    @Inject
    private ResponseProviders responseProviders;

    @Inject
    private ShowTerminatedClusterConfigService showTerminatedClusterConfigService;

    public StackV4Response decorate(StackV4Response stackResponse, Stack stack, Collection<String> entries) {
        if (entries != null
                        && !entries.isEmpty()
                        && (showTerminatedClusterConfigService.get().isActive() || !Status.DELETE_COMPLETED.equals(stackResponse.getStatus()))) {
            for (String entry : entries) {
                ResponseProvider responseProvider = responseProviders.get(entry);
                stackResponse = (responseProvider == null) ? stackResponse : responseProvider.providerEntriesToStackResponse(stack, stackResponse);
            }
        }
        return stackResponse;
    }
}

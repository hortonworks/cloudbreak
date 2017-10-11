package com.sequenceiq.cloudbreak.service.decorator;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.decorator.responseprovider.ResponseProvider;
import com.sequenceiq.cloudbreak.service.decorator.responseprovider.ResponseProviders;

@Service
public class StackResponseDecorator {
    @Inject
    private ResponseProviders responseProviders;

    public StackResponse decorate(StackResponse stackResponse, Stack stack, Set<String> entries) {
        if (entries != null && !entries.isEmpty()) {
            for (String entry : entries) {
                ResponseProvider responseProvider = responseProviders.get(entry);
                stackResponse = (responseProvider == null) ? stackResponse : responseProvider.providerEntriesToStackResponse(stack, stackResponse);
            }
        }
        return stackResponse;
    }
}

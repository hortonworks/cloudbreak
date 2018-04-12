package com.sequenceiq.cloudbreak.service.decorator;

import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.decorator.responseprovider.ResponseProvider;
import com.sequenceiq.cloudbreak.service.decorator.responseprovider.ResponseProviders;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Collection;

@Service
public class StackResponseDecorator {
    @Inject
    private ResponseProviders responseProviders;

    public StackResponse decorate(StackResponse stackResponse, Stack stack, Collection<String> entries) {
        if (entries != null && !entries.isEmpty()) {
            for (String entry : entries) {
                ResponseProvider responseProvider = responseProviders.get(entry);
                stackResponse = (responseProvider == null) ? stackResponse : responseProvider.providerEntriesToStackResponse(stack, stackResponse);
            }
        }
        return stackResponse;
    }
}

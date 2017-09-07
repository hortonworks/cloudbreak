package com.sequenceiq.cloudbreak.service.decorator;

import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.decorator.responseprovider.ResponseProvider;

@Service
public class StackResponseDecorator implements Decorator<StackResponse> {

    @Resource
    private Map<String, ResponseProvider> responseProviders;

    private enum DecorationData {
        ENTRY,
        STACK
    }

    @Override
    public StackResponse decorate(StackResponse stackResponse, Object... data) {
        Set<String> entries = (Set<String>) data[DecorationData.ENTRY.ordinal()];
        Stack stack = (Stack) data[DecorationData.STACK.ordinal()];
        if (entries != null && !entries.isEmpty()) {
            for (String entry : entries) {
                ResponseProvider responseProvider = responseProviders.get(entry);
                stackResponse = (responseProvider == null) ? stackResponse : responseProvider.providerEntriesToStackResponse(stack, stackResponse);
            }
        }
        return stackResponse;
    }
}

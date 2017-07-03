package com.sequenceiq.cloudbreak.service.decorator.responseprovider;

import java.util.List;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson;
import com.sequenceiq.cloudbreak.api.model.StackResponse;
import com.sequenceiq.cloudbreak.api.model.StackResponseEntries;
import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.CloudbreakEventRepository;

@Component
public class StackResponseEventProvider implements ResponseProvider {

    @Inject
    private CloudbreakEventRepository cloudbreakEventRepository;

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Override
    public StackResponse providerEntriesToStackResponse(Stack stack, StackResponse stackResponse) {
        List<CloudbreakEvent> cloudbreakEventsForStack = cloudbreakEventRepository.findCloudbreakEventsForStack(stack.getOwner(), stack.getId());
        List<CloudbreakEventsJson> convertedCloudbreakEventsForStack = (List<CloudbreakEventsJson>) conversionService
                .convert(cloudbreakEventsForStack,
                        TypeDescriptor.forObject(cloudbreakEventsForStack),
                        TypeDescriptor.collection(List.class, TypeDescriptor.valueOf(CloudbreakEventsJson.class)));
        stackResponse.setCloudbreakEvents(convertedCloudbreakEventsForStack);
        return stackResponse;
    }

    @Override
    public String type() {
        return StackResponseEntries.EVENTS.getEntryName();
    }
}

package com.sequenceiq.freeipa.converter.stack;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.list.ListFreeIpaResponse;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;

@Component
public class FreeIpaToListFreeIpaResponseConverter {

    @Inject
    private StackToAvailabilityStatusConverter stackToAvailabilityStatusConverter;

    public List<ListFreeIpaResponse> convertList(List<FreeIpa> freeIpaList) {
        return freeIpaList.stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    private ListFreeIpaResponse convert(FreeIpa freeIpa) {
        ListFreeIpaResponse response = new ListFreeIpaResponse();
        response.setDomain(freeIpa.getDomain());
        Stack stack = freeIpa.getStack();
        if (stack != null) {
            response.setName(stack.getName());
            response.setCrn(stack.getResourceCrn());
            response.setEnvironmentCrn(stack.getEnvironmentCrn());
            response.setStatus(stack.getStackStatus().getStatus());
            response.setStatusString(stack.getStackStatus().getStatusString());
            response.setAvailabilityStatus(stackToAvailabilityStatusConverter.convert(stack));
        }
        return response;
    }
}

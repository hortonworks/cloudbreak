package com.sequenceiq.freeipa.converter.stack;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.list.ListFreeIpaResponse;
import com.sequenceiq.freeipa.entity.Stack;

@Component
public class StackToListFreeIpaResponseConverter {

    public List<ListFreeIpaResponse> convertList(List<Stack> stackList) {
        return stackList.stream()
                        .map(this::convert)
                        .collect(Collectors.toList());
    }

    private ListFreeIpaResponse convert(Stack stack) {
        ListFreeIpaResponse response = new ListFreeIpaResponse();
        response.setName(stack.getName());
        response.setCrn(stack.getResourceCrn());
        response.setEnvironmentCrn(stack.getEnvironmentCrn());
        response.setStatus(stack.getStackStatus().getStatus());
        return response;
    }
}

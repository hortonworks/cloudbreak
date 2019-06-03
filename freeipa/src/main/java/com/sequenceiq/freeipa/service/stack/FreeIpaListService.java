package com.sequenceiq.freeipa.service.stack;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.list.ListFreeIpaResponse;
import com.sequenceiq.freeipa.converter.stack.StackToListFreeIpaResponseConverter;
import com.sequenceiq.freeipa.entity.Stack;

@Service
public class FreeIpaListService {

    @Inject
    private StackService stackService;

    @Inject
    private StackToListFreeIpaResponseConverter stackToListFreeIpaResponseConverter;

    public List<ListFreeIpaResponse> list(String accountId) {
        List<Stack> stackList = stackService.getAllByAccountId(accountId);
        return stackToListFreeIpaResponseConverter.convertList(stackList);
    }
}

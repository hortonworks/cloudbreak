package com.sequenceiq.freeipa.service.stack;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.service.list.AuthorizationResource;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.list.ListFreeIpaResponse;
import com.sequenceiq.freeipa.converter.stack.FreeIpaToListFreeIpaResponseConverter;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;

@Service
public class FreeIpaListService {

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private FreeIpaToListFreeIpaResponseConverter freeIpaToListFreeIpaResponseConverter;

    public List<ListFreeIpaResponse> list(String accountId) {
        List<FreeIpa> stackList = freeIpaService.getAllByAccountId(accountId);
        return freeIpaToListFreeIpaResponseConverter.convertList(stackList);
    }

    public List<AuthorizationResource> listAsAuthorizationResources(String accountId) {
        return freeIpaService.getAllAsAuthorizationResources(accountId);
    }

    public List<ListFreeIpaResponse> listAllByIds(List<Long> ids) {
        List<FreeIpa> stackList = freeIpaService.getAllByIds(ids);
        return freeIpaToListFreeIpaResponseConverter.convertList(stackList);
    }
}

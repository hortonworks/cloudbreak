package com.sequenceiq.freeipa.authorization;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.list.AbstractAuthorizationFiltering;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.list.ListFreeIpaResponse;
import com.sequenceiq.freeipa.service.stack.FreeIpaListService;
import com.sequenceiq.freeipa.util.CrnService;

@Component
public class FreeIpaFiltering extends AbstractAuthorizationFiltering<List<ListFreeIpaResponse>> {

    @Inject
    private CrnService crnService;

    @Inject
    private FreeIpaListService freeIpaListService;

    public List<ListFreeIpaResponse> filterFreeIpas(AuthorizationResourceAction action) {
        return filterResources(Crn.safeFromString(ThreadBasedUserCrnProvider.getUserCrn()), action, Map.of());
    }

    @Override
    public List<ResourceWithId> getAllResources(Map<String, Object> args) {
        String accountId = crnService.getCurrentAccountId();
        return freeIpaListService.listAsAuthorizationResources(accountId);
    }

    @Override
    public List<ListFreeIpaResponse> filterByIds(List<Long> authorizedResourceIds, Map<String, Object> args) {
        return freeIpaListService.listAllByIds(authorizedResourceIds);
    }

    @Override
    public List<ListFreeIpaResponse> getAll(Map<String, Object> args) {
        String accountId = crnService.getCurrentAccountId();
        return freeIpaListService.list(accountId);
    }
}

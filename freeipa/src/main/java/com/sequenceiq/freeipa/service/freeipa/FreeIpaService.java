package com.sequenceiq.freeipa.service.freeipa;

import java.util.List;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.flow.core.ResourceIdProvider;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.FreeIpaServerRequest;
import com.sequenceiq.freeipa.converter.freeipa.FreeIpaServerRequestToFreeIpaConverter;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.projection.FreeIpaListView;
import com.sequenceiq.freeipa.repository.FreeIpaRepository;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.util.CrnService;

@Service
public class FreeIpaService implements ResourceIdProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaService.class);

    @Inject
    private StackService stackService;

    @Inject
    private CrnService crnService;

    @Inject
    private FreeIpaRepository repository;

    @Inject
    private FreeIpaServerRequestToFreeIpaConverter freeIpaConverter;

    public FreeIpa save(FreeIpa freeIpa) {
        return repository.save(freeIpa);
    }

    public FreeIpa findByStack(Stack stack) {
        return repository.getByStack(stack).orElseThrow(() -> new NotFoundException(String.format("FreeIpa not found for Stack [%s]", stack.getId())));
    }

    public FreeIpa findByStackId(Long stackId) {
        return repository.getByStackId(stackId).orElseThrow(() -> new NotFoundException(String.format("FreeIpa not found for Stack [%s]", stackId)));
    }

    public FreeIpa create(Stack stack, FreeIpaServerRequest request, String osType) {
        FreeIpa freeIpa = freeIpaConverter.convert(request, osType);
        freeIpa.setStack(stack);
        return save(freeIpa);
    }

    public List<FreeIpaListView> getAllViewByAccountId(String accountId) {
        return repository.findViewByAccountId(accountId);
    }

    @Override
    public Long getResourceIdByResourceCrn(String resourceCrn) {
        Long resourceId;
        if (Crn.fromString(resourceCrn).getResourceType() == Crn.ResourceType.ENVIRONMENT) {
            resourceId = stackService.getByEnvironmentCrnAndAccountId(resourceCrn, crnService.getCurrentAccountId()).getId();
            LOGGER.warn("Fetched FreeIpa resource successfully by using Environment CRN while it is expected to use FreeIpa CRN for getResourceIdByResourceCrn");
        } else {
            resourceId = stackService.getResourceBasicViewByCrn(resourceCrn).getId();
        }
        return resourceId;
    }

    @Override
    public String getResourceCrnByResourceId(Long resourceId) {
        return findByStackId(resourceId).getStack().getResourceCrn();
    }

    public List<FreeIpaListView> getAllViewByIds(List<Long> ids) {
        return repository.findAllViewByIds(ids);
    }

    public List<ResourceWithId> getAllAsAuthorizationResources(String accountId) {
        return repository.findAllAsAuthorizationResources(accountId);
    }
}

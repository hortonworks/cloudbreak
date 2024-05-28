package com.sequenceiq.cloudbreak.service.sharedservice;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.service.HierarchyAuthResourcePropertyProvider;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
public class PaasDatalakeAuthorizationService implements HierarchyAuthResourcePropertyProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaasDatalakeAuthorizationService.class);

    @Inject
    private StackDtoService stackDtoService;

    @Override
    public AuthorizationResourceType getSupportedAuthorizationResourceType() {
        return AuthorizationResourceType.DATALAKE;
    }

    @Override
    public String getResourceCrnByResourceName(String resourceName) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return getNotTerminatedDatalakeStackViewSafely(() -> stackDtoService.findNotTerminatedByNameAndAccountId(resourceName, accountId),
                "%s stack not found", "%s stack is not a Data Lake.", resourceName)
                .getResourceCrn();
    }

    @Override
    public List<String> getResourceCrnListByResourceNameList(List<String> resourceNames) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return stackDtoService.findNotTerminatedByNamesAndAccountId(resourceNames, accountId)
                .stream()
                .filter(stackView -> StackType.DATALAKE.equals(stackView.getType()))
                .map(StackView::getResourceCrn)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<String> getEnvironmentCrnByResourceCrn(String resourceCrn) {
        try {
            return Optional.of(getNotTerminatedDatalakeStackViewSafely(() -> stackDtoService.findNotTerminatedByCrn(resourceCrn),
                    "Stack by CRN %s not found", "Stack with CRN %s is not a Data Lake.", resourceCrn)
                    .getEnvironmentCrn());
        } catch (NotFoundException e) {
            LOGGER.error(String.format("Getting environment crn by resource crn %s failed, ", resourceCrn), e);
            return Optional.empty();
        }
    }

    @Override
    public Map<String, Optional<String>> getEnvironmentCrnsByResourceCrns(Collection<String> resourceCrns) {
        return stackDtoService.findNotTerminatedByCrns(resourceCrns)
                .stream()
                .filter(stackView -> StackType.DATALAKE.equals(stackView.getType()))
                .collect(Collectors.toMap(StackView::getResourceCrn, stackView -> Optional.ofNullable(stackView.getEnvironmentCrn())));
    }

    private StackView getNotTerminatedDatalakeStackViewSafely(Supplier<Optional<? extends StackView>> optionalStackView, String notFoundMessageTemplate,
            String notDatalakeMessageTemplate, String input) {
        StackView stackView = optionalStackView.get().orElseThrow(() -> new NotFoundException(String.format(notFoundMessageTemplate, input)));
        if (!StackType.DATALAKE.equals(stackView.getType())) {
            throw new BadRequestException(String.format(notDatalakeMessageTemplate, input));
        }
        return stackView;
    }

    @Override
    public EnumSet<Crn.ResourceType> getSupportedCrnResourceTypes() {
        return EnumSet.of(Crn.ResourceType.DATALAKE);
    }
}

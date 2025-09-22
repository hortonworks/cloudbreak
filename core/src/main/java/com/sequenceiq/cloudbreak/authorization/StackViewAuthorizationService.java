package com.sequenceiq.cloudbreak.authorization;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_DATAHUB;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_DATALAKE;
import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.CommonPermissionCheckingUtils;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.stack.StackViewService;

@Service
public class StackViewAuthorizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackViewAuthorizationService.class);

    @Inject
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Inject
    private StackViewService stackViewService;

    public void checkReadPermissionForStackView(StackView stackView) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        List<String> checkedCrns = List.of(stackView.getResourceCrn(), stackView.getEnvironmentCrn());
        AuthorizationResourceAction action = switch (stackView.getType()) {
            case WORKLOAD -> DESCRIBE_DATAHUB;
            case DATALAKE -> DESCRIBE_DATALAKE;
            default -> null;
        };
        if (action != null) {
            Map<String, Boolean> resultMap = commonPermissionCheckingUtils.getPermissionsForUserOnResources(action, userCrn, checkedCrns);
            if (!resultMap.isEmpty() && resultMap.values().stream().noneMatch(Boolean::booleanValue)) {
                commonPermissionCheckingUtils.throwAccessDeniedIfActionNotAllowed(action, checkedCrns);
            }
        }
    }

    public void checkReadPermissionForStackCrn(String crn, Long workspaceId) {
        Optional<StackView> stack = Optional.ofNullable(stackViewService.findByCrn(crn, workspaceId)).orElseThrow(notFound("stack", crn));
        stack.ifPresent(this::checkReadPermissionForStackView);
    }

    public void checkReadPermissionForStackName(String name, Long workspaceId) {
        Optional<StackView> stack = Optional.ofNullable(stackViewService.findByName(name, workspaceId)).orElseThrow(notFound("stack", name));
        stack.ifPresent(this::checkReadPermissionForStackView);
    }
}

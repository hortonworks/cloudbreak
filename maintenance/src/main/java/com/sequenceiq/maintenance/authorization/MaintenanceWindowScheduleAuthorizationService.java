package com.sequenceiq.maintenance.authorization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;

import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.UmsRightProvider;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;

/**
 * Enforces authorization for maintenance window schedules and skip operations.
 * <p>
 * UMS mapping (see {@code maintenance-api/AUTH.md}, kept in sync with this class):
 * <ul>
 *   <li>TENANT read: power user <em>or</em> environment creator (account-level rights)</li>
 *   <li>TENANT write: power user (account-level right)</li>
 *   <li>ENVIRONMENT / DATAHUB / DATALAKE: one resource right on {@code scopeId}, chosen by access mode — describe for
 *       read, the corresponding write action for write</li>
 *   <li>FREEIPA: <em>any one of</em> several resource rights on {@code scopeId}, per mode</li>
 * </ul>
 * Callers must validate scope with {@link com.sequenceiq.maintenance.service.MaintenanceWindowScheduleService#validateScope}
 * (or {@link com.sequenceiq.maintenance.service.MaintenanceWindowScheduleScopeValidator}) before calling this class; this class assumes a
 * resource-scope {@code scopeId} is already a CRN belonging to the caller's account.
 */
@Service
public class MaintenanceWindowScheduleAuthorizationService {

    private static final List<AuthorizationResourceAction> TENANT_READ_ACCOUNT_ACTIONS = List.of(
            AuthorizationResourceAction.POWERUSER_ONLY,
            AuthorizationResourceAction.CREATE_ENVIRONMENT);

    private static final List<AuthorizationResourceAction> FREEIPA_READ_ACTIONS = List.of(
            AuthorizationResourceAction.REPAIR_FREEIPA,
            AuthorizationResourceAction.ADMIN_FREEIPA,
            AuthorizationResourceAction.DESCRIBE_ENVIRONMENT);

    private static final List<AuthorizationResourceAction> FREEIPA_WRITE_ACTIONS = List.of(
            AuthorizationResourceAction.ADMIN_FREEIPA,
            AuthorizationResourceAction.EDIT_ENVIRONMENT);

    private final GrpcUmsClient grpcUmsClient;

    private final UmsRightProvider umsRightProvider;

    @Inject
    public MaintenanceWindowScheduleAuthorizationService(GrpcUmsClient grpcUmsClient, UmsRightProvider umsRightProvider) {
        this.grpcUmsClient = grpcUmsClient;
        this.umsRightProvider = umsRightProvider;
    }

    public void authorize(MaintenanceScopeType scopeType, String scopeId, MaintenanceWindowScheduleAccessMode mode) {
        if (!canAccess(scopeType, scopeId, mode)) {
            throw new ForbiddenException(String.format(
                    "You have no right to %s maintenance window schedule for scope %s/%s",
                    mode.name().toLowerCase(Locale.ROOT), scopeType, scopeId));
        }
    }

    public boolean canAccess(MaintenanceScopeType scopeType, String scopeId, MaintenanceWindowScheduleAccessMode mode) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        if (scopeType == MaintenanceScopeType.TENANT) {
            return canAccessTenant(userCrn, mode);
        }
        return hasAnyResourceRight(userCrn, scopeId, resourceActions(scopeType, mode));
    }

    /**
     * Resolves read access for many scopes using one UMS call per distinct action rather than one per scope, so an
     * unscoped list does not fan out into N serial gRPC round trips.
     * <p>
     * TENANT scopes are decided by a single account-right lookup and are not batched.
     *
     * @return the subset of {@code scopes} the caller may read
     */
    public Set<MaintenanceScopeKey> filterReadable(Collection<MaintenanceScopeKey> scopes) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        Set<MaintenanceScopeKey> readable = new LinkedHashSet<>();
        Map<AuthorizationResourceAction, List<String>> scopeIdsByAction = new HashMap<>();
        for (MaintenanceScopeKey scope : scopes) {
            if (scope.scopeType() == MaintenanceScopeType.TENANT) {
                if (canAccessTenant(userCrn, MaintenanceWindowScheduleAccessMode.READ)) {
                    readable.add(scope);
                }
            } else {
                for (AuthorizationResourceAction action : readActions(scope)) {
                    scopeIdsByAction.computeIfAbsent(action, ignored -> new ArrayList<>()).add(scope.scopeId());
                }
            }
        }
        Map<AuthorizationResourceAction, Map<String, Boolean>> resultsByAction = new HashMap<>();
        scopeIdsByAction.forEach((action, scopeIds) -> resultsByAction.put(action, hasRights(userCrn, scopeIds, action)));
        for (MaintenanceScopeKey scope : scopes) {
            if (scope.scopeType() != MaintenanceScopeType.TENANT && readActions(scope).stream()
                    .anyMatch(action -> resultsByAction.getOrDefault(action, Map.of())
                            .getOrDefault(scope.scopeId(), Boolean.FALSE))) {
                readable.add(scope);
            }
        }
        return readable;
    }

    private static List<AuthorizationResourceAction> readActions(MaintenanceScopeKey scope) {
        return resourceActions(scope.scopeType(), MaintenanceWindowScheduleAccessMode.READ);
    }

    private static List<AuthorizationResourceAction> resourceActions(
            MaintenanceScopeType scopeType, MaintenanceWindowScheduleAccessMode mode) {
        boolean read = mode == MaintenanceWindowScheduleAccessMode.READ;
        return switch (scopeType) {
            case ENVIRONMENT -> List.of(read
                    ? AuthorizationResourceAction.DESCRIBE_ENVIRONMENT
                    : AuthorizationResourceAction.EDIT_ENVIRONMENT);
            case DATAHUB -> List.of(read
                    ? AuthorizationResourceAction.DESCRIBE_DATAHUB
                    : AuthorizationResourceAction.UPGRADE_DATAHUB);
            case DATALAKE -> List.of(read
                    ? AuthorizationResourceAction.DESCRIBE_DATALAKE
                    : AuthorizationResourceAction.MODIFY_DATALAKE);
            case FREEIPA -> read ? FREEIPA_READ_ACTIONS : FREEIPA_WRITE_ACTIONS;
            case TENANT -> throw new IllegalArgumentException(
                    "TENANT scope is authorized by account right, not resource right");
        };
    }

    private boolean canAccessTenant(String userCrn, MaintenanceWindowScheduleAccessMode mode) {
        if (mode == MaintenanceWindowScheduleAccessMode.READ) {
            return hasAnyAccountRight(userCrn, TENANT_READ_ACCOUNT_ACTIONS);
        }
        return hasAccountRight(userCrn, AuthorizationResourceAction.POWERUSER_ONLY);
    }

    private boolean hasAnyResourceRight(String userCrn, String resourceCrn, List<AuthorizationResourceAction> actions) {
        return actions.stream().anyMatch(action -> hasResourceRight(userCrn, resourceCrn, action));
    }

    private boolean hasAnyAccountRight(String userCrn, List<AuthorizationResourceAction> actions) {
        return actions.stream().anyMatch(action -> hasAccountRight(userCrn, action));
    }

    private boolean hasAccountRight(String userCrn, AuthorizationResourceAction action) {
        return grpcUmsClient.checkAccountRight(userCrn, umsRightProvider.getRight(action));
    }

    private boolean hasResourceRight(String userCrn, String resourceCrn, AuthorizationResourceAction action) {
        return grpcUmsClient.checkResourceRight(userCrn, umsRightProvider.getRight(action), resourceCrn);
    }

    private Map<String, Boolean> hasRights(String userCrn, List<String> resourceCrns, AuthorizationResourceAction action) {
        return grpcUmsClient.hasRights(userCrn, resourceCrns, umsRightProvider.getRight(action));
    }
}

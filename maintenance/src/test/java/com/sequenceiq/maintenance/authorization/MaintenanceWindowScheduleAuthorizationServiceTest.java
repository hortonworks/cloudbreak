package com.sequenceiq.maintenance.authorization;

import static com.sequenceiq.maintenance.authorization.MaintenanceWindowScheduleAccessMode.READ;
import static com.sequenceiq.maintenance.authorization.MaintenanceWindowScheduleAccessMode.WRITE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.ws.rs.ForbiddenException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.UmsRightProvider;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;

@ExtendWith(MockitoExtension.class)
class MaintenanceWindowScheduleAuthorizationServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:acc-1:user:1";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:acc-1:environment:env-1";

    private static final String DATAHUB_CRN = "crn:cdp:datahub:us-west-1:acc-1:cluster:dh-1";

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:acc-1:datalake:dl-1";

    private static final String FREEIPA_CRN = "crn:cdp:freeipa:us-west-1:acc-1:freeipa:fi-1";

    private static final String POWERUSER = "cloudbreak/allowPowerUserOnly";

    private static final String CREATE_ENV = "environments/createEnvironment";

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @Mock
    private UmsRightProvider umsRightProvider;

    @InjectMocks
    private MaintenanceWindowScheduleAuthorizationService underTest;

    @Test
    void tenantReadAllowedForPowerUser() {
        right(AuthorizationResourceAction.POWERUSER_ONLY, POWERUSER);
        when(grpcUmsClient.checkAccountRight(USER_CRN, POWERUSER)).thenReturn(true);

        assertThat(asUser(() -> underTest.canAccess(MaintenanceScopeType.TENANT, "acc-1", READ))).isTrue();
    }

    @Test
    void tenantReadAllowedForEnvironmentCreatorWhenNotPowerUser() {
        right(AuthorizationResourceAction.POWERUSER_ONLY, POWERUSER);
        right(AuthorizationResourceAction.CREATE_ENVIRONMENT, CREATE_ENV);
        when(grpcUmsClient.checkAccountRight(USER_CRN, POWERUSER)).thenReturn(false);
        when(grpcUmsClient.checkAccountRight(USER_CRN, CREATE_ENV)).thenReturn(true);

        assertThat(asUser(() -> underTest.canAccess(MaintenanceScopeType.TENANT, "acc-1", READ))).isTrue();
    }

    @Test
    void tenantReadForbiddenWithoutAccountRights() {
        right(AuthorizationResourceAction.POWERUSER_ONLY, POWERUSER);
        right(AuthorizationResourceAction.CREATE_ENVIRONMENT, CREATE_ENV);
        when(grpcUmsClient.checkAccountRight(eq(USER_CRN), anyString())).thenReturn(false);

        assertThat(asUser(() -> underTest.canAccess(MaintenanceScopeType.TENANT, "acc-1", READ))).isFalse();
    }

    @Test
    void tenantWriteRequiresPowerUser() {
        right(AuthorizationResourceAction.POWERUSER_ONLY, POWERUSER);
        when(grpcUmsClient.checkAccountRight(USER_CRN, POWERUSER)).thenReturn(true);

        assertThat(asUser(() -> underTest.canAccess(MaintenanceScopeType.TENANT, "acc-1", WRITE))).isTrue();
    }

    @Test
    void tenantWriteForbiddenForEnvironmentCreatorOnly() {
        right(AuthorizationResourceAction.POWERUSER_ONLY, POWERUSER);
        when(grpcUmsClient.checkAccountRight(USER_CRN, POWERUSER)).thenReturn(false);

        assertThat(asUser(() -> underTest.canAccess(MaintenanceScopeType.TENANT, "acc-1", WRITE))).isFalse();
    }

    @Test
    void environmentReadUsesDescribeEnvironmentAndAllowsWhenGranted() {
        resourceRight(AuthorizationResourceAction.DESCRIBE_ENVIRONMENT, "environments/describeEnvironment", ENV_CRN, true);

        assertThat(asUser(() -> underTest.canAccess(MaintenanceScopeType.ENVIRONMENT, ENV_CRN, READ))).isTrue();
        verify(grpcUmsClient).checkResourceRight(USER_CRN, "environments/describeEnvironment", ENV_CRN);
    }

    @Test
    void environmentWriteUsesEditEnvironmentAndDeniesWhenMissing() {
        resourceRight(AuthorizationResourceAction.EDIT_ENVIRONMENT, "environments/editEnvironment", ENV_CRN, false);

        assertThat(asUser(() -> underTest.canAccess(MaintenanceScopeType.ENVIRONMENT, ENV_CRN, WRITE))).isFalse();
    }

    @Test
    void datahubReadForbiddenWhenDescribeMissing() {
        resourceRight(AuthorizationResourceAction.DESCRIBE_DATAHUB, "datahub/describeDatahub", DATAHUB_CRN, false);

        assertThat(asUser(() -> underTest.canAccess(MaintenanceScopeType.DATAHUB, DATAHUB_CRN, READ))).isFalse();
    }

    @Test
    void datahubWriteUsesDatahubWriteRight() {
        resourceRight(AuthorizationResourceAction.UPGRADE_DATAHUB, "datahub/write", DATAHUB_CRN, true);

        assertThat(asUser(() -> underTest.canAccess(MaintenanceScopeType.DATAHUB, DATAHUB_CRN, WRITE))).isTrue();
        verify(grpcUmsClient).checkResourceRight(USER_CRN, "datahub/write", DATAHUB_CRN);
    }

    @Test
    void datalakeReadUsesDescribeDatalake() {
        resourceRight(AuthorizationResourceAction.DESCRIBE_DATALAKE, "datalake/describeDatalake", DATALAKE_CRN, true);

        assertThat(asUser(() -> underTest.canAccess(MaintenanceScopeType.DATALAKE, DATALAKE_CRN, READ))).isTrue();
    }

    @Test
    void datalakeWriteUsesModifyDatalake() {
        resourceRight(AuthorizationResourceAction.MODIFY_DATALAKE, "datalake/modifyDatalake", DATALAKE_CRN, true);

        assertThat(asUser(() -> underTest.canAccess(MaintenanceScopeType.DATALAKE, DATALAKE_CRN, WRITE))).isTrue();
        verify(grpcUmsClient).checkResourceRight(USER_CRN, "datalake/modifyDatalake", DATALAKE_CRN);
    }

    @Test
    void freeipaReadFallsThroughToLaterActionsWhenFirstMissing() {
        resourceRight(AuthorizationResourceAction.REPAIR_FREEIPA, "environments/repairFreeIPA", FREEIPA_CRN, false);
        resourceRight(AuthorizationResourceAction.ADMIN_FREEIPA, "environments/adminFreeIPA", FREEIPA_CRN, false);
        resourceRight(AuthorizationResourceAction.DESCRIBE_ENVIRONMENT, "environments/describeEnvironment", FREEIPA_CRN, true);

        assertThat(asUser(() -> underTest.canAccess(MaintenanceScopeType.FREEIPA, FREEIPA_CRN, READ))).isTrue();
    }

    @Test
    void freeipaReadDeniedWhenNoActionGranted() {
        resourceRight(AuthorizationResourceAction.REPAIR_FREEIPA, "environments/repairFreeIPA", FREEIPA_CRN, false);
        resourceRight(AuthorizationResourceAction.ADMIN_FREEIPA, "environments/adminFreeIPA", FREEIPA_CRN, false);
        resourceRight(AuthorizationResourceAction.DESCRIBE_ENVIRONMENT, "environments/describeEnvironment", FREEIPA_CRN, false);

        assertThat(asUser(() -> underTest.canAccess(MaintenanceScopeType.FREEIPA, FREEIPA_CRN, READ))).isFalse();
    }

    @Test
    void freeipaWriteShortCircuitsOnFirstGrantedAction() {
        resourceRight(AuthorizationResourceAction.ADMIN_FREEIPA, "environments/adminFreeIPA", FREEIPA_CRN, true);
        lenient().when(umsRightProvider.getRight(AuthorizationResourceAction.EDIT_ENVIRONMENT))
                .thenReturn("environments/editEnvironment");

        assertThat(asUser(() -> underTest.canAccess(MaintenanceScopeType.FREEIPA, FREEIPA_CRN, WRITE))).isTrue();
        verify(grpcUmsClient, never()).checkResourceRight(USER_CRN, "environments/editEnvironment", FREEIPA_CRN);
    }

    @Test
    void authorizeThrowsWhenAccessDenied() {
        right(AuthorizationResourceAction.POWERUSER_ONLY, POWERUSER);
        when(grpcUmsClient.checkAccountRight(USER_CRN, POWERUSER)).thenReturn(false);

        assertThatThrownBy(() -> asUser(() -> {
            underTest.authorize(MaintenanceScopeType.TENANT, "acc-1", WRITE);
            return null;
        })).isInstanceOf(ForbiddenException.class);
    }

    /**
     * The point of {@code filterReadable}: resource scopes resolve through the batching {@code hasRights} call, not one
     * {@code checkResourceRight} per schedule.
     */
    @Test
    void filterReadableBatchesResourceScopesIntoOneCallPerAction() {
        right(AuthorizationResourceAction.DESCRIBE_DATAHUB, "datahub/describeDatahub");
        String otherDatahub = "crn:cdp:datahub:us-west-1:acc-1:cluster:dh-2";
        when(grpcUmsClient.hasRights(USER_CRN, List.of(DATAHUB_CRN, otherDatahub), "datahub/describeDatahub"))
                .thenReturn(Map.of(DATAHUB_CRN, true, otherDatahub, false));

        Set<MaintenanceScopeKey> readable = asUser(() -> underTest.filterReadable(List.of(
                new MaintenanceScopeKey(MaintenanceScopeType.DATAHUB, DATAHUB_CRN),
                new MaintenanceScopeKey(MaintenanceScopeType.DATAHUB, otherDatahub))));

        assertThat(readable).containsExactly(new MaintenanceScopeKey(MaintenanceScopeType.DATAHUB, DATAHUB_CRN));
        verify(grpcUmsClient, never()).checkResourceRight(anyString(), anyString(), anyString());
    }

    @Test
    void filterReadableResolvesTenantScopeByAccountRight() {
        right(AuthorizationResourceAction.POWERUSER_ONLY, POWERUSER);
        when(grpcUmsClient.checkAccountRight(USER_CRN, POWERUSER)).thenReturn(true);

        Set<MaintenanceScopeKey> readable = asUser(() -> underTest.filterReadable(
                List.of(new MaintenanceScopeKey(MaintenanceScopeType.TENANT, "acc-1"))));

        assertThat(readable).containsExactly(new MaintenanceScopeKey(MaintenanceScopeType.TENANT, "acc-1"));
    }

    @Test
    void filterReadableReturnsEmptyForNoScopes() {
        assertThat(asUser(() -> underTest.filterReadable(List.of()))).isEmpty();
    }

    private void right(AuthorizationResourceAction action, String right) {
        when(umsRightProvider.getRight(action)).thenReturn(right);
    }

    private void resourceRight(AuthorizationResourceAction action, String right, String resourceCrn, boolean granted) {
        lenient().when(umsRightProvider.getRight(action)).thenReturn(right);
        lenient().when(grpcUmsClient.checkResourceRight(USER_CRN, right, resourceCrn)).thenReturn(granted);
    }

    private static <T> T asUser(Supplier<T> supplier) {
        return ThreadBasedUserCrnProvider.doAs(USER_CRN, supplier);
    }
}

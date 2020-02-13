package com.sequenceiq.freeipa.service.freeipa.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.cloudera.thunderhead.service.authorization.AuthorizationProto;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.authorization.resource.RightUtils;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.CrnParseException;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.security.InternalCrnBuilder;
import com.sequenceiq.freeipa.service.freeipa.user.model.EnvironmentAccessRights;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

class EnvironmentAccessCheckerTest {

    private static final String IAM_INTERNAL_ACTOR_CRN = new InternalCrnBuilder(Crn.Service.IAM).getInternalCrnForServiceAsString();

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String NOT_CRN = "not:a:crn:";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:"
            + ACCOUNT_ID + ":environment:" + UUID.randomUUID().toString();

    private static final String MEMBER_CRN = "crn:cdp:environments:us-west-1:"
            + ACCOUNT_ID + ":user:" + UUID.randomUUID().toString();

    private GrpcUmsClient grpcUmsClient = mock(GrpcUmsClient.class);

    @Test
    void testEnvironmentCrnNull() {
        assertThrows(NullPointerException.class, () -> createEnvironmentAccessChecker(null));
    }

    @Test
    void testEnvironmentCrnNotACrn() {
        assertThrows(CrnParseException.class, () -> createEnvironmentAccessChecker(NOT_CRN));
    }

    @Test
    void testEnvironmentAccessCheckerChecksRightRightChecks() {
        EnvironmentAccessChecker underTest = createEnvironmentAccessChecker(ENV_CRN);
        ArgumentCaptor<List<AuthorizationProto.RightCheck>> argumentCaptor = ArgumentCaptor.forClass((Class) List.class);
        when(grpcUmsClient.hasRights(anyString(), eq(MEMBER_CRN), any(), any(Optional.class))).thenReturn(List.of(true, true));

        underTest.hasAccess(MEMBER_CRN, Optional.empty());

        verify(grpcUmsClient).hasRights(eq(IAM_INTERNAL_ACTOR_CRN), eq(MEMBER_CRN), argumentCaptor.capture(), any());
        List<AuthorizationProto.RightCheck> capturedRightChecks = argumentCaptor.getValue();
        assertEquals(2, capturedRightChecks.size());
        AuthorizationProto.RightCheck hasAccess = capturedRightChecks.get(0);
        assertEquals(RightUtils.getRight(AuthorizationResource.ENVIRONMENT, ResourceAction.ACCESS_ENVIRONMENT), hasAccess.getRight());
        assertEquals(ENV_CRN, hasAccess.getResource());
        AuthorizationProto.RightCheck isAdmin = capturedRightChecks.get(1);
        assertEquals(RightUtils.getRight(AuthorizationResource.ENVIRONMENT, ResourceAction.ADMIN_FREEIPA), isAdmin.getRight());
    }

    @Test
    void testEnvironmentAccessCheckerCreatesRightEnvironmentAccessRights() {
        EnvironmentAccessChecker underTest = createEnvironmentAccessChecker(ENV_CRN);

        for (boolean hasAccess : new boolean[] { false, true}) {
            for (boolean ipaAdmin : new boolean[] { false, true}) {
                when(grpcUmsClient.hasRights(anyString(), eq(MEMBER_CRN), any(), any(Optional.class))).thenReturn(List.of(hasAccess, ipaAdmin));

                EnvironmentAccessRights environmentAccessRights = underTest.hasAccess(MEMBER_CRN, Optional.empty());

                assertEquals(hasAccess, environmentAccessRights.hasEnvironmentAccessRight());
                assertEquals(ipaAdmin, environmentAccessRights.hasAdminFreeIPARight());
            }
        }
    }

    @Test
    void testEnvironmentAccessCheckerNoAccessIfMemberNotFound() {
        EnvironmentAccessChecker underTest = createEnvironmentAccessChecker(ENV_CRN);

        Throwable ex = new StatusRuntimeException(Status.Code.NOT_FOUND.toStatus());
        when(grpcUmsClient.hasRights(anyString(), eq(MEMBER_CRN), any(), any(Optional.class))).thenThrow(ex);

        EnvironmentAccessRights environmentAccessRights = underTest.hasAccess(MEMBER_CRN, Optional.empty());

        assertFalse(environmentAccessRights.hasEnvironmentAccessRight());
        assertFalse(environmentAccessRights.hasAdminFreeIPARight());
    }

    private EnvironmentAccessChecker createEnvironmentAccessChecker(String environmentCrn) {
        return new EnvironmentAccessChecker(grpcUmsClient, environmentCrn);
    }
}
package com.sequenceiq.freeipa.service.freeipa.user.ums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.authorization.AuthorizationProto;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.crn.CrnParseException;
import com.sequenceiq.freeipa.service.freeipa.user.UserSyncConstants;
import com.sequenceiq.freeipa.service.freeipa.user.model.EnvironmentAccessRights;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

@ExtendWith(MockitoExtension.class)
class EnvironmentAccessCheckerTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String NOT_CRN = "not:a:crn:";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:"
            + ACCOUNT_ID + ":environment:" + UUID.randomUUID().toString();

    private static final String MEMBER_CRN = "crn:cdp:environments:us-west-1:"
            + ACCOUNT_ID + ":user:" + UUID.randomUUID().toString();

    @Mock
    private GrpcUmsClient grpcUmsClient;

    @Spy
    @SuppressFBWarnings
    private AuthorizationRightChecksFactory authorizationRightChecksFactory = new AuthorizationRightChecksFactory();

    @InjectMocks
    private EnvironmentAccessCheckerFactory environmentAccessCheckerFactory;

    @Test
    void testEnvironmentCrnNull() {
        assertThrows(NullPointerException.class, () ->
                environmentAccessCheckerFactory.create(null));
    }

    @Test
    void testEnvironmentCrnNotACrn() {
        assertThrows(CrnParseException.class, () ->
                environmentAccessCheckerFactory.create(NOT_CRN));
    }

    @Test
    void testEnvironmentAccessCheckerChecksRightRightChecks() {
        EnvironmentAccessChecker underTest = environmentAccessCheckerFactory.create(ENV_CRN);
        ArgumentCaptor<List<AuthorizationProto.RightCheck>> argumentCaptor = ArgumentCaptor.forClass((Class) List.class);
        when(grpcUmsClient.hasRightsNoCache(
                eq(MEMBER_CRN), anyList(), any())).thenReturn(List.of(true, true));

        underTest.hasAccess(MEMBER_CRN);

        verify(grpcUmsClient).hasRightsNoCache(eq(MEMBER_CRN), argumentCaptor.capture(), any());
        List<AuthorizationProto.RightCheck> capturedRightChecks = argumentCaptor.getValue();
        assertEquals(2, capturedRightChecks.size());
        AuthorizationProto.RightCheck hasAccess = capturedRightChecks.get(0);
        Assertions.assertEquals(UserSyncConstants.ACCESS_ENVIRONMENT, hasAccess.getRight());
        assertEquals(ENV_CRN, hasAccess.getResource());
        AuthorizationProto.RightCheck isAdmin = capturedRightChecks.get(1);
        assertEquals(UserSyncConstants.ADMIN_FREEIPA, isAdmin.getRight());
    }

    @Test
    void testEnvironmentAccessCheckerCreatesRightEnvironmentAccessRights() {
        EnvironmentAccessChecker underTest = environmentAccessCheckerFactory.create(ENV_CRN);

        for (boolean hasAccess : new boolean[] { false, true}) {
            for (boolean ipaAdmin : new boolean[] { false, true}) {
                when(grpcUmsClient.hasRightsNoCache(eq(MEMBER_CRN), anyList(), any()))
                        .thenReturn(List.of(hasAccess, ipaAdmin));

                EnvironmentAccessRights environmentAccessRights = underTest.hasAccess(MEMBER_CRN);

                assertEquals(hasAccess, environmentAccessRights.hasEnvironmentAccessRight());
                assertEquals(ipaAdmin, environmentAccessRights.hasAdminFreeIpaRight());
            }
        }
    }

    @Test
    void testEnvironmentAccessCheckerNoAccessIfMemberNotFound() {
        EnvironmentAccessChecker underTest = environmentAccessCheckerFactory.create(ENV_CRN);

        Throwable ex = new StatusRuntimeException(Status.Code.NOT_FOUND.toStatus());
        when(grpcUmsClient.hasRightsNoCache(eq(MEMBER_CRN), anyList(), any()))
                .thenThrow(ex);

        EnvironmentAccessRights environmentAccessRights = underTest.hasAccess(MEMBER_CRN);

        assertFalse(environmentAccessRights.hasEnvironmentAccessRight());
        assertFalse(environmentAccessRights.hasAdminFreeIpaRight());
    }
}
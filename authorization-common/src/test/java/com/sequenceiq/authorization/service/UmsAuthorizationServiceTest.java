package com.sequenceiq.authorization.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import javax.ws.rs.BadRequestException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.access.AccessDeniedException;

import com.google.common.collect.Lists;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;

@RunWith(MockitoJUnitRunner.class)
public class UmsAuthorizationServiceTest {

    private static final String USER_ID = "userId";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:" + USER_ID;

    private static final String RESOURCE_CRN = "crn:cdp:datalake:us-west-1:1234:resource:" + USER_ID;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private GrpcUmsClient umsClient;

    @InjectMocks
    private UmsAuthorizationService underTest;

    @Test
    public void testCheckReadRight() {
        when(umsClient.checkRight(anyString(), anyString(), anyString(), any())).thenReturn(false);

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("You have no right to perform datalake/write. This requires one of these roles: PowerUser. "
                + "You can request access through IAM service from an administrator.");

        underTest.checkRightOfUser(USER_CRN, AuthorizationResourceType.DATALAKE, AuthorizationResourceAction.WRITE);
    }

    @Test
    public void testCheckWriteRight() {
        when(umsClient.checkRight(anyString(), anyString(), anyString(), any())).thenReturn(false);

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("You have no right to perform datalake/read. This requires one of these roles: PowerUser. "
                + "You can request access through IAM service from an administrator.");

        underTest.checkRightOfUser(USER_CRN, AuthorizationResourceType.DATALAKE, AuthorizationResourceAction.READ);
    }

    @Test
    public void testHasRightOfUserWithValidResourceTypeAndAction() {
        when(umsClient.checkRight(anyString(), anyString(), anyString(), any())).thenReturn(true);

        assertTrue(underTest.hasRightOfUser(USER_CRN, "datalake", "write"));

        when(umsClient.checkRight(anyString(), anyString(), anyString(), any())).thenReturn(false);

        assertFalse(underTest.hasRightOfUser(USER_CRN, "datalake", "write"));
    }

    @Test
    public void testCheckReadRightOnResource() {
        when(umsClient.checkRight(anyString(), anyString(), anyString(), anyString(), any())).thenReturn(false);

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("You have no right to perform datalake/write. This requires one of these roles: PowerUser. "
                + "You can request access through IAM service from an administrator.");

        underTest.checkRightOfUserOnResource(USER_CRN, AuthorizationResourceType.DATALAKE, AuthorizationResourceAction.WRITE, RESOURCE_CRN);
    }

    @Test
    public void testCheckWriteRightOnResource() {
        when(umsClient.checkRight(anyString(), anyString(), anyString(), anyString(), any())).thenReturn(false);

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("You have no right to perform datalake/read. This requires one of these roles: PowerUser. "
                + "You can request access through IAM service from an administrator.");

        underTest.checkRightOfUserOnResource(USER_CRN, AuthorizationResourceType.DATALAKE, AuthorizationResourceAction.READ, RESOURCE_CRN);
    }

    @Test
    public void testCheckRightOnResourcesFailure() {
        when(umsClient.hasRights(anyString(), anyString(), anyMap(), any())).thenReturn(Lists.newArrayList(Boolean.TRUE, Boolean.TRUE, Boolean.FALSE));

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("You have no right to perform datalake/write. This requires one of these roles: PowerUser. "
                + "You can request access through IAM service from an administrator.");

        underTest.checkRightOfUserOnResources(USER_CRN, AuthorizationResourceType.DATALAKE, AuthorizationResourceAction.WRITE, Lists.newArrayList(RESOURCE_CRN));
    }

    @Test
    public void testCheckRightOnResources() {
        when(umsClient.hasRights(anyString(), anyString(), anyMap(), any())).thenReturn(Lists.newArrayList(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE));

        underTest.checkRightOfUserOnResources(USER_CRN, AuthorizationResourceType.DATALAKE, AuthorizationResourceAction.WRITE, Lists.newArrayList(RESOURCE_CRN));
    }

    @Test
    public void testHasRightOfUserWithInvalidResourceType() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Resource or action cannot be found by request!");

        underTest.hasRightOfUser(USER_CRN, "invalid", "write");

        verifyZeroInteractions(umsClient);
    }

    @Test
    public void testHasRightOfUserWithInvalidAction() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Resource or action cannot be found by request!");

        underTest.hasRightOfUser(USER_CRN, "datalake", "invalid");

        verifyZeroInteractions(umsClient);
    }

}

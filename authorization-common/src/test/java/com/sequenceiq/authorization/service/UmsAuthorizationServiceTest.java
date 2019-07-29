package com.sequenceiq.authorization.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;

@RunWith(MockitoJUnitRunner.class)
public class UmsAuthorizationServiceTest {

    private static final String USER_ID = "userId";

    private static final String USER_CRN = "crn:altus:iam:us-west-1:1234:user:" + USER_ID;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private GrpcUmsClient umsClient;

    @InjectMocks
    private UmsAuthorizationService underTest;

    @Test
    public void testCheckRight() {
        when(umsClient.checkRight(anyString(), anyString(), anyString(), any())).thenReturn(false);

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("You have no right to perform datalake/write on DATALAKE. This requires PowerUser role. "
                + "You can request access through IAM service from an administrator.");

        underTest.checkRightOfUserForResource(USER_CRN, AuthorizationResource.DATALAKE, ResourceAction.WRITE);
    }

    @Test
    public void testHasRightOfUserForResourceWithValidResourceAndAction() {
        when(umsClient.checkRight(anyString(), anyString(), anyString(), any())).thenReturn(true);

        assertTrue(underTest.hasRightOfUserForResource(USER_CRN, "datalake", "write"));

        when(umsClient.checkRight(anyString(), anyString(), anyString(), any())).thenReturn(false);

        assertFalse(underTest.hasRightOfUserForResource(USER_CRN, "datalake", "write"));
    }

    @Test
    public void testHasRightOfUserForResourceWithInvalidResource() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Resource or action cannot be found by request!");

        underTest.hasRightOfUserForResource(USER_CRN, "invalid", "write");

        verifyZeroInteractions(umsClient);
    }

    @Test
    public void testHasRightOfUserForResourceWithInvalidAction() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Resource or action cannot be found by request!");

        underTest.hasRightOfUserForResource(USER_CRN, "datalake", "invalid");

        verifyZeroInteractions(umsClient);
    }

}

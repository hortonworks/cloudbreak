package com.sequenceiq.authorization.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import javax.ws.rs.BadRequestException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.AdditionalMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.access.AccessDeniedException;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;

@RunWith(MockitoJUnitRunner.class)
public class UmsAccountAuthorizationServiceTest {

    private static final String USER_ID = "userId";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:" + USER_ID;

    private static final String RESOURCE_CRN = "crn:cdp:datalake:us-west-1:1234:resource:1";

    private static final String RESOURCE_CRN2 = "crn:cdp:datalake:us-west-1:1234:resource:2";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private GrpcUmsClient umsClient;

    @Mock
    private UmsRightProvider umsRightProvider;

    @InjectMocks
    private UmsAccountAuthorizationService underTest;

    @Before
    public void init() {
        when(umsRightProvider.getRight(any())).thenReturn("datalake/read");
        when(umsRightProvider.getByName(eq("datalake/read"))).thenReturn(Optional.of(AuthorizationResourceAction.DATALAKE_READ));
        when(umsRightProvider.getByName(AdditionalMatchers.not(eq("datalake/read")))).thenReturn(Optional.empty());
    }

    @Test
    public void testCheckRight() {
        when(umsClient.checkRight(anyString(), anyString(), anyString(), any())).thenReturn(false);

        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("You have no right to perform datalake/read in account 1234");

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.
                checkRightOfUser(USER_CRN, AuthorizationResourceAction.DATALAKE_READ));
    }

    @Test
    public void testHasRightOfUserWithValidResourceTypeAndAction() {
        when(umsClient.checkRight(anyString(), anyString(), anyString(), any())).thenReturn(true);

        assertTrue(ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.hasRightOfUser(USER_CRN, "datalake/read")));

        when(umsClient.checkRight(anyString(), anyString(), anyString(), any())).thenReturn(false);

        assertFalse(ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.hasRightOfUser(USER_CRN, "datalake/read")));
    }

    @Test
    public void testHasRightOfUserWithInvalidAction() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Action cannot be found by request!");

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.hasRightOfUser(USER_CRN, "invalid"));

        verifyZeroInteractions(umsClient);
    }

    @Test
    public void testCheckCallerIsSelfOrHasRightSameActor() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.checkCallerIsSelfOrHasRight(USER_CRN, USER_CRN, AuthorizationResourceAction.GET_KEYTAB));
    }

    @Test
    public void testCheckCallerIsSelfOrHasRightDifferent() {
        String userInDifferentAccount = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();
        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage("Unauthorized to run this operation in a different account");

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.
                checkCallerIsSelfOrHasRight(USER_CRN, userInDifferentAccount, AuthorizationResourceAction.DATALAKE_READ));
    }

    @Test
    public void testActorAndTargetDifferentAndMissingRight() {
        String user2 = "crn:cdp:iam:us-west-1:1234:user:someOtherUserId";
        when(umsClient.checkRight(any(), any(), any(), any())).thenReturn(false);
        thrown.expect(AccessDeniedException.class);
        thrown.expectMessage(String.format("You have no right to perform datalake/read on user %s", user2));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.
                checkCallerIsSelfOrHasRight(USER_CRN, user2, AuthorizationResourceAction.DATALAKE_READ));
    }

    @Test
    public void testActorAndTargetDifferentHasRequiredRight() {
        String user2 = "crn:cdp:iam:us-west-1:1234:user:someOtherUserId";
        when(umsClient.checkRight(any(), any(), any(), any())).thenReturn(true);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.
                checkCallerIsSelfOrHasRight(USER_CRN, user2, AuthorizationResourceAction.DATALAKE_READ));
    }
}

package com.sequenceiq.authorization.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
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

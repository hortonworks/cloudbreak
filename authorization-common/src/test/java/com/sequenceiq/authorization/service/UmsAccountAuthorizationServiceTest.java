package com.sequenceiq.authorization.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;

@ExtendWith(MockitoExtension.class)
public class UmsAccountAuthorizationServiceTest {

    private static final String USER_ID = "userId";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:" + USER_ID;

    @Mock
    private GrpcUmsClient umsClient;

    @Mock
    private UmsRightProvider umsRightProvider;

    @InjectMocks
    private UmsAccountAuthorizationService underTest;

    @Test
    public void testCheckRight() {
        when(umsRightProvider.getRight(any())).thenReturn(AuthorizationResourceAction.DATALAKE_READ.getRight());

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> {
            ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.
                    checkRightOfUser(USER_CRN, AuthorizationResourceAction.DESCRIBE_DATALAKE));
        });

        assertEquals("You have no right to perform datalake/read in account 1234", exception.getMessage());
    }

    @Test
    public void testHasRightOfUserWithValidResourceTypeAndAction() {
        when(umsClient.checkAccountRight(anyString(), any(), any())).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.checkRightOfUser(USER_CRN, AuthorizationResourceAction.DESCRIBE_DATALAKE)));
    }

    @Test
    public void testCheckCallerIsSelfOrHasRightSameActor() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.checkCallerIsSelfOrHasRight(USER_CRN, USER_CRN, AuthorizationResourceAction.GET_KEYTAB));
    }

    @Test
    public void testCheckCallerIsSelfOrHasRightDifferent() {
        String userInDifferentAccount = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.
                checkCallerIsSelfOrHasRight(USER_CRN, userInDifferentAccount, AuthorizationResourceAction.DATALAKE_READ)));
        assertEquals("Unauthorized to run this operation in a different account", exception.getMessage());
    }

    @Test
    public void testActorAndTargetDifferentAndMissingRight() {
        String user2 = "crn:cdp:iam:us-west-1:1234:user:someOtherUserId";
        when(umsRightProvider.getRight(any())).thenReturn(AuthorizationResourceAction.DATALAKE_READ.getRight());
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.
                checkCallerIsSelfOrHasRight(USER_CRN, user2, AuthorizationResourceAction.DATALAKE_READ)));
        assertEquals("You have no right to perform datalake/read in account 1234", exception.getMessage());
    }

    @Test
    public void testActorAndTargetDifferentHasRequiredRight() {
        String user2 = "crn:cdp:iam:us-west-1:1234:user:someOtherUserId";

        when(umsClient.checkAccountRight(any(), any(), any())).thenReturn(true);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.
                checkCallerIsSelfOrHasRight(USER_CRN, user2, AuthorizationResourceAction.DATALAKE_READ));
    }
}

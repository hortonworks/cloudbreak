package com.sequenceiq.freeipa.controller;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.freeipa.controller.exception.BadRequestException;
import com.sequenceiq.freeipa.kerberosmgmt.v1.KerberosMgmtV1Controller;
import com.sequenceiq.freeipa.kerberosmgmt.v1.UserKeytabService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class KerberosMgmtV1ControllerTest {

    private static final String ACCOUNT_ID = UUID.randomUUID().toString();

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":user:" + UUID.randomUUID().toString();

    private static final String ENV_CRN = "crn:cdp:environment:us-west-1:" + ACCOUNT_ID + ":environment:" + UUID.randomUUID().toString();

    @InjectMocks
    private KerberosMgmtV1Controller underTest;

    @Mock
    private UserKeytabService userKeytabService;

    @Test
    void getKeytabSameCallerAndTarget() {
        String keytabBase64 = "keytabBase64...";
        when(userKeytabService.getKeytabBase64(any(), any())).thenReturn(keytabBase64);
        assertEquals(keytabBase64, ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getUserKeytab(ENV_CRN, USER_CRN)));
        verify(userKeytabService, times(1)).getKeytabBase64(USER_CRN, ENV_CRN);
    }

    @Test
    void getKeytabDifferentUser() {
        String keytabBase64 = "keytabBase64...";
        String targetUserCrn = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":user:" + UUID.randomUUID().toString();
        assertThrows(BadRequestException.class, () ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getUserKeytab(ENV_CRN, targetUserCrn)));
    }
}

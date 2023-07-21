package com.sequenceiq.freeipa.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@ExtendWith(MockitoExtension.class)
public class PreferredOsServiceTest {

    private static final String CENTOS7 = "centos7";

    private static final String REDHAT7 = "redhat7";

    private static final String REDHAT8 = "redhat8";

    private static final String ACCOUNT_ID = "cloudera";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":user:" + UUID.randomUUID();

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private PreferredOsService victim;

    @BeforeEach
    public void initTest() {
        ReflectionTestUtils.setField(victim, PreferredOsService.class, "defaultOs", CENTOS7, null);
    }

    @Test
    public void shouldPreferTheRequestedOsParameterRegardlessGrantedEntitlements() {
        String actual = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> victim.getPreferredOs(REDHAT7));

        assertEquals(REDHAT7, actual);
        verifyNoMoreInteractions(entitlementService);
    }

    @Test
    public void shouldPreferTheDefaultOsInCaseOfMissingRequestedOsAndNotGrantedRhel8Support() {
        when(entitlementService.isRhel8ImagePreferred(ACCOUNT_ID)).thenReturn(false);

        String actual = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> victim.getPreferredOs(null));
        assertEquals(CENTOS7, actual);
        verifyNoMoreInteractions(entitlementService);
    }

    @Test
    public void shouldPreferTheDefaultOsInCaseOfMissingRequestedOsAndNotGrantedRhel8Preference() {
        when(entitlementService.isRhel8ImagePreferred(ACCOUNT_ID)).thenReturn(false);

        String actual = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> victim.getPreferredOs(null));
        assertEquals(CENTOS7, actual);
        verifyNoMoreInteractions(entitlementService);
    }

    @Test
    public void shouldPreferRhel8InCaseOfMissingRequestedOsAndGrantedRhel8SupportAndPreference() {
        when(entitlementService.isRhel8ImagePreferred(ACCOUNT_ID)).thenReturn(true);

        String actual = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> victim.getPreferredOs(null));
        assertEquals(REDHAT8, actual);
    }
}
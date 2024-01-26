package com.sequenceiq.freeipa.service.image;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoMoreInteractions;

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
public class SupportedOsServiceTest {

    private static final String CENTOS7 = "centos7";

    private static final String REDHAT7 = "redhat7";

    private static final String REDHAT8 = "redhat8";

    private static final String ACCOUNT_ID = "cloudera";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":user:" + UUID.randomUUID();

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private SupportedOsService victim;

    @BeforeEach
    public void initTest() {
        ReflectionTestUtils.setField(victim, SupportedOsService.class, "defaultOs", CENTOS7, null);
    }

    @Test
    public void shouldSupportDefaultOsRegardlesEntitlements() {
        boolean actual = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> victim.isSupported(CENTOS7));

        assertTrue(actual);
        verifyNoMoreInteractions(entitlementService);
    }

    @Test
    public void shouldSupportNonDefaultAndNonRhel8OsRegardlesEntitlements() {
        boolean actual = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> victim.isSupported(REDHAT7));

        assertTrue(actual);
        verifyNoMoreInteractions(entitlementService);
    }

    @Test
    public void shouldSupportRhel8Os() {
        boolean actual = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> victim.isSupported(REDHAT8));
        assertTrue(actual);

        actual = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> victim.isRhel8Supported());
        assertTrue(actual);
    }

    @Test
    public void shouldNotSupportRandomOs() {
        boolean actual = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> victim.isSupported("ubuntu"));

        assertFalse(actual);
    }

}
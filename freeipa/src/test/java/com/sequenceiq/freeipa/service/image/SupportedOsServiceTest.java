package com.sequenceiq.freeipa.service.image;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@ExtendWith(MockitoExtension.class)
public class SupportedOsServiceTest {

    private static final String CENTOS7 = "centos7";

    private static final String REDHAT7 = "redhat7";

    private static final String REDHAT8 = "redhat8";

    private static final String ACCOUNT_ID = "cloudera";

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
        boolean actual = victim.isSupported(ACCOUNT_ID, CENTOS7);

        assertTrue(actual);
        verifyNoMoreInteractions(entitlementService);
    }

    @Test
    public void shouldSupportNonDefaultAndNonRhel8OsRegardlesEntitlements() {
        boolean actual = victim.isSupported(ACCOUNT_ID, REDHAT7);

        assertTrue(actual);
        verifyNoMoreInteractions(entitlementService);
    }

    @Test
    public void shouldSupportRhel8OsInCaseOfGrantedSupport() {
        when(entitlementService.isRhel8ImageSupportEnabled(ACCOUNT_ID)).thenReturn(true);

        boolean actual = victim.isSupported(ACCOUNT_ID, REDHAT8);

        assertTrue(actual);
    }

    @Test
    public void shouldNotSupportRhel8OsInCaseOfNonGrantedRhel8Support() {
        when(entitlementService.isRhel8ImageSupportEnabled(ACCOUNT_ID)).thenReturn(false);

        boolean actual = victim.isSupported(ACCOUNT_ID, REDHAT8);

        assertFalse(actual);
    }
}
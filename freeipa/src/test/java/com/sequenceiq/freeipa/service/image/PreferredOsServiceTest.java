package com.sequenceiq.freeipa.service.image;

import static com.sequenceiq.common.model.OsType.RHEL8;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
public class PreferredOsServiceTest {

    private static final String REDHAT7 = "redhat7";

    private static final String ACCOUNT_ID = "cloudera";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":user:" + UUID.randomUUID();

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private PreferredOsService victim;

    @BeforeEach
    public void initTest() {
        ReflectionTestUtils.setField(victim, PreferredOsService.class, "defaultOs", RHEL8.getOs(), null);
    }

    @Test
    public void shouldPreferTheRequestedOsParameterRegardlessGrantedEntitlements() {
        String actual = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> victim.getPreferredOs(REDHAT7));

        assertEquals(REDHAT7, actual);
        verifyNoMoreInteractions(entitlementService);
    }

    @Test
    public void getDefaultOs() {
        String actual = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> victim.getPreferredOs(null));

        assertEquals(RHEL8.getOs(), actual);
        verifyNoMoreInteractions(entitlementService);
    }
}
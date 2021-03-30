package com.sequenceiq.cloudbreak.service;


import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.CustomConfigProperty;
import com.sequenceiq.cloudbreak.domain.CustomConfigs;
import com.sequenceiq.cloudbreak.exception.ServiceTypeNotFoundException;

@ExtendWith(MockitoExtension.class)
class CustomConfigsValidatorTest {

    private static final String TEST_NAME = "test";

    private static final String TEST_CRN = "crn:cdp:resource:us-west-1:tenant:customconfigs:c7da2918-dd14-49ed-9b43-33ff55bd6309";

    private static final String TEST_ACCOUNT_ID = "accid";

    private static final String TEST_USER_CRN = "crn:cdp:iam:us-west-1:" + TEST_ACCOUNT_ID + ":user:username";

    private static final String TEST_VERSION = "7.2.10";

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private CustomConfigsValidator underTest;

    private CustomConfigs customConfigs = new CustomConfigs(
            TEST_NAME,
            TEST_CRN,
            Set.of(),
            TEST_VERSION,
            TEST_ACCOUNT_ID,
            System.currentTimeMillis()
    );

    @Test
    void testValidateServiceNames() {
        CustomConfigProperty invalidProperty = new CustomConfigProperty("property1",
                "value1",
                "role1",
                "invalidServiceName");
        customConfigs.setConfigs(Set.of(invalidProperty));
        assertThrows(ServiceTypeNotFoundException.class, () -> underTest.validateServiceNames(customConfigs));
    }

    @Test
    void testForEntitlementsOnCustomConfigs() {
        // when: false
        when(entitlementService.datahubCustomConfigsEnabled(anyString())).thenReturn(false);
        //then
        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> assertThrows(BadRequestException.class, () -> underTest.validateIfAccountIsEntitled()));
    }
}
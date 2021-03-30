package com.sequenceiq.cloudbreak.validation;


import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.CustomConfigurationProperty;
import com.sequenceiq.cloudbreak.domain.CustomConfigurations;
import com.sequenceiq.cloudbreak.exception.CustomConfigurationsServiceTypeNotFoundException;

@ExtendWith(MockitoExtension.class)
class CustomConfigurationsValidatorTest {

    private static final String TEST_NAME = "test";

    private static final String TEST_CRN = "crn:cdp:resource:us-west-1:tenant:customconfigs:c7da2918-dd14-49ed-9b43-33ff55bd6309";

    private static final String TEST_ACCOUNT_ID = "accid";

    private static final String TEST_VERSION = "7.2.10";

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private CustomConfigurationsValidator underTest;

    private CustomConfigurations customConfigurations = new CustomConfigurations(
            TEST_NAME,
            TEST_CRN,
            Set.of(),
            TEST_VERSION,
            TEST_ACCOUNT_ID,
            System.currentTimeMillis()
    );

    @Test
    void testValidateServiceNames() {
        CustomConfigurationProperty invalidProperty = new CustomConfigurationProperty("property1",
                "value1",
                "role1",
                "invalidServiceName");
        customConfigurations.setConfigurations(Set.of(invalidProperty));
        assertThrows(CustomConfigurationsServiceTypeNotFoundException.class, () -> underTest.validateServiceNames(customConfigurations));
    }

    @Test
    void testForEntitlementsOnCustomConfigs() {
        // when: false
        when(entitlementService.datahubCustomConfigsEnabled(anyString())).thenReturn(false);
        //then
        assertThrows(BadRequestException.class, () -> underTest.validateIfAccountIsEntitled(TEST_ACCOUNT_ID));
    }
}
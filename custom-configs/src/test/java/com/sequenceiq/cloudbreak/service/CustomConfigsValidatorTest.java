package com.sequenceiq.cloudbreak.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.sequenceiq.cloudbreak.domain.CustomConfigProperty;
import com.sequenceiq.cloudbreak.domain.CustomConfigs;
import com.sequenceiq.cloudbreak.exception.ServiceTypeNotFoundException;

class CustomConfigsValidatorTest {

    private CustomConfigsValidator underTest;

    private CustomConfigs customConfigs = new CustomConfigs(
            "test",
            "crn:cdp:resource:us-west-1:tenant:customconfigs:c7da2918-dd14-49ed-9b43-33ff55bd6309",
            Set.of(),
            "7.2.8",
            System.currentTimeMillis(),
            null
    );

    @BeforeEach
    void setUp() {
        underTest = new CustomConfigsValidator();
    }

    @Test
    void testValidateServiceNames() {
        CustomConfigProperty invalidProperty = new CustomConfigProperty("property1",
                "value1",
                "role1",
                "invalidServiceName");
        customConfigs.setConfigs(Set.of(invalidProperty));
        assertThrows(ServiceTypeNotFoundException.class, () -> underTest.validateServiceNames(customConfigs));
    }
}
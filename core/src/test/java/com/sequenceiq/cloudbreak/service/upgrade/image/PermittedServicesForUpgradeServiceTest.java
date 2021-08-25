package com.sequenceiq.cloudbreak.service.upgrade.image;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

public class PermittedServicesForUpgradeServiceTest {

    private final PermittedServicesForUpgradeService underTest = new PermittedServicesForUpgradeService();

    @ParameterizedTest(name = "{3}")
    @MethodSource("serviceAndVersionDataProvider")
    void test(String serviceName, String serviceVersion, boolean expectedAllowedResult, String testCaseDescription) {
        setUpPermittedServices(Set.of("Service1: 1.2.3 "));

        assertEquals(expectedAllowedResult, underTest.isAllowedForUpgrade(serviceName, serviceVersion));
    }

    static Object[][] serviceAndVersionDataProvider() {
        return new Object[][] {
                {"Service2", "100.100.100", false, "A service not listed is not accepted"},
                {"Service1", "1.2.2", false, "A service with a too low version is not allowed"},
                {"Service1", "1.2.3", true, "A service with the minimum required version is allowed"},
                {"Service1", "1.2.4", true, "A service above the minimum required version is allowed"}
        };
    }

    private void setUpPermittedServices(Set<String> servicesWithMinimumVersion) {
        ReflectionTestUtils.setField(underTest, "permittedServicesForUpgrade", servicesWithMinimumVersion);
        underTest.init();
    }

}

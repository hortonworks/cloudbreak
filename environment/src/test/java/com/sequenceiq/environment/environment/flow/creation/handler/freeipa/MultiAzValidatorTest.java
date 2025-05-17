package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

@ExtendWith(MockitoExtension.class)
public class MultiAzValidatorTest {

    @InjectMocks
    private MultiAzValidator underTest;

    static Object[][] supportedCloudPlatformForMultiAz() {
        return new Object[][]{
                {"AWS", true},
                {"AZURE", true},
                {"GCP", true}
        };
    }

    static Object[][] getMultiAzEntitlements() {
        return new Object[][]{
                {CloudPlatform.AWS, Set.of()},
                {CloudPlatform.AZURE, Set.of(Entitlement.CDP_CB_AZURE_MULTIAZ)},
                {CloudPlatform.GCP, Set.of(Entitlement.CDP_CB_GCP_MULTIAZ)}
        };
    }

    @BeforeEach
    void setUp() {
        underTest.init();
        ReflectionTestUtils.setField(underTest, "supportedMultiAzPlatforms", Set.of("AWS", "AZURE", "GCP"));
    }

    @ParameterizedTest(name = "testSupportedMultiAzForEnvironment{0}")
    @MethodSource("supportedCloudPlatformForMultiAz")
    public void testSupportedMultiAzForEnvironment(String cloudPlatform, boolean supportedMultiAz) {
        assertEquals(supportedMultiAz, underTest.suportedMultiAzForEnvironment(cloudPlatform));
    }

    @ParameterizedTest(name = "testGetMultiAzEntitlements{0}")
    @MethodSource("getMultiAzEntitlements")
    public void testGetMultiAzEntitlements(CloudPlatform cloudPlatform, Set<Entitlement> requiredEntitlements) {
        assertEquals(requiredEntitlements, underTest.getMultiAzEntitlements(cloudPlatform));
    }
}

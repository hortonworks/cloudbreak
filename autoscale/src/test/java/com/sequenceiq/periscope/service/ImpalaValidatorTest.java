package com.sequenceiq.periscope.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

@ExtendWith(MockitoExtension.class)
class ImpalaValidatorTest {

    @InjectMocks
    private ImpalaValidator impalaValidator;

    static Object[][] getImpalaScheduleBasedScalingEntitlements() {
        return new Object[][]{
                {CloudPlatform.AWS, Set.of(Entitlement.DATAHUB_AWS_AUTOSCALING, Entitlement.DATAHUB_AWS_IMPALA_SCHEDULE_BASED_SCALING)},
                {CloudPlatform.AZURE, Set.of(Entitlement.DATAHUB_AZURE_AUTOSCALING, Entitlement.DATAHUB_AZURE_IMPALA_SCHEDULE_BASED_SCALING)},
                {CloudPlatform.GCP, Set.of(Entitlement.DATAHUB_GCP_AUTOSCALING, Entitlement.DATAHUB_GCP_IMPALA_SCHEDULE_BASED_SCALING)}
        };
    }

    @ParameterizedTest(name = "testGetImpalaScheduleBasedScalingEntitlements{0}")
    @MethodSource("getImpalaScheduleBasedScalingEntitlements")
    public void testGetImpalaScheduleBasedScalingEntitlements(CloudPlatform cloudPlatform, Set<Entitlement> requiredEntitlements) {
        assertEquals(requiredEntitlements, impalaValidator.getImpalaScheduleScalingEntitlements(cloudPlatform));
    }

}
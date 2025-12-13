package com.sequenceiq.environment.environment.validation.validators;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGenerator;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.UsedSubnetWithResourceResponse;
import com.sequenceiq.common.api.UsedSubnetsByEnvironmentResponse;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.DatabaseServerV4Endpoint;

@ExtendWith(MockitoExtension.class)
public class SubnetUsageValidatorTest {

    private static final String ENVIRONMENT_CRN = "envcrn";

    private final StackV4Endpoint stackV4Endpoint = mock(StackV4Endpoint.class);

    private final FreeIpaV1Endpoint freeIpaV1Endpoint = mock(FreeIpaV1Endpoint.class);

    private final DatabaseServerV4Endpoint databaseServerV4Endpoint = mock(DatabaseServerV4Endpoint.class);

    private final SubnetUsageValidator underTest = new SubnetUsageValidator(stackV4Endpoint,
            freeIpaV1Endpoint,
            databaseServerV4Endpoint);

    @Mock
    private Environment environment;

    @Mock
    private RegionAwareInternalCrnGenerator regionAwareInternalCrnGenerator;

    @Test
    public void testValidateWhenNoSubnetMetasAndNoUsedSubnet() {
        NetworkDto network = NetworkDto.builder()
                .withSubnetMetas(new HashMap<>())
                .build();
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        when(environment.getResourceCrn()).thenReturn(ENVIRONMENT_CRN);
        when(stackV4Endpoint.getUsedSubnetsByEnvironment(0L, ENVIRONMENT_CRN)).thenReturn(new UsedSubnetsByEnvironmentResponse(emptyList()));
        when(freeIpaV1Endpoint.getUsedSubnetsByEnvironment(ENVIRONMENT_CRN)).thenReturn(new UsedSubnetsByEnvironmentResponse(emptyList()));
        when(databaseServerV4Endpoint.getUsedSubnetsByEnvironment(ENVIRONMENT_CRN)).thenReturn(new UsedSubnetsByEnvironmentResponse(emptyList()));
        underTest.validate(environment, network, resultBuilder);
        assertFalse(resultBuilder.build().hasError());
    }

    @Test
    public void testValidateWhenNoSubnetMetasAndHasUsedSubnet() {
        NetworkDto network = NetworkDto.builder()
                .withSubnetMetas(new HashMap<>())
                .build();
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        when(environment.getResourceCrn()).thenReturn(ENVIRONMENT_CRN);
        when(stackV4Endpoint.getUsedSubnetsByEnvironment(0L, ENVIRONMENT_CRN))
                .thenReturn(new UsedSubnetsByEnvironmentResponse(List.of(createUsedSubnetWithCrn("sub1", "WORKLOAD"))));
        when(freeIpaV1Endpoint.getUsedSubnetsByEnvironment(ENVIRONMENT_CRN))
                .thenReturn(new UsedSubnetsByEnvironmentResponse(List.of(createUsedSubnetWithCrn("sub2", "FREEIPA"))));
        when(databaseServerV4Endpoint.getUsedSubnetsByEnvironment(ENVIRONMENT_CRN))
                .thenReturn(new UsedSubnetsByEnvironmentResponse(List.of(createUsedSubnetWithCrn("sub3", "REDBEAMS"))));
        underTest.validate(environment, network, resultBuilder);
        ValidationResult validationResult = resultBuilder.build();
        assertTrue(validationResult.hasError());
        assertEquals("1. The Data Hub uses the subnets of [sub1], cannot remove these from the environment\n" +
                "2. The External Database uses the subnets of [sub3], cannot remove these from the environment\n" +
                "3. The FreeIPA uses the subnets of [sub2], cannot remove these from the environment", validationResult.getFormattedErrors());
    }

    private UsedSubnetWithResourceResponse createUsedSubnetWithCrn(String subnetId, String type) {
        return new UsedSubnetWithResourceResponse("name", subnetId, "crn", type);
    }
}

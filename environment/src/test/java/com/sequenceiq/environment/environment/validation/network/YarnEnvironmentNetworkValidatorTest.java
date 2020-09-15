package com.sequenceiq.environment.environment.validation.network;

import static org.junit.Assert.assertFalse;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.validation.ValidationResult.ValidationResultBuilder;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.validation.network.yarn.YarnEnvironmentNetworkValidator;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.dto.YarnParams;

public class YarnEnvironmentNetworkValidatorTest {

    private YarnEnvironmentNetworkValidator underTest;

    @BeforeEach
    void setUp() {
        underTest = new YarnEnvironmentNetworkValidator();
    }

    @Test
    void testValidateWhenNoNetworkCidrAndNoNetworkId() {
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(null, null, new YarnParams(), null, null, 1);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();

        underTest.validateDuringRequest(networkDto, resultBuilder);

        assertFalse(resultBuilder.build().hasError());
    }

    @Test
    void testValidateDuringFlowWhenNoQueueInYarnParams() {
        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(null, null, new YarnParams(), null, null, 1);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();

        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setNetwork(networkDto);

        underTest.validateDuringFlow(environmentDto, networkDto, resultBuilder);

        NetworkTestUtils.checkErrorsPresent(resultBuilder, List.of(
                "The 'Queue(queue)' parameter should be specified for the 'YARN' environment specific network!"
        ));
    }

    @Test
    void testValidateDuringFlowWhenLifetimeLessThenZeroInYarnParams() {
        YarnParams yarnParams = new YarnParams();
        yarnParams.setQueue("queue");
        yarnParams.setLifetime(-1);

        NetworkDto networkDto = NetworkTestUtils.getNetworkDto(null, null, yarnParams, null, null, 1);
        ValidationResultBuilder resultBuilder = new ValidationResultBuilder();

        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setNetwork(networkDto);

        underTest.validateDuringFlow(environmentDto, networkDto, resultBuilder);

        NetworkTestUtils.checkErrorsPresent(resultBuilder, List.of(
                "The 'lifetime' parameter should be non negative for 'YARN' environment specific network!"
        ));
    }

}

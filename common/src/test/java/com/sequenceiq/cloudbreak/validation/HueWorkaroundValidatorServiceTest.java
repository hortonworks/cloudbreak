package com.sequenceiq.cloudbreak.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

@RunWith(MockitoJUnitRunner.class)
public class HueWorkaroundValidatorServiceTest {

    private HueWorkaroundValidatorService underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new HueWorkaroundValidatorService();
    }

    @Test
    public void validateBlueprintRequestWhenHueIsPresentedAndAllTheGroupNameHasTheCorrectLengthShouldNotThrowException() {
        assertDoesNotThrow(() -> underTest.validateForBlueprintRequest(Set.of("master")));
    }

    @Test
    public void validateBlueprintRequestWhenHueIsPresentedAndHueHostgroupIsTooLongMustThrowException() {
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.validateForBlueprintRequest(Set.of("master123456")));
        Assert.assertEquals("Hue does not support CDP Data Hubs where hostgroup name is longer than 7 characters. "
                        + "Please retry creating the template by shortening the hostgroup name: master123456  under 7 characters.",
                badRequestException.getMessage());
    }

    @Test
    public void validateStackRequestWhenHueIsPresentedAndStackNameTooLongMustThrowException() {
        String stackName = "iamveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryveryverylong";
        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.validateForStackRequest(Set.of("master"), stackName));
        Assert.assertEquals("Your Data Hub contains Hue. Hue does not support CDP Data Hubs where Data Hub name is longer than 20 characters. "
                        + "Please retry creating Data Hub using a Data Hub name that is shorter than 20 characters.",
                badRequestException.getMessage());
    }

    @Test
    public void validateStackRequestWhenHueIsPresentedAndHueHostgroupIsTooLongMustThrowException() {
        String stackName = "thisisfine";

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.validateForStackRequest(Set.of("master123456"), stackName));
        Assert.assertEquals("Hue does not support CDP Data Hubs where hostgroup name is longer than 7 characters. "
                        + "Please retry creating the template by shortening the hostgroup name: master123456  under 7 characters.",
                badRequestException.getMessage());
    }

    @Test
    public void validateStackRequestWhenHueIsPresentedAndHueHostgroupIsGateWayMustTNothrowException() {
        String stackName = "thisisfine";

        assertDoesNotThrow(() -> underTest.validateForStackRequest(Set.of("gateway"), stackName));
    }

    @Test
    public void validateEndpointNameIfHuePresentedAndEndpointNameTooLongShouldThrowException() {
        String endpointName = "thisisfine.thisisfinethisisfinethisisfinethisisfinethisisfinethisisfinethisisfinethisisfine";

        BadRequestException badRequestException = assertThrows(BadRequestException.class,
                () -> underTest.validateForEnvironmentDomainName(Set.of("master"), endpointName));
        Assert.assertEquals("Your Data Hub contains Hue. Hue does not support CDP Data Hubs where the fully qualified domain name of a "
                        + "VM is longer than 63 characters. Generated hostname: "
                        + "thisisfine.thisisfinethisisfinethisisfinethisisfinethisisfinethisisfinethisisfinethisisfine. "
                        + "Please retry creating Data Hub using a Data Hub name that is shorter than 20 characters and a"
                        + " hostgroup name that is shorter than 7 characters.",
                badRequestException.getMessage());
    }

}
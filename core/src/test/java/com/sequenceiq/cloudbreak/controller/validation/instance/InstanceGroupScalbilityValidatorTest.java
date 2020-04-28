package com.sequenceiq.cloudbreak.controller.validation.instance;

import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.State;
import com.sequenceiq.common.api.type.ScalingMode;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class InstanceGroupScalbilityValidatorTest {

    @InjectMocks
    private InstanceGroupScalbilityValidator underTest;

    @Test
    public void validate() {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("test");

        instanceGroup.setScalingMode(ScalingMode.UNSPECIFIED);
        ValidationResult validate = underTest.validate(instanceGroup);
        assertEquals(State.VALID, validate.getState());
        assertEquals("", validate.getFormattedErrors());

        instanceGroup.setScalingMode(ScalingMode.NONE);
        validate = underTest.validate(instanceGroup);
        assertEquals(State.ERROR, validate.getState());
        assertEquals("1. Instance group '%s' in stack '%s' is not enabled to scale.", validate.getFormattedErrors());

        instanceGroup = null;
        validate = underTest.validate(instanceGroup);
        assertEquals(State.ERROR, validate.getState());
        assertEquals("1. Instance group '%s' not found in stack '%s'.", validate.getFormattedErrors());
    }
}

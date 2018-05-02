package com.sequenceiq.cloudbreak.controller.validation.stack;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import com.sequenceiq.cloudbreak.api.model.InstanceGroupRequest;
import com.sequenceiq.cloudbreak.api.model.StackRequest;
import com.sequenceiq.cloudbreak.api.model.TemplateRequest;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult.State;
import com.sequenceiq.cloudbreak.controller.validation.template.TemplateRequestValidator;

public class StackRequestValidatorTest {

    private final TemplateRequestValidator templateRequestValidator = new TemplateRequestValidator();

    private final StackRequestValidator underTest = new StackRequestValidator(templateRequestValidator);

    @Test
    public void testWithZeroRootVolumeSize() {
        StackRequest stackRequest = stackRequestWithRootVolumeSize(0);
        ValidationResult validationResult = underTest.validate(stackRequest);
        assertEquals(State.ERROR, validationResult.getState());
    }

    @Test
    public void testWithNegativeRootVolumeSize() {
        StackRequest stackRequest = stackRequestWithRootVolumeSize(-1);
        ValidationResult validationResult = underTest.validate(stackRequest);
        assertEquals(State.ERROR, validationResult.getState());
    }

    @Test
    public void testNullValueIsAllowedForRootVolumeSize() {
        StackRequest stackRequest = stackRequestWithRootVolumeSize(null);
        ValidationResult validationResult = underTest.validate(stackRequest);
        assertEquals(State.VALID, validationResult.getState());
    }

    @Test
    public void testWithPositiveRootVolumeSize() {
        StackRequest stackRequest = stackRequestWithRootVolumeSize(1);
        ValidationResult validationResult = underTest.validate(stackRequest);
        assertEquals(State.VALID, validationResult.getState());
    }

    private StackRequest stackRequestWithRootVolumeSize(Integer rootVolumeSize) {
        TemplateRequest templateRequest = new TemplateRequest();
        templateRequest.setRootVolumeSize(rootVolumeSize);
        InstanceGroupRequest instanceGroupRequest = new InstanceGroupRequest();
        instanceGroupRequest.setTemplate(templateRequest);
        StackRequest stackRequest = new StackRequest();
        stackRequest.setInstanceGroups(Arrays.asList(instanceGroupRequest));
        return stackRequest;
    }

}
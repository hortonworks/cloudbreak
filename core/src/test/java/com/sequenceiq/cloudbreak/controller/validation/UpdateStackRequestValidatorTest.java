package com.sequenceiq.cloudbreak.controller.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintTarget;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import javax.validation.Payload;
import javax.validation.metadata.ConstraintDescriptor;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.model.InstanceGroupAdjustmentJson;
import com.sequenceiq.cloudbreak.api.model.StatusRequest;
import com.sequenceiq.cloudbreak.api.model.UpdateStackJson;
import com.sequenceiq.cloudbreak.validation.UpdateStackRequestValidator;

public class UpdateStackRequestValidatorTest {

    private UpdateStackRequestValidator underTest;

    private ConstraintValidatorContext constraintValidatorContext;

    @Before
    public void setUp() {
        underTest = new UpdateStackRequestValidator();
        constraintValidatorContext = new ConstraintValidatorContextImpl(
                new ArrayList<>(), null,
                PathImpl.createRootPath(),
                new DummyConstraintDescriptor()
        );
    }

    @Test
    public void testIsValidShouldReturnTrueWhenStatusIsUpdated() {
        UpdateStackJson updateStackJson = new UpdateStackJson();
        updateStackJson.setInstanceGroupAdjustment(null);
        updateStackJson.setStatus(StatusRequest.STARTED);
        boolean valid = underTest.isValid(updateStackJson, constraintValidatorContext);
        assertTrue(valid);
    }

    @Test
    public void testIsValidShouldReturnTrueWhenNodeCountIsUpdated() {
        UpdateStackJson updateStackJson = new UpdateStackJson();
        InstanceGroupAdjustmentJson instanceGroupAdjustmentJson = new InstanceGroupAdjustmentJson();
        instanceGroupAdjustmentJson.setScalingAdjustment(12);
        instanceGroupAdjustmentJson.setInstanceGroup("slave_1");
        updateStackJson.setInstanceGroupAdjustment(instanceGroupAdjustmentJson);
        updateStackJson.setStatus(null);
        boolean valid = underTest.isValid(updateStackJson, constraintValidatorContext);
        assertTrue(valid);
    }

    @Test
    public void testInValidShouldReturnTrueWhenNodeCountIsLowerThanOneUpdatedAndWithClusterEvent() {
        UpdateStackJson updateStackJson = new UpdateStackJson();
        updateStackJson.setWithClusterEvent(true);
        InstanceGroupAdjustmentJson instanceGroupAdjustmentJson = new InstanceGroupAdjustmentJson();
        instanceGroupAdjustmentJson.setScalingAdjustment(-1);
        instanceGroupAdjustmentJson.setInstanceGroup("slave_1");
        updateStackJson.setInstanceGroupAdjustment(instanceGroupAdjustmentJson);
        updateStackJson.setStatus(null);
        boolean valid = underTest.isValid(updateStackJson, constraintValidatorContext);
        assertFalse(valid);
    }

    @Test
    public void testIsValidShouldReturnFalseWhenRequestContainsNodeCountAndStatus() {
        UpdateStackJson updateStackJson = new UpdateStackJson();
        InstanceGroupAdjustmentJson instanceGroupAdjustmentJson = new InstanceGroupAdjustmentJson();
        instanceGroupAdjustmentJson.setScalingAdjustment(4);
        instanceGroupAdjustmentJson.setInstanceGroup("slave_1");
        updateStackJson.setStatus(StatusRequest.STARTED);
        updateStackJson.setInstanceGroupAdjustment(instanceGroupAdjustmentJson);
        boolean valid = underTest.isValid(updateStackJson, constraintValidatorContext);
        assertFalse(valid);
    }

    @Test
    public void testIsValidShouldReturnFalseWhenRequestContainsOnlyNulls() {

        UpdateStackJson updateStackJson = new UpdateStackJson();
        updateStackJson.setInstanceGroupAdjustment(null);
        updateStackJson.setStatus(null);
        boolean valid = underTest.isValid(updateStackJson, constraintValidatorContext);
        assertFalse(valid);
    }

    private static class DummyAnnotation implements Annotation {

        @Override
        public boolean equals(Object obj) {
            return this == obj;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public String toString() {
            return "dummy";
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return getClass();
        }
    }

    private static class DummyConstraintDescriptor implements ConstraintDescriptor<DummyAnnotation> {

        @Override
        public DummyAnnotation getAnnotation() {
            return null;
        }

        @Override
        public String getMessageTemplate() {
            return "";
        }

        @Override
        public Set<Class<?>> getGroups() {
            return Collections.emptySet();
        }

        @Override
        public Set<Class<? extends Payload>> getPayload() {
            return Collections.emptySet();
        }

        @Override
        public ConstraintTarget getValidationAppliesTo() {
            return ConstraintTarget.PARAMETERS;
        }

        @Override
        public List<Class<? extends ConstraintValidator<DummyAnnotation, ?>>> getConstraintValidatorClasses() {
            return Collections.emptyList();
        }

        @Override
        public Map<String, Object> getAttributes() {
            return Collections.emptyMap();
        }

        @Override
        public Set<ConstraintDescriptor<?>> getComposingConstraints() {
            return Collections.emptySet();
        }

        @Override
        public boolean isReportAsSingleViolation() {
            return false;
        }
    }
}
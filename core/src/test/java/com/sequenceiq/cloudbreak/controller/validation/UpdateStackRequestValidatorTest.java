package com.sequenceiq.cloudbreak.controller.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.validation.ConstraintTarget;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.metadata.ConstraintDescriptor;
import jakarta.validation.metadata.ValidateUnwrappedValue;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.hibernate.validator.messageinterpolation.ExpressionLanguageFeatureLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.UpdateStackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.StatusRequest;
import com.sequenceiq.cloudbreak.validation.UpdateStackRequestValidator;

class UpdateStackRequestValidatorTest {

    private UpdateStackRequestValidator underTest;

    private ConstraintValidatorContext constraintValidatorContext;

    @BeforeEach
    void setUp() {
        underTest = new UpdateStackRequestValidator();
        constraintValidatorContext = new ConstraintValidatorContextImpl(
                null,
                PathImpl.createPathFromString("status"),
                new DummyConstraintDescriptor(),
                null,
                ExpressionLanguageFeatureLevel.NONE,
                ExpressionLanguageFeatureLevel.NONE
        );
    }

    @Test
    void testIsValidShouldReturnTrueWhenStatusIsUpdated() {
        UpdateStackV4Request updateStackJson = new UpdateStackV4Request();
        updateStackJson.setInstanceGroupAdjustment(null);
        updateStackJson.setStatus(StatusRequest.STARTED);
        boolean valid = underTest.isValid(updateStackJson, constraintValidatorContext);
        assertTrue(valid);
    }

    @Test
    void testIsValidShouldReturnTrueWhenNodeCountIsUpdated() {
        UpdateStackV4Request updateStackJson = new UpdateStackV4Request();
        InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson = new InstanceGroupAdjustmentV4Request();
        instanceGroupAdjustmentJson.setScalingAdjustment(12);
        instanceGroupAdjustmentJson.setInstanceGroup("slave_1");
        updateStackJson.setInstanceGroupAdjustment(instanceGroupAdjustmentJson);
        updateStackJson.setStatus(null);
        boolean valid = underTest.isValid(updateStackJson, constraintValidatorContext);
        assertTrue(valid);
    }

    @Test
    void testInValidShouldReturnTrueWhenNodeCountIsLowerThanOneUpdatedAndWithClusterEvent() {
        UpdateStackV4Request updateStackJson = new UpdateStackV4Request();
        updateStackJson.setWithClusterEvent(true);
        InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson = new InstanceGroupAdjustmentV4Request();
        instanceGroupAdjustmentJson.setScalingAdjustment(-1);
        instanceGroupAdjustmentJson.setInstanceGroup("slave_1");
        updateStackJson.setInstanceGroupAdjustment(instanceGroupAdjustmentJson);
        updateStackJson.setStatus(null);
        boolean valid = underTest.isValid(updateStackJson, constraintValidatorContext);
        assertFalse(valid);
    }

    @Test
    void testIsValidShouldReturnFalseWhenRequestContainsNodeCountAndStatus() {
        UpdateStackV4Request updateStackJson = new UpdateStackV4Request();
        InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson = new InstanceGroupAdjustmentV4Request();
        instanceGroupAdjustmentJson.setScalingAdjustment(4);
        instanceGroupAdjustmentJson.setInstanceGroup("slave_1");
        updateStackJson.setStatus(StatusRequest.STARTED);
        updateStackJson.setInstanceGroupAdjustment(instanceGroupAdjustmentJson);
        boolean valid = underTest.isValid(updateStackJson, constraintValidatorContext);
        assertFalse(valid);
    }

    @Test
    void testIsValidShouldReturnFalseWhenRequestContainsOnlyNulls() {

        UpdateStackV4Request updateStackJson = new UpdateStackV4Request();
        updateStackJson.setInstanceGroupAdjustment(null);
        updateStackJson.setStatus(null);
        boolean valid = underTest.isValid(updateStackJson, constraintValidatorContext);
        assertFalse(valid);
    }

    @Test
    void testIsValidShouldReturnFalseWhenNodeCountIsUpdatedWithoutScalingAdjustment() {
        UpdateStackV4Request updateStackJson = new UpdateStackV4Request();
        InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson = new InstanceGroupAdjustmentV4Request();
        instanceGroupAdjustmentJson.setInstanceGroup("slave_1");
        updateStackJson.setInstanceGroupAdjustment(instanceGroupAdjustmentJson);
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

        @Override
        public ValidateUnwrappedValue getValueUnwrapping() {
            return null;
        }

        @Override
        public <U> U unwrap(Class<U> type) {
            return null;
        }
    }
}

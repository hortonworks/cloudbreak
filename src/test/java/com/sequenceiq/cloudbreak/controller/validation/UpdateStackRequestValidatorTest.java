package com.sequenceiq.cloudbreak.controller.validation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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

import com.sequenceiq.cloudbreak.controller.json.UpdateStackJson;
import com.sequenceiq.cloudbreak.domain.StatusRequest;

public class UpdateStackRequestValidatorTest {

    private UpdateStackRequestValidator underTest;

    private ConstraintValidatorContext constraintValidatorContext;

    @Before
    public void setUp() {
        underTest = new UpdateStackRequestValidator();
        constraintValidatorContext = new ConstraintValidatorContextImpl(
                new ArrayList<String>(),
                PathImpl.createRootPath(),
                new DummyConstraintDescriptor()
                );
    }

    @Test
    public void testIsValidShouldReturnTrueWhenStatusIsUpdated() {
        UpdateStackJson updateStackJson = new UpdateStackJson();
        updateStackJson.setScalingAdjustment(null);
        updateStackJson.setStatus(StatusRequest.STARTED);
        boolean valid = underTest.isValid(updateStackJson, constraintValidatorContext);
        assertTrue(valid);
    }

    @Test
    public void testIsValidShouldReturnTrueWhenNodeCountIsUpdated() {
        UpdateStackJson updateStackJson = new UpdateStackJson();
        updateStackJson.setScalingAdjustment(12);
        updateStackJson.setStatus(null);
        boolean valid = underTest.isValid(updateStackJson, constraintValidatorContext);
        assertTrue(valid);
    }

    @Test
    public void testIsValidShouldReturnFalseWhenRequestContainsNodeCountAndStatus() {
        UpdateStackJson updateStackJson = new UpdateStackJson();
        updateStackJson.setScalingAdjustment(4);
        updateStackJson.setStatus(StatusRequest.STARTED);
        boolean valid = underTest.isValid(updateStackJson, constraintValidatorContext);
        assertFalse(valid);
    }

    @Test
    public void testIsValidShouldReturnFalseWhenRequestContainsOnlyNulls() {

        UpdateStackJson updateStackJson = new UpdateStackJson();
        updateStackJson.setScalingAdjustment(null);
        updateStackJson.setStatus(null);
        boolean valid = underTest.isValid(updateStackJson, constraintValidatorContext);
        assertFalse(valid);
    }

    private class DummyAnnotation implements Annotation {

        @Override
        public boolean equals(Object obj) {
            return false;
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

    private class DummyConstraintDescriptor implements ConstraintDescriptor<DummyAnnotation>, Serializable {

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
            return new HashSet<>();
        }

        @Override
        public Set<Class<? extends Payload>> getPayload() {
            return new HashSet<>();
        }

        @Override
        public ConstraintTarget getValidationAppliesTo() {
            return ConstraintTarget.PARAMETERS;
        }

        @Override
        public List<Class<? extends ConstraintValidator<DummyAnnotation, ?>>> getConstraintValidatorClasses() {
            return new ArrayList<>();
        }

        @Override
        public Map<String, Object> getAttributes() {
            return new HashMap<>();
        }

        @Override
        public Set<ConstraintDescriptor<?>> getComposingConstraints() {
            return new HashSet<>();
        }

        @Override
        public boolean isReportAsSingleViolation() {
            return false;
        }
    }
}
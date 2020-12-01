package com.sequenceiq.cloudbreak.controller.validation;

import static org.junit.Assert.assertTrue;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintTarget;
import javax.validation.ConstraintValidator;
import javax.validation.Payload;
import javax.validation.metadata.ConstraintDescriptor;
import javax.validation.metadata.ValidateUnwrappedValue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.validation.network.NetworkConfigurationValidator;

@RunWith(MockitoJUnitRunner.class)
public class NetworkConfigurationValidatorTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @InjectMocks
    private NetworkConfigurationValidator underTest;

    @Test
    public void validNetworkRequestReturnTrue() {
        assertTrue(underTest.validateNetworkForStack(TestUtil.network(), TestUtil.generateGcpInstanceGroupsByNodeCount(1, 2, 3)));
    }

    @Test
    public void inValidNetworkRequestReturnFalse() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Cannot assign more than 0 addresses in the selected subnet.");
        underTest.validateNetworkForStack(TestUtil.network("10.0.0.1/32"), TestUtil.generateGcpInstanceGroupsByNodeCount(10000, 10000, 10000));
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
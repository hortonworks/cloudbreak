package com.sequenceiq.cloudbreak.controller.validation;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;

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
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.InstanceType;
import com.google.common.collect.ImmutableList;
import com.sequenceiq.cloudbreak.controller.json.TemplateJson;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;

@RunWith(MockitoJUnitRunner.class)
public class ProvisionParametersValidatorTest {

    @InjectMocks
    private ProvisionParametersValidator underTest;

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Mock
    private ValidProvisionRequest validProvisionRequest;

    @Before
    public void setUp() {
        underTest.initialize(validProvisionRequest);
        ReflectionTestUtils.setField(underTest, "parameterValidators",
                ImmutableList.of(
                        new ParametersRequiredValidator(),
                        new ParametersTypeValidator(),
                        new ParametersRegexValidator()
                )
        );
        given(constraintValidatorContext.buildConstraintViolationWithTemplate(anyString())).willReturn(
                new ConstraintValidatorContextImpl(
                        new ArrayList<String>(),
                        PathImpl.createRootPath(),
                        new DummyConstraintDescriptor()
                ).buildConstraintViolationWithTemplate("dummytemplate")
        );
    }

    @Test
    public void validAwsTemplateJsonWillReturnTrue() {
        TemplateJson templateJson = new TemplateJson();
        templateJson.setCloudPlatform(CloudPlatform.AWS);
        templateJson.setDescription("description");
        templateJson.setName("name");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(AwsTemplateParam.AMI_ID.getName(), "ami");
        parameters.put(AwsTemplateParam.INSTANCE_TYPE.getName(), InstanceType.C1Medium.name());
        parameters.put(AwsTemplateParam.REGION.getName(), Regions.AP_NORTHEAST_1);
        parameters.put(AwsTemplateParam.SSH_LOCATION.getName(), "0.0.0.0/0");
        templateJson.setParameters(parameters);
        assertEquals(underTest.isValid(templateJson, constraintValidatorContext), true);
    }

    @Test
    public void validAwsTemplateWithInvalidSshLocationJsonWillReturnTrue() {
        TemplateJson templateJson = new TemplateJson();
        templateJson.setCloudPlatform(CloudPlatform.AWS);
        templateJson.setDescription("description");
        templateJson.setName("name");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(AwsTemplateParam.AMI_ID.getName(), "ami");
        parameters.put(AwsTemplateParam.INSTANCE_TYPE.getName(), InstanceType.C1Medium.name());
        parameters.put(AwsTemplateParam.REGION.getName(), Regions.AP_NORTHEAST_1);
        parameters.put(AwsTemplateParam.SSH_LOCATION.getName(), "0.0.0.0");
        templateJson.setParameters(parameters);
        assertEquals(underTest.isValid(templateJson, constraintValidatorContext), false);
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
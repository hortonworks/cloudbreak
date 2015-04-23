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

import com.sequenceiq.cloudbreak.controller.json.TemplateRequest;
import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.amazonaws.services.ec2.model.InstanceType;
import com.google.common.collect.ImmutableList;
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
        TemplateRequest templateJson = new TemplateRequest();
        templateJson.setCloudPlatform(CloudPlatform.AWS);
        templateJson.setDescription("description");
        templateJson.setName("name");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(AwsTemplateParam.INSTANCE_TYPE.getName(), InstanceType.C1Medium.name());
        parameters.put(AwsTemplateParam.SSH_LOCATION.getName(), "0.0.0.0/0");
        templateJson.setVolumeCount(3);
        templateJson.setVolumeSize(30);
        parameters.put(AwsTemplateParam.VOLUME_TYPE.getName(), "Gp2");
        templateJson.setParameters(parameters);
        assertEquals(underTest.isValid(templateJson, constraintValidatorContext), true);
    }

    @Test
    public void validAwsTemplateWithSpecificSshJsonWillReturnTrue() {
        TemplateRequest templateJson = new TemplateRequest();
        templateJson.setCloudPlatform(CloudPlatform.AWS);
        templateJson.setDescription("description");
        templateJson.setName("name");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(AwsTemplateParam.INSTANCE_TYPE.getName(), InstanceType.C1Medium.name());
        parameters.put(AwsTemplateParam.SSH_LOCATION.getName(), "192.12.12.12/12");
        templateJson.setVolumeCount(3);
        templateJson.setVolumeSize(30);
        parameters.put(AwsTemplateParam.VOLUME_TYPE.getName(), "Gp2");
        templateJson.setParameters(parameters);
        assertEquals(underTest.isValid(templateJson, constraintValidatorContext), true);
    }

    @Test
    public void validAwsTemplateWithInvalidSshLocationJsonWillReturnTrue() {
        TemplateRequest templateJson = new TemplateRequest();
        templateJson.setCloudPlatform(CloudPlatform.AWS);
        templateJson.setDescription("description");
        templateJson.setName("name");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(AwsTemplateParam.INSTANCE_TYPE.getName(), InstanceType.C1Medium.name());
        parameters.put(AwsTemplateParam.SSH_LOCATION.getName(), "0.0.0.0");
        templateJson.setVolumeCount(3);
        templateJson.setVolumeSize(30);
        parameters.put(AwsTemplateParam.VOLUME_TYPE.getName(), "Gp2");
        templateJson.setParameters(parameters);
        assertEquals(underTest.isValid(templateJson, constraintValidatorContext), false);
    }

    @Test
    public void validAwsTemplateWithSpotPriceWithIntegerJsonWillReturnTrue() {
        TemplateRequest templateJson = new TemplateRequest();
        templateJson.setCloudPlatform(CloudPlatform.AWS);
        templateJson.setDescription("description");
        templateJson.setName("name");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(AwsTemplateParam.INSTANCE_TYPE.getName(), InstanceType.C1Medium.name());
        parameters.put(AwsTemplateParam.SSH_LOCATION.getName(), "0.0.0.0/0");
        templateJson.setVolumeCount(3);
        templateJson.setVolumeSize(30);
        parameters.put(AwsTemplateParam.VOLUME_TYPE.getName(), "Gp2");
        parameters.put(AwsTemplateParam.SPOT_PRICE.getName(), Integer.valueOf(1));
        templateJson.setParameters(parameters);
        assertEquals(underTest.isValid(templateJson, constraintValidatorContext), true);
    }

    @Test
    public void validAwsTemplateWithSpotPriceWithStringJsonWillReturnFalse() {
        TemplateRequest templateJson = new TemplateRequest();
        templateJson.setCloudPlatform(CloudPlatform.AWS);
        templateJson.setDescription("description");
        templateJson.setName("name");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(AwsTemplateParam.INSTANCE_TYPE.getName(), InstanceType.C1Medium.name());
        parameters.put(AwsTemplateParam.SSH_LOCATION.getName(), "0.0.0.0/0");
        templateJson.setVolumeCount(3);
        templateJson.setVolumeSize(30);
        parameters.put(AwsTemplateParam.VOLUME_TYPE.getName(), "Gp2");
        parameters.put(AwsTemplateParam.SPOT_PRICE.getName(), "apple");
        templateJson.setParameters(parameters);
        assertEquals(underTest.isValid(templateJson, constraintValidatorContext), false);
    }

    @Test
    public void validAwsTemplateWithSpotPriceWithDoubleJsonWillReturnTrue() {
        TemplateRequest templateJson = new TemplateRequest();
        templateJson.setCloudPlatform(CloudPlatform.AWS);
        templateJson.setDescription("description");
        templateJson.setName("name");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(AwsTemplateParam.INSTANCE_TYPE.getName(), InstanceType.C1Medium.name());
        parameters.put(AwsTemplateParam.SSH_LOCATION.getName(), "0.0.0.0/0");
        templateJson.setVolumeCount(3);
        templateJson.setVolumeSize(30);
        parameters.put(AwsTemplateParam.VOLUME_TYPE.getName(), "Gp2");
        parameters.put(AwsTemplateParam.SPOT_PRICE.getName(), Double.valueOf(1.2));
        templateJson.setParameters(parameters);
        assertEquals(underTest.isValid(templateJson, constraintValidatorContext), true);
    }

    @Test
    public void validAwsTemplateWithInvalidSshLocationWithSpecificNumberJsonWillReturnTrue() {
        TemplateRequest templateJson = new TemplateRequest();
        templateJson.setCloudPlatform(CloudPlatform.AWS);
        templateJson.setDescription("description");
        templateJson.setName("name");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(AwsTemplateParam.INSTANCE_TYPE.getName(), InstanceType.C1Medium.name());
        parameters.put(AwsTemplateParam.SSH_LOCATION.getName(), "192.0.0.0/256");
        templateJson.setVolumeCount(3);
        templateJson.setVolumeSize(30);
        parameters.put(AwsTemplateParam.VOLUME_TYPE.getName(), "Gp2");
        templateJson.setParameters(parameters);
        assertEquals(underTest.isValid(templateJson, constraintValidatorContext), false);
    }

    @Test
    public void awsTemplateJsonWithInvalidVolumeTypeFails() {
        TemplateRequest templateJson = new TemplateRequest();
        templateJson.setCloudPlatform(CloudPlatform.AWS);
        templateJson.setDescription("description");
        templateJson.setName("name");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(AwsTemplateParam.INSTANCE_TYPE.getName(), InstanceType.C1Medium.name());
        parameters.put(AwsTemplateParam.SSH_LOCATION.getName(), "0.0.0.0/0");
        templateJson.setVolumeCount(3);
        templateJson.setVolumeSize(30);
        parameters.put(AwsTemplateParam.VOLUME_TYPE.getName(), "invalid");
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
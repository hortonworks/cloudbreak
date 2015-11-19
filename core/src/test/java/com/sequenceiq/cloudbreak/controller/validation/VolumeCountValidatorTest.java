package com.sequenceiq.cloudbreak.controller.validation;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

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

import com.sequenceiq.cloudbreak.controller.json.TemplateRequest;
import com.sequenceiq.cloudbreak.common.type.CloudPlatform;

@RunWith(MockitoJUnitRunner.class)
public class VolumeCountValidatorTest {

    private static final String C1XLARGE_INSTANCE = "c1.xlarge";
    private static final String C3LARGE_INSTANCE = "c3.large";

    @InjectMocks
    private VolumeCountValidator underTest;

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Mock
    private ValidVolume validVolume;

    @Before
    public void setUp() {
        when(validVolume.minCount()).thenReturn(1);
        when(validVolume.maxCount()).thenReturn(24);
        when(validVolume.minSize()).thenReturn(10);
        when(validVolume.maxSize()).thenReturn(1000);

        underTest.initialize(validVolume);
        given(constraintValidatorContext.buildConstraintViolationWithTemplate(anyString())).willReturn(
                new ConstraintValidatorContextImpl(
                        new ArrayList<String>(),
                        PathImpl.createRootPath(),
                        new DummyConstraintDescriptor()
                ).buildConstraintViolationWithTemplate("dummytemplate")
        );
    }

    @Test
    public void validAwsTemplateDisksReturnTrue() {
        TemplateRequest templateJson = new TemplateRequest();
        Map<String, Object> parameters = new HashMap<>();
        templateJson.setVolumeCount(3);
        templateJson.setVolumeSize(30);
        templateJson.setCloudPlatform(CloudPlatform.AWS);
        parameters.put(AwsTemplateParam.VOLUME_TYPE.getName(), "gp2");
        parameters.put(AwsTemplateParam.INSTANCE_TYPE.getName(), C1XLARGE_INSTANCE);
        templateJson.setParameters(parameters);
        assertEquals(underTest.isValid(templateJson, constraintValidatorContext), true);
    }

    @Test
    public void validAzureTemplateDisksReturnTrue() {
        TemplateRequest templateJson = new TemplateRequest();
        Map<String, Object> parameters = new HashMap<>();
        templateJson.setVolumeCount(3);
        templateJson.setVolumeSize(30);
        templateJson.setCloudPlatform(CloudPlatform.AZURE);

        templateJson.setParameters(parameters);
        assertEquals(underTest.isValid(templateJson, constraintValidatorContext), true);
    }

    @Test
    public void inValidAwsTemplateInValidDisk1SizeReturnTrue() {
        TemplateRequest templateJson = new TemplateRequest();
        Map<String, Object> parameters = new HashMap<>();
        templateJson.setVolumeCount(3);
        templateJson.setVolumeSize(3000);
        templateJson.setCloudPlatform(CloudPlatform.AWS);
        parameters.put(AwsTemplateParam.VOLUME_TYPE.getName(), "gp2");
        parameters.put(AwsTemplateParam.INSTANCE_TYPE.getName(), C1XLARGE_INSTANCE);

        templateJson.setParameters(parameters);
        assertEquals(underTest.isValid(templateJson, constraintValidatorContext), false);
    }

    @Test
    public void inValidAwsTemplateInValidDisk2SizeReturnTrue() {
        TemplateRequest templateJson = new TemplateRequest();
        Map<String, Object> parameters = new HashMap<>();
        templateJson.setVolumeCount(3);
        templateJson.setVolumeSize(0);
        templateJson.setCloudPlatform(CloudPlatform.AWS);
        parameters.put(AwsTemplateParam.VOLUME_TYPE.getName(), "gp2");
        parameters.put(AwsTemplateParam.INSTANCE_TYPE.getName(), C1XLARGE_INSTANCE);

        templateJson.setParameters(parameters);
        assertEquals(underTest.isValid(templateJson, constraintValidatorContext), false);
    }

    @Test
    public void inValidAwsTemplateInValidDiskCount1SizeReturnTrue() {
        TemplateRequest templateJson = new TemplateRequest();
        Map<String, Object> parameters = new HashMap<>();
        templateJson.setVolumeCount(30);
        templateJson.setVolumeSize(100);
        templateJson.setCloudPlatform(CloudPlatform.AWS);
        parameters.put(AwsTemplateParam.VOLUME_TYPE.getName(), "gp2");
        parameters.put(AwsTemplateParam.INSTANCE_TYPE.getName(), C1XLARGE_INSTANCE);

        templateJson.setParameters(parameters);
        assertEquals(underTest.isValid(templateJson, constraintValidatorContext), false);
    }

    @Test
    public void inValidAwsTemplateInValidDiskCount2SizeReturnTrue() {
        TemplateRequest templateJson = new TemplateRequest();
        Map<String, Object> parameters = new HashMap<>();
        templateJson.setVolumeCount(0);
        templateJson.setVolumeSize(100);
        templateJson.setCloudPlatform(CloudPlatform.AWS);
        parameters.put(AwsTemplateParam.VOLUME_TYPE.getName(), "gp2");
        parameters.put(AwsTemplateParam.INSTANCE_TYPE.getName(), C1XLARGE_INSTANCE);

        templateJson.setParameters(parameters);
        assertEquals(underTest.isValid(templateJson, constraintValidatorContext), false);
    }

    @Test
    public void validephemeralAwsTemplateDisksReturnTrue() {
        TemplateRequest templateJson = new TemplateRequest();
        Map<String, Object> parameters = new HashMap<>();
        templateJson.setVolumeCount(1);
        templateJson.setVolumeSize(30);
        templateJson.setCloudPlatform(CloudPlatform.AWS);
        parameters.put(AwsTemplateParam.VOLUME_TYPE.getName(), "ephemeral");
        parameters.put(AwsTemplateParam.INSTANCE_TYPE.getName(), C3LARGE_INSTANCE);

        templateJson.setParameters(parameters);
        assertEquals(underTest.isValid(templateJson, constraintValidatorContext), true);
    }

    @Test
    public void inValidEphemeralAwsTemplateInValidDisk1SizeReturnTrue() {
        TemplateRequest templateJson = new TemplateRequest();
        Map<String, Object> parameters = new HashMap<>();
        templateJson.setVolumeCount(3);
        templateJson.setVolumeSize(3000);
        templateJson.setCloudPlatform(CloudPlatform.AWS);
        parameters.put(AwsTemplateParam.VOLUME_TYPE.getName(), "ephemeral");
        parameters.put(AwsTemplateParam.INSTANCE_TYPE.getName(), C3LARGE_INSTANCE);

        templateJson.setParameters(parameters);
        assertEquals(underTest.isValid(templateJson, constraintValidatorContext), false);
    }

    @Test
    public void inValidEphemeralAwsTemplateInValidDisk2SizeReturnTrue() {
        TemplateRequest templateJson = new TemplateRequest();
        Map<String, Object> parameters = new HashMap<>();
        templateJson.setVolumeCount(3);
        templateJson.setVolumeSize(0);
        templateJson.setCloudPlatform(CloudPlatform.AWS);
        parameters.put(AwsTemplateParam.VOLUME_TYPE.getName(), "ephemeral");
        parameters.put(AwsTemplateParam.INSTANCE_TYPE.getName(), C3LARGE_INSTANCE);

        templateJson.setParameters(parameters);
        assertEquals(underTest.isValid(templateJson, constraintValidatorContext), false);
    }

    @Test
    public void inValidEphemeralAwsTemplateInValidDiskCount1SizeReturnTrue() {
        TemplateRequest templateJson = new TemplateRequest();
        Map<String, Object> parameters = new HashMap<>();
        templateJson.setVolumeCount(30);
        templateJson.setVolumeSize(100);
        templateJson.setCloudPlatform(CloudPlatform.AWS);
        parameters.put(AwsTemplateParam.VOLUME_TYPE.getName(), "ephemeral");
        parameters.put(AwsTemplateParam.INSTANCE_TYPE.getName(), C3LARGE_INSTANCE);

        templateJson.setParameters(parameters);
        assertEquals(underTest.isValid(templateJson, constraintValidatorContext), false);
    }

    @Test
    public void inValidEphemeralAwsTemplateInValidDiskCount2SizeReturnTrue() {
        TemplateRequest templateJson = new TemplateRequest();
        Map<String, Object> parameters = new HashMap<>();
        templateJson.setVolumeCount(0);
        templateJson.setVolumeSize(100);
        templateJson.setCloudPlatform(CloudPlatform.AWS);
        parameters.put(AwsTemplateParam.VOLUME_TYPE.getName(), "ephemeral");
        parameters.put(AwsTemplateParam.INSTANCE_TYPE.getName(), C3LARGE_INSTANCE);

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
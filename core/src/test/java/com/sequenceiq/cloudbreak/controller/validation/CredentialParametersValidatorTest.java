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

import com.google.common.collect.ImmutableList;
import com.sequenceiq.cloudbreak.controller.json.CredentialRequest;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;

@RunWith(MockitoJUnitRunner.class)
public class CredentialParametersValidatorTest {

    @InjectMocks
    private CredentialParametersValidator underTest;

    @Mock
    private ConstraintValidatorContext constraintValidatorContext;

    @Mock
    private ValidCredentialRequest validCredentialRequest;

    @Before
    public void setUp() {
        underTest.initialize(validCredentialRequest);
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
    public void validAwsCredentialJsonWillReturnTrue() {
        CredentialRequest credentialJson = new CredentialRequest();
        credentialJson.setCloudPlatform(CloudPlatform.AWS);
        credentialJson.setDescription("description");
        credentialJson.setName("name");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(AWSCredentialParam.ROLE_ARN.getName(), "arn");
        credentialJson.setParameters(parameters);
        credentialJson.setPublicKey("ssh key");
        assertEquals(underTest.isValid(credentialJson, constraintValidatorContext), true);
    }

    @Test
    public void validAwsCredentialJsonRoleArnMissingValidationFails() {
        CredentialRequest credentialJson = new CredentialRequest();
        credentialJson.setCloudPlatform(CloudPlatform.AWS);
        credentialJson.setDescription("description");
        credentialJson.setName("name");
        Map<String, Object> parameters = new HashMap<>();
        credentialJson.setParameters(parameters);
        credentialJson.setPublicKey("ssh key");
        assertEquals(underTest.isValid(credentialJson, constraintValidatorContext), false);
    }

    @Test
    public void validAzureCredentialJsonWillReturnTrue() {
        CredentialRequest credentialJson = new CredentialRequest();
        credentialJson.setCloudPlatform(CloudPlatform.AZURE);
        credentialJson.setDescription("description");
        credentialJson.setName("name");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(RequiredAzureCredentialParam.SUBSCRIPTION_ID.getName(), "id");
        credentialJson.setParameters(parameters);
        credentialJson.setPublicKey("ssh key");
        assertEquals(underTest.isValid(credentialJson, constraintValidatorContext), true);
    }

    @Test
    public void validAzureCredentialJsonSubscriptionIdMissingValidationFails() {
        CredentialRequest credentialJson = new CredentialRequest();
        credentialJson.setCloudPlatform(CloudPlatform.AZURE);
        credentialJson.setDescription("description");
        credentialJson.setName("name");
        Map<String, Object> parameters = new HashMap<>();
        credentialJson.setParameters(parameters);
        credentialJson.setPublicKey("ssh key");
        assertEquals(underTest.isValid(credentialJson, constraintValidatorContext), false);
    }

    @Test
    public void validGcpCredentialJsonWillReturnTrue() {
        CredentialRequest credentialJson = new CredentialRequest();
        credentialJson.setCloudPlatform(CloudPlatform.GCP);
        credentialJson.setDescription("description");
        credentialJson.setName("name");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(GcpCredentialParam.PROJECTID.getName(), "projectid");
        parameters.put(GcpCredentialParam.SERVICE_ACCOUNT_ID.getName(), "accountid");
        parameters.put(GcpCredentialParam.SERVICE_ACCOUNT_PRIVATE_KEY.getName(), "privatekey");
        credentialJson.setParameters(parameters);
        credentialJson.setPublicKey("ssh key");
        assertEquals(underTest.isValid(credentialJson, constraintValidatorContext), true);
    }

    @Test
    public void invalidGcpCredentialJsonProjectIdMissingValidationFails() {
        CredentialRequest credentialJson = new CredentialRequest();
        credentialJson.setCloudPlatform(CloudPlatform.GCP);
        credentialJson.setDescription("description");
        credentialJson.setName("name");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(GcpCredentialParam.SERVICE_ACCOUNT_ID.getName(), "accountid");
        parameters.put(GcpCredentialParam.SERVICE_ACCOUNT_PRIVATE_KEY.getName(), "privatekey");
        credentialJson.setParameters(parameters);
        credentialJson.setPublicKey("ssh key");
        assertEquals(underTest.isValid(credentialJson, constraintValidatorContext), false);
    }

    @Test
    public void inValidGcpCredentialJsonAccountIdMissingValidationFails() {
        CredentialRequest credentialJson = new CredentialRequest();
        credentialJson.setCloudPlatform(CloudPlatform.GCP);
        credentialJson.setDescription("description");
        credentialJson.setName("name");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(GcpCredentialParam.PROJECTID.getName(), "projectid");
        parameters.put(GcpCredentialParam.SERVICE_ACCOUNT_PRIVATE_KEY.getName(), "privatekey");
        credentialJson.setParameters(parameters);
        credentialJson.setPublicKey("ssh key");
        assertEquals(underTest.isValid(credentialJson, constraintValidatorContext), false);
    }

    @Test
    public void inValidGcpCredentialJsonServiceAccountPrivateKeyMissingValidationFails() {
        CredentialRequest credentialJson = new CredentialRequest();
        credentialJson.setCloudPlatform(CloudPlatform.GCP);
        credentialJson.setDescription("description");
        credentialJson.setName("name");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(GcpCredentialParam.SERVICE_ACCOUNT_ID.getName(), "accountid");
        parameters.put(GcpCredentialParam.PROJECTID.getName(), "projectid");
        credentialJson.setParameters(parameters);
        credentialJson.setPublicKey("ssh key");
        assertEquals(underTest.isValid(credentialJson, constraintValidatorContext), false);
    }

    @Test
    public void inValidOpenStackCredentialJsonEndpointMissingValidationFails() {
        CredentialRequest credentialJson = new CredentialRequest();
        credentialJson.setCloudPlatform(CloudPlatform.OPENSTACK);
        credentialJson.setDescription("description");
        credentialJson.setName("name");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(OpenStackCredentialParam.PASSWORD.getName(), "password");
        parameters.put(OpenStackCredentialParam.TENANT_NAME.getName(), "tenant");
        parameters.put(OpenStackCredentialParam.USER.getName(), "user");

        credentialJson.setParameters(parameters);
        credentialJson.setPublicKey("ssh key");
        assertEquals(underTest.isValid(credentialJson, constraintValidatorContext), false);
    }

    @Test
    public void inValidOpenStackCredentialJsonPasswordMissingValidationFails() {
        CredentialRequest credentialJson = new CredentialRequest();
        credentialJson.setCloudPlatform(CloudPlatform.OPENSTACK);
        credentialJson.setDescription("description");
        credentialJson.setName("name");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(OpenStackCredentialParam.ENDPOINT.getName(), "password");
        parameters.put(OpenStackCredentialParam.TENANT_NAME.getName(), "tenant");
        parameters.put(OpenStackCredentialParam.USER.getName(), "user");

        credentialJson.setParameters(parameters);
        credentialJson.setPublicKey("ssh key");
        assertEquals(underTest.isValid(credentialJson, constraintValidatorContext), false);
    }

    @Test
    public void inValidOpenStackCredentialJsonTenantNameMissingValidationFails() {
        CredentialRequest credentialJson = new CredentialRequest();
        credentialJson.setCloudPlatform(CloudPlatform.OPENSTACK);
        credentialJson.setDescription("description");
        credentialJson.setName("name");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(OpenStackCredentialParam.ENDPOINT.getName(), "password");
        parameters.put(OpenStackCredentialParam.PASSWORD.getName(), "tenant");
        parameters.put(OpenStackCredentialParam.USER.getName(), "user");

        credentialJson.setParameters(parameters);
        credentialJson.setPublicKey("ssh key");
        assertEquals(underTest.isValid(credentialJson, constraintValidatorContext), false);
    }

    @Test
    public void inValidOpenStackCredentialJsonUserMissingValidationFails() {
        CredentialRequest credentialJson = new CredentialRequest();
        credentialJson.setCloudPlatform(CloudPlatform.OPENSTACK);
        credentialJson.setDescription("description");
        credentialJson.setName("name");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(OpenStackCredentialParam.ENDPOINT.getName(), "password");
        parameters.put(OpenStackCredentialParam.PASSWORD.getName(), "tenant");
        parameters.put(OpenStackCredentialParam.TENANT_NAME.getName(), "user");

        credentialJson.setParameters(parameters);
        credentialJson.setPublicKey("ssh key");
        assertEquals(underTest.isValid(credentialJson, constraintValidatorContext), false);
    }

    @Test
    public void validOpenStackCredentialJsonSuccesValidation() {
        CredentialRequest credentialJson = new CredentialRequest();
        credentialJson.setCloudPlatform(CloudPlatform.OPENSTACK);
        credentialJson.setDescription("description");
        credentialJson.setName("name");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(OpenStackCredentialParam.ENDPOINT.getName(), "password");
        parameters.put(OpenStackCredentialParam.USER.getName(), "user");
        parameters.put(OpenStackCredentialParam.PASSWORD.getName(), "tenant");
        parameters.put(OpenStackCredentialParam.TENANT_NAME.getName(), "user");

        credentialJson.setParameters(parameters);
        credentialJson.setPublicKey("ssh key");
        assertEquals(underTest.isValid(credentialJson, constraintValidatorContext), true);
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

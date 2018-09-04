package com.sequenceiq.cloudbreak.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

import javax.validation.ConstraintValidatorContext;

import org.junit.Test;

import com.sequenceiq.cloudbreak.api.model.CredentialRequest;

/**
 * Created by gpapp on 6/14/18.
 */
public class CredentialRequestValidatorTest {

    private static final String CREDENTIAL_REQUEST_KEYSTONE_VERSION_KEY = "keystoneVersion";

    private static final String KEYSTONE_V2 = "cb-keystone-v2";

    private static final String KEYSTONE_V3 = "cb-keystone-v3";

    private static final String CREDENTIAL_REQUEST_KEYSTONE_SCOPE_KEY = "keystoneAuthScope";

    private static final String KEYSTONE_V3_DEFAULT_SCOPE = "cb-keystone-v3-default-scope";

    private static final String KEYSTONE_V3_PROJECT_SCOPE = "cb-keystone-v3-project-scope";

    private static final String KEYSTONE_V3_DOMAIN_SCOPE = "cb-keystone-v3-domain-scope";

    private static final String AWS = "AWS";

    private static final String AZURE = "AZURE";

    private static final String GCP = "GCP";

    private static final String OPENSTACK = "OPENSTACK";

    private final CredentialRequestValidator underTest = new CredentialRequestValidator();

    private final ConstraintValidatorContext context = ContextMockUtil.createContextMock();

    @Test
    public void testCloudProvidersThatAreValid() {
        String baseErrorMessage = "Cloud provider credential requests that should be valid: ";
        StringBuilder failedConditions = new StringBuilder(baseErrorMessage);

        addIfFalse(AWS, underTest.isValid(new CredentialRequestBuilder().withCloudPlatform(AWS).build(), context), failedConditions);
        addIfFalse(AZURE, underTest.isValid(new CredentialRequestBuilder().withCloudPlatform(AZURE).build(), context), failedConditions);
        addIfFalse(GCP, underTest.isValid(new CredentialRequestBuilder().withCloudPlatform(GCP).build(), context), failedConditions);

        assertEquals(baseErrorMessage, failedConditions.toString());
    }

    @Test
    public void testOpenStackV2CredentialIsValid() {
        assertTrue(underTest.isValid(new OpenStackCredentialBuilder().withKeystoneVersionV2().build(), context));
    }

    @Test
    public void testOpenStackV3NotDefaultScopeCredentialsAreValid() {
        String baseErrorMessage = "Openstack Keystone V3 scopes that should be valid: ";
        StringBuilder failedConditions = new StringBuilder(baseErrorMessage);

        addIfFalse(KEYSTONE_V3_PROJECT_SCOPE, underTest.isValid(new OpenStackV3CredentialBuilder().withProjectScope().build(), context), failedConditions);
        addIfFalse(KEYSTONE_V3_DOMAIN_SCOPE, underTest.isValid(new OpenStackV3CredentialBuilder().withDomainScope().build(), context), failedConditions);

        assertEquals(baseErrorMessage, failedConditions.toString());
    }

    @Test
    public void testOpenStackV3DefaultScopeCredentialIsNotValid() {
        assertFalse(underTest.isValid(new OpenStackV3CredentialBuilder().withDefaultScope().build(), context));
        verify(context).buildConstraintViolationWithTemplate("OpenStack Keystone V3 credentials with default scope cannot be created");
    }

    private void addIfFalse(String testCondition, boolean result, StringBuilder failingConditions) {
        if (!result) {
            failingConditions.append(testCondition).append(", ");
        }
    }

    private static class CredentialRequestBuilder {
        private final CredentialRequest request = new CredentialRequest();

        CredentialRequestBuilder withCloudPlatform(String platform) {
            request.setCloudPlatform(platform);
            return this;
        }

        CredentialRequest build() {
            return request;
        }

        protected CredentialRequest getRequest() {
            return request;
        }
    }

    private static class OpenStackCredentialBuilder extends CredentialRequestBuilder {

        OpenStackCredentialBuilder() {
            withCloudPlatform(OPENSTACK);
        }

        OpenStackCredentialBuilder withKeystoneVersionV2() {
            getRequest().getParameters().put(CREDENTIAL_REQUEST_KEYSTONE_VERSION_KEY, KEYSTONE_V2);
            return this;
        }

        void withKeystoneVersionV3() {
            getRequest().getParameters().put(CREDENTIAL_REQUEST_KEYSTONE_VERSION_KEY, KEYSTONE_V3);
        }
    }

    private static class OpenStackV3CredentialBuilder extends OpenStackCredentialBuilder {

        OpenStackV3CredentialBuilder() {
            withKeystoneVersionV3();
        }

        OpenStackCredentialBuilder withDefaultScope() {
            getRequest().getParameters().put(CREDENTIAL_REQUEST_KEYSTONE_SCOPE_KEY, KEYSTONE_V3_DEFAULT_SCOPE);
            return this;
        }

        OpenStackCredentialBuilder withProjectScope() {
            getRequest().getParameters().put(CREDENTIAL_REQUEST_KEYSTONE_SCOPE_KEY, KEYSTONE_V3_PROJECT_SCOPE);
            return this;
        }

        OpenStackCredentialBuilder withDomainScope() {
            getRequest().getParameters().put(CREDENTIAL_REQUEST_KEYSTONE_SCOPE_KEY, KEYSTONE_V3_DOMAIN_SCOPE);
            return this;
        }
    }
}

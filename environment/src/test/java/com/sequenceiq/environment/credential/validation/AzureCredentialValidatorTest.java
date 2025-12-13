package com.sequenceiq.environment.credential.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.credential.AppAuthenticationType;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AppBasedRequest;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AzureCredentialRequestParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.RoleBasedRequest;
import com.sequenceiq.environment.api.v1.credential.model.request.CredentialRequest;
import com.sequenceiq.environment.credential.attributes.CredentialAttributes;
import com.sequenceiq.environment.credential.attributes.azure.AzureCredentialAttributes;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.validation.aws.AzureCredentialValidator;

@ExtendWith(MockitoExtension.class)
public class AzureCredentialValidatorTest {
    @InjectMocks
    private AzureCredentialValidator underTest;

    @Test
    void testValidateCreateValidCertBased() {
        CredentialRequest credentialRequest = new CredentialRequest();
        AzureCredentialRequestParameters azureCredentialRequestParameters = new AzureCredentialRequestParameters();
        AppBasedRequest appBasedRequest = new AppBasedRequest();
        appBasedRequest.setAuthenticationType(AppAuthenticationType.CERTIFICATE);
        azureCredentialRequestParameters.setAppBased(appBasedRequest);
        credentialRequest.setAzure(azureCredentialRequestParameters);
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        ValidationResult validationResult = underTest.validateCreate(credentialRequest, validationResultBuilder);
        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateCreateValidSecretBased() {
        CredentialRequest credentialRequest = new CredentialRequest();
        AzureCredentialRequestParameters azureCredentialRequestParameters = new AzureCredentialRequestParameters();
        azureCredentialRequestParameters.setSubscriptionId("subscription");
        azureCredentialRequestParameters.setTenantId("tenant");
        AppBasedRequest appBasedRequest = new AppBasedRequest();
        appBasedRequest.setAuthenticationType(AppAuthenticationType.SECRET);
        azureCredentialRequestParameters.setAppBased(appBasedRequest);
        credentialRequest.setAzure(azureCredentialRequestParameters);
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        ValidationResult validationResult = underTest.validateCreate(credentialRequest, validationResultBuilder);
        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateCreateInvalidSecretBased() {
        CredentialRequest credentialRequest = new CredentialRequest();
        AzureCredentialRequestParameters azureCredentialRequestParameters = new AzureCredentialRequestParameters();
        AppBasedRequest appBasedRequest = new AppBasedRequest();
        appBasedRequest.setAuthenticationType(AppAuthenticationType.SECRET);
        azureCredentialRequestParameters.setAppBased(appBasedRequest);
        credentialRequest.setAzure(azureCredentialRequestParameters);
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        ValidationResult validationResult = underTest.validateCreate(credentialRequest, validationResultBuilder);
        assertTrue(validationResult.hasError());
        assertEquals(validationResult.getErrors().size(), 2);
        assertTrue(validationResult.getErrors().get(0).startsWith("subscriptionId is mandatory for"));
        assertTrue(validationResult.getErrors().get(1).startsWith("tenantId is mandatory for"));
    }

    @Test
    void testValidateCreateWhenAppBasedAndRoleBasedAreNull() {
        CredentialRequest credentialRequest = new CredentialRequest();
        AzureCredentialRequestParameters azureCredentialRequestParameters = new AzureCredentialRequestParameters();
        credentialRequest.setAzure(azureCredentialRequestParameters);
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        ValidationResult validationResult = underTest.validateCreate(credentialRequest, validationResultBuilder);
        assertTrue(validationResult.hasError());
        assertEquals(validationResult.getErrors().size(), 1);
        assertEquals(validationResult.getErrors().get(0), "appBaseRequest or roleBasedRequest have to be defined in azure specific parameters");
    }

    @Test
    void testValidateCreateInvalidRoleBased() {
        CredentialRequest credentialRequest = new CredentialRequest();
        AzureCredentialRequestParameters azureCredentialRequestParameters = new AzureCredentialRequestParameters();
        RoleBasedRequest roleBasedRequest = new RoleBasedRequest();
        azureCredentialRequestParameters.setRoleBased(roleBasedRequest);
        credentialRequest.setAzure(azureCredentialRequestParameters);
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        ValidationResult validationResult = underTest.validateCreate(credentialRequest, validationResultBuilder);
        assertTrue(validationResult.hasError());
        assertEquals(validationResult.getErrors().size(), 2);
        assertTrue(validationResult.getErrors().get(0).startsWith("subscriptionId is mandatory for"));
        assertTrue(validationResult.getErrors().get(1).startsWith("tenantId is mandatory for"));
    }

    @Test
    void testValidateCreateWhenAzureParamsAreMissing() {
        CredentialRequest credentialRequest = new CredentialRequest();
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        ValidationResult validationResult = underTest.validateCreate(credentialRequest, validationResultBuilder);
        assertTrue(validationResult.hasError());
        assertEquals(validationResult.getErrors().size(), 1);
        assertEquals(validationResult.getErrors().get(0), "Azure specific parameters are missing from the credential creation request");
    }

    @Test
    void testValidateUpdateValidAppBased() throws Exception {
        Credential origCred = new Credential();
        Credential newCred = new Credential();
        CredentialAttributes credentialAttributes = new CredentialAttributes();
        AzureCredentialAttributes azureCredentialAttributes = new AzureCredentialAttributes();
        azureCredentialAttributes.setSubscriptionId("subscription");
        azureCredentialAttributes.setTenantId("tenant");
        credentialAttributes.setAzure(azureCredentialAttributes);
        newCred.setAttributes(JsonUtil.writeValueAsString(credentialAttributes));
        newCred.setAttributes(new Json(credentialAttributes).getValue());
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        ValidationResult validationResult = underTest.validateUpdate(origCred, newCred, validationResultBuilder);
        assertFalse(validationResult.hasError());
    }

    @Test
    void testValidateUpdateWhenCredentialAttributesAreMissing() {
        Credential origCred = new Credential();
        Credential newCred = new Credential();
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        ValidationResult validationResult = underTest.validateUpdate(origCred, newCred, validationResultBuilder);
        assertTrue(validationResult.hasError());
        assertEquals(validationResult.getErrors().size(), 1);
        assertEquals(validationResult.getErrors().get(0), "Credential attributes are missing from the credential modification request");
    }

    @Test
    void testValidateUpdateWhenCredentialAttributesJsonIsIllegal() {
        Credential origCred = new Credential();
        Credential newCred = new Credential();
        newCred.setAttributes("not a json");
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        ValidationResult validationResult = underTest.validateUpdate(origCred, newCred, validationResultBuilder);
        assertTrue(validationResult.hasError());
        assertEquals(validationResult.getErrors().size(), 1);
        assertTrue(validationResult.getErrors().get(0).startsWith("Provider specific attributes cannot be read"));
    }

    @Test
    void testValidateUpdateMissingIds() throws Exception {
        Credential origCred = new Credential();
        Credential newCred = new Credential();
        CredentialAttributes credentialAttributes = new CredentialAttributes();
        AzureCredentialAttributes azureCredentialAttributes = new AzureCredentialAttributes();
        credentialAttributes.setAzure(azureCredentialAttributes);
        newCred.setAttributes(JsonUtil.writeValueAsString(credentialAttributes));
        newCred.setAttributes(new Json(credentialAttributes).getValue());
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        ValidationResult validationResult = underTest.validateUpdate(origCred, newCred, validationResultBuilder);
        assertTrue(validationResult.hasError());
        assertEquals(validationResult.getErrors().size(), 2);
        assertEquals(validationResult.getErrors().get(0), "subscriptionId is mandatory for azure credential modification");
        assertEquals(validationResult.getErrors().get(1), "tenantId is mandatory for azure credential modification");
    }

    @Test
    void testValidateUpdateWhenAzureAttributesAreMissing() throws Exception {
        Credential origCred = new Credential();
        Credential newCred = new Credential();
        CredentialAttributes credentialAttributes = new CredentialAttributes();
        newCred.setAttributes(JsonUtil.writeValueAsString(credentialAttributes));
        newCred.setAttributes(new Json(credentialAttributes).getValue());
        ValidationResult.ValidationResultBuilder validationResultBuilder = new ValidationResult.ValidationResultBuilder();
        ValidationResult validationResult = underTest.validateUpdate(origCred, newCred, validationResultBuilder);
        assertTrue(validationResult.hasError());
        assertEquals(validationResult.getErrors().size(), 1);
        assertEquals(validationResult.getErrors().get(0), "Azure specific parameters are missing from the credential modification request");
    }
}

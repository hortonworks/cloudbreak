package com.sequenceiq.cloudbreak.controller.validation.stack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.model.EncryptionKeyConfigJson;
import com.sequenceiq.cloudbreak.api.model.PlatformEncryptionKeysResponse;
import com.sequenceiq.cloudbreak.api.model.PlatformResourceRequestJson;
import com.sequenceiq.cloudbreak.api.model.TemplateRequest;
import com.sequenceiq.cloudbreak.api.model.stack.StackRequest;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupRequest;
import com.sequenceiq.cloudbreak.api.model.v2.template.EncryptionType;
import com.sequenceiq.cloudbreak.controller.PlatformParameterV1Controller;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.template.TemplateRequestValidator;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;

public class StackAwsEncryptionValidatorTest {

    private static final String TYPE = "type";

    private static final String KEY = "key";

    private static final String TEST_ENCRYPTION_KEY = "arn:aws:kms:eu-west-2:123456789012:key/1a2b3c4d-5e6f-7g8h-9i0j-1k2l3m4n5o6p";

    private StackRequestValidator underTest;

    @Mock
    private PlatformParameterV1Controller parameterV1Controller;

    @Mock
    private CredentialService credentialService;

    @Mock
    private TemplateRequestValidator templateRequestValidator;

    @Mock
    private StackRequest subject;

    @Mock
    private TemplateRequest templateRequest;

    @Mock
    private InstanceGroupRequest instanceGroupRequest;

    private Map<String, Object> parameters;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        underTest = new StackRequestValidator(templateRequestValidator);
        underTest.setCredentialService(credentialService);
        underTest.setParameterV1Controller(parameterV1Controller);
        parameters = new LinkedHashMap<>();
        when(subject.getClusterToAttach()).thenReturn(null);
        when(templateRequestValidator.validate(any())).thenReturn(ValidationResult.builder().build());
        when(templateRequest.getParameters()).thenReturn(parameters);
    }

    @Test
    public void testValidateEncryptionKeyWhenTemplateParametersHasTypeKeyButItsTypeIsNotEncryptionTypeThenThereIsNoEncryptionKeyCheck() {
        parameters.put(TYPE, "some value which is definitely not an EncryptionType instance. in this case it's a string");
        when(subject.getInstanceGroups()).thenReturn(getInstanceGroupWithRequest(createRequestWithParameters(parameters)));

        ValidationResult result = underTest.validate(subject);

        assertTrue(result.getErrors().isEmpty());
        verify(credentialService, times(0)).get(anyString(), anyString());
        verify(parameterV1Controller, times(0)).getEncryptionKeys(any(PlatformResourceRequestJson.class));
    }

    @Test
    public void testValidateEncryptionKeyWhenTemplateParametersHasTypeKeyAndItsTypeIsEncryptionTypeWithDefaultValueThenThereIsNoEncryptionKeyCheck() {
        parameters.put(TYPE, EncryptionType.DEFAULT);
        when(subject.getInstanceGroups()).thenReturn(getInstanceGroupWithRequest(createRequestWithParameters(parameters)));

        ValidationResult result = underTest.validate(subject);

        assertTrue(result.getErrors().isEmpty());
        verify(credentialService, times(0)).get(anyString(), anyString());
        verify(parameterV1Controller, times(0)).getEncryptionKeys(any(PlatformResourceRequestJson.class));
    }

    @Test
    public void testValidateEncryptionKeyWhenTemplateParametersHasTypeKeyAndItsTypeIsEncryptionTypeWithNoneValueThenThereIsNoEncryptionKeyCheck() {
        parameters.put(TYPE, EncryptionType.NONE);
        when(subject.getInstanceGroups()).thenReturn(getInstanceGroupWithRequest(createRequestWithParameters(parameters)));

        ValidationResult result = underTest.validate(subject);

        assertTrue(result.getErrors().isEmpty());
        verify(credentialService, times(0)).get(anyString(), anyString());
        verify(parameterV1Controller, times(0)).getEncryptionKeys(any(PlatformResourceRequestJson.class));
    }

    @Test
    public void testValidateEncryptionKeyWhenEncryptionKeysCouldNotBeRetrievedThenThereIsNoEncryptionKeyCheck() {
        parameters.put(TYPE, EncryptionType.CUSTOM);
        when(subject.getInstanceGroups()).thenReturn(getInstanceGroupWithRequest(createRequestWithParameters(parameters)));
        when(credentialService.get(ArgumentMatchers.<String>any(), any())).thenReturn(new Credential());
        when(parameterV1Controller.getEncryptionKeys(any(PlatformResourceRequestJson.class))).thenReturn(null);

        ValidationResult result = underTest.validate(subject);

        assertTrue(result.getErrors().isEmpty());
        verify(credentialService, times(1)).get(ArgumentMatchers.<String>any(), any());
        verify(parameterV1Controller, times(1)).getEncryptionKeys(any(PlatformResourceRequestJson.class));
    }

    @Test
    public void testValidateEncryptionKeyWhenThereIsNoReturningEncryptionKeyFromControllerThenThereIsNoEncryptionKeyCheck() {
        parameters.put(TYPE, EncryptionType.CUSTOM);
        when(subject.getInstanceGroups()).thenReturn(getInstanceGroupWithRequest(createRequestWithParameters(parameters)));
        when(credentialService.get(ArgumentMatchers.<String>any(), any())).thenReturn(new Credential());
        when(parameterV1Controller.getEncryptionKeys(any(PlatformResourceRequestJson.class))).thenReturn(new PlatformEncryptionKeysResponse());

        ValidationResult result = underTest.validate(subject);

        assertTrue(result.getErrors().isEmpty());
        verify(credentialService, times(1)).get(ArgumentMatchers.<String>any(), any());
        verify(parameterV1Controller, times(1)).getEncryptionKeys(any(PlatformResourceRequestJson.class));
    }

    @Test
    public void testValidateEncryptionKeyWhenEncryptionKeysAreExistsButDoesNotContainsKeyEntryThenValidationErrorShouldComeBack() {
        parameters.put(TYPE, EncryptionType.CUSTOM);
        PlatformEncryptionKeysResponse encryptionKeysResponse = createPlatformEncryptionKeysResponseWithoutNameValue();
        when(subject.getInstanceGroups()).thenReturn(getInstanceGroupWithRequest(createRequestWithParameters(parameters)));
        when(credentialService.get(ArgumentMatchers.<String>any(), any())).thenReturn(new Credential());
        when(parameterV1Controller.getEncryptionKeys(any(PlatformResourceRequestJson.class))).thenReturn(encryptionKeysResponse);

        ValidationResult result = underTest.validate(subject);

        assertFalse(result.getErrors().isEmpty());
        assertEquals(1, result.getErrors().size());
        assertEquals("There is no encryption key provided but CUSTOM type is given for encryption.", result.getErrors().get(0));
        verify(credentialService, times(1)).get(ArgumentMatchers.<String>any(), any());
        verify(parameterV1Controller, times(1)).getEncryptionKeys(any(PlatformResourceRequestJson.class));
    }

    @Test
    public void testValidateEncryptionKeyWhenEncryptionKeysAreExistsAndContainsKeyEntryButItsValueIsNotInTheListedKeysThenValidationErrorShouldComeBack() {
        parameters.put(TYPE, EncryptionType.CUSTOM);
        parameters.put(KEY, "some invalid value which does not exists in the listed encryption keys");
        PlatformEncryptionKeysResponse encryptionKeysResponse = createPlatformEncryptionKeysResponseWithoutNameValue();
        when(subject.getInstanceGroups()).thenReturn(getInstanceGroupWithRequest(createRequestWithParameters(parameters)));
        when(credentialService.get(ArgumentMatchers.<String>any(), any())).thenReturn(new Credential());
        when(parameterV1Controller.getEncryptionKeys(any(PlatformResourceRequestJson.class))).thenReturn(encryptionKeysResponse);

        ValidationResult result = underTest.validate(subject);

        assertFalse(result.getErrors().isEmpty());
        assertEquals(1, result.getErrors().size());
        assertEquals("The provided encryption key does not exists in the given region's encryption key list for this credential.", result.getErrors().get(0));
        verify(credentialService, times(1)).get(ArgumentMatchers.<String>any(), any());
        verify(parameterV1Controller, times(1)).getEncryptionKeys(any(PlatformResourceRequestJson.class));
    }

    @Test
    public void testValidateEncryptionKeyWhenEncryptionKeysAreExistsAndContainsKeyEntryAndItsValueIsInTheListedKeysThenEverythingShouldGoFine() {
        parameters.put(TYPE, EncryptionType.CUSTOM);
        parameters.put(KEY, TEST_ENCRYPTION_KEY);
        PlatformEncryptionKeysResponse encryptionKeysResponse = createPlatformEncryptionKeysResponseWithNameValue();
        when(subject.getInstanceGroups()).thenReturn(getInstanceGroupWithRequest(createRequestWithParameters(parameters)));
        when(credentialService.get(ArgumentMatchers.<String>any(), any())).thenReturn(new Credential());
        when(parameterV1Controller.getEncryptionKeys(any(PlatformResourceRequestJson.class))).thenReturn(encryptionKeysResponse);

        ValidationResult result = underTest.validate(subject);

        assertTrue(result.getErrors().isEmpty());
        verify(credentialService, times(1)).get(ArgumentMatchers.<String>any(), any());
        verify(parameterV1Controller, times(1)).getEncryptionKeys(any(PlatformResourceRequestJson.class));
    }

    private InstanceGroupRequest createRequestWithParameters(Map<String, Object> parameters) {
        InstanceGroupRequest request = new InstanceGroupRequest();
        TemplateRequest template = new TemplateRequest();
        template.setParameters(parameters);
        request.setTemplate(template);
        return request;
    }

    private List<InstanceGroupRequest> getInstanceGroupWithRequest(InstanceGroupRequest... requests) {
        return Arrays.asList(requests);
    }

    private PlatformEncryptionKeysResponse createPlatformEncryptionKeysResponseWithoutNameValue() {
        PlatformEncryptionKeysResponse encryptionKeysResponse = new PlatformEncryptionKeysResponse();
        EncryptionKeyConfigJson testInput = new EncryptionKeyConfigJson();
        encryptionKeysResponse.setEncryptionKeyConfigs(Set.of(testInput));
        return encryptionKeysResponse;
    }

    private PlatformEncryptionKeysResponse createPlatformEncryptionKeysResponseWithNameValue() {
        PlatformEncryptionKeysResponse encryptionKeysResponse = new PlatformEncryptionKeysResponse();
        EncryptionKeyConfigJson testInput = new EncryptionKeyConfigJson();
        testInput.setName(TEST_ENCRYPTION_KEY);
        encryptionKeysResponse.setEncryptionKeyConfigs(Set.of(testInput));
        return encryptionKeysResponse;
    }

}
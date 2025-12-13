package com.sequenceiq.cloudbreak.controller.validation.stack;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsEncryptionV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKey;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.controller.validation.template.InstanceTemplateValidator;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.environment.PlatformResourceClientService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.type.EncryptionType;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class StackAwsEncryptionValidatorTest extends StackRequestValidatorTestBase {

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:account:environment:env";

    private static final String TEST_ENCRYPTION_KEY = "arn:aws:kms:eu-west-2:123456789012:key/1a2b3c4d-5e6f-7g8h-9i0j-1k2l3m4n5o6p";

    @Mock
    private InstanceTemplateValidator templateRequestValidator;

    @Mock
    private PlatformResourceClientService platformResourceClientService;

    @Mock
    private EnvironmentService environmentClientService;

    @Mock
    private Stack subject;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private StackValidator underTest;

    StackAwsEncryptionValidatorTest() {
        super(LoggerFactory.getLogger(StackAwsEncryptionValidatorTest.class));
    }

    @BeforeEach
    void setup() {
        CredentialResponse credentialResponse = new CredentialResponse();
        credentialResponse.setName("cred");

        when(subject.getEnvironmentCrn()).thenReturn(ENV_CRN);
        lenient().when(subject.getRegion()).thenReturn("region");
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCredential(credentialResponse);
        environmentResponse.setCrn(ENV_CRN);
        when(environmentClientService.getByCrn(anyString())).thenReturn(environmentResponse);
        when(templateRequestValidator.validate(any())).thenReturn(ValidationResult.builder().build());
    }

    @Test
    void testValidateEncryptionKeyWhenTemplateParametersHasTypeKeyAndItsTypeIsEncryptionTypeWithDefaultValueThenThereIsNoEncryptionKeyCheck() {
        AwsInstanceTemplateV4Parameters parameters = new AwsInstanceTemplateV4Parameters();
        parameters.setEncryption(encryption(EncryptionType.DEFAULT, null));
        when(subject.getInstanceGroups()).thenReturn(getInstanceGroupWithRequest(createRequestWithParameters(parameters)));
        ValidationResult.ValidationResultBuilder builder = new ValidationResult.ValidationResultBuilder();

        underTest.validate(subject, builder);

        assertValidationErrorIsEmpty(builder.build().getErrors());
        verify(platformResourceClientService, times(0)).getEncryptionKeys(anyString(), anyString());
    }

    @Test
    void testValidateEncryptionKeyWhenTemplateParametersHasTypeKeyAndItsTypeIsEncryptionTypeWithNoneValueThenThereIsNoEncryptionKeyCheck() {
        AwsInstanceTemplateV4Parameters parameters = new AwsInstanceTemplateV4Parameters();
        parameters.setEncryption(encryption(EncryptionType.NONE, null));
        when(subject.getInstanceGroups()).thenReturn(getInstanceGroupWithRequest(createRequestWithParameters(parameters)));
        ValidationResult.ValidationResultBuilder builder = new ValidationResult.ValidationResultBuilder();

        underTest.validate(subject, builder);

        assertValidationErrorIsEmpty(builder.build().getErrors());
        verify(platformResourceClientService, times(0)).getEncryptionKeys(anyString(), anyString());
    }

    @Test
    void testValidateEncryptionKeyWhenEncryptionKeysCouldNotBeRetrievedThenThereIsNoEncryptionKeyCheck() {
        AwsInstanceTemplateV4Parameters parameters = new AwsInstanceTemplateV4Parameters();
        parameters.setEncryption(encryption(EncryptionType.CUSTOM, null));
        when(subject.getInstanceGroups()).thenReturn(getInstanceGroupWithRequest(createRequestWithParameters(parameters)));
        ValidationResult.ValidationResultBuilder builder = new ValidationResult.ValidationResultBuilder();

        underTest.validate(subject, builder);

        assertValidationErrorIsEmpty(builder.build().getErrors());
        verify(platformResourceClientService, times(1)).getEncryptionKeys(anyString(), anyString());
    }

    @Test
    void testValidateEncryptionKeyWhenThereIsNoReturningEncryptionKeyFromControllerThenThereIsNoEncryptionKeyCheck() {
        AwsInstanceTemplateV4Parameters parameters = new AwsInstanceTemplateV4Parameters();
        parameters.setEncryption(encryption(EncryptionType.CUSTOM, null));

        when(subject.getInstanceGroups()).thenReturn(getInstanceGroupWithRequest(createRequestWithParameters(parameters)));
        ValidationResult.ValidationResultBuilder builder = new ValidationResult.ValidationResultBuilder();

        underTest.validate(subject, builder);

        assertValidationErrorIsEmpty(builder.build().getErrors());
        verify(platformResourceClientService, times(1)).getEncryptionKeys(anyString(), anyString());
    }

    @Test
    void testValidateEncryptionKeyWhenEncryptionKeysExistAndContainsKeyEntryAndItsValueIsInTheListedKeysThenEverythingShouldGoFine() {
        AwsInstanceTemplateV4Parameters parameters = new AwsInstanceTemplateV4Parameters();
        parameters.setEncryption(encryption(EncryptionType.CUSTOM, TEST_ENCRYPTION_KEY));
        when(subject.getInstanceGroups()).thenReturn(getInstanceGroupWithRequest(createRequestWithParameters(parameters)));
        ValidationResult.ValidationResultBuilder builder = new ValidationResult.ValidationResultBuilder();

        underTest.validate(subject, builder);

        assertValidationErrorIsEmpty(builder.build().getErrors());
        verify(platformResourceClientService, times(1)).getEncryptionKeys(anyString(), anyString());
    }

    private InstanceGroup createRequestWithParameters(AwsInstanceTemplateV4Parameters parameters) {
        InstanceGroup request = new InstanceGroup();
        Template template = new Template();
        template.setAttributes(new Json(parameters));
        template.setCloudPlatform("AWS");
        request.setTemplate(template);
        return request;
    }

    private Set<InstanceGroup> getInstanceGroupWithRequest(InstanceGroup... requests) {
        return Sets.newHashSet(requests);
    }

    private AwsEncryptionV4Parameters encryption(EncryptionType type, String key) {
        AwsEncryptionV4Parameters encryption = new AwsEncryptionV4Parameters();
        encryption.setType(type);
        encryption.setKey(key);
        return encryption;
    }

    private CloudEncryptionKeys createPlatformEncryptionKeysResponseWithNameValue() {
        CloudEncryptionKey testInput = new CloudEncryptionKey();
        testInput.setName(TEST_ENCRYPTION_KEY);
        return new CloudEncryptionKeys(Set.of(testInput));
    }
}

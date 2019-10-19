package com.sequenceiq.cloudbreak.controller.validation.stack;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsEncryptionV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.EnvironmentSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.placement.PlacementSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKey;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.common.api.type.EncryptionType;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.template.InstanceTemplateV4RequestValidator;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.PlatformResourceRequest;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.environment.PlatformResourceClientService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@RunWith(MockitoJUnitRunner.class)
public class StackAwsEncryptionValidatorTest extends StackRequestValidatorTestBase {

    private static final String CREDENTIAL_NAME = "someCred";

    private static final String TEST_ENCRYPTION_KEY = "arn:aws:kms:eu-west-2:123456789012:key/1a2b3c4d-5e6f-7g8h-9i0j-1k2l3m4n5o6p";

    @Mock
    private InstanceTemplateV4RequestValidator templateRequestValidator;

    @Mock
    private StackV4Request subject;

    @Mock
    private InstanceTemplateV4Request templateRequest;

    @Mock
    private InstanceGroupV4Request instanceGroupRequest;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private ClusterV4Request clusterRequest;

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private Blueprint blueprint;

    @Mock
    private Json blueprintTags;

    @Mock
    private PlatformResourceRequest platformResourceRequest;

    @Mock
    private EnvironmentSettingsV4Request environmentSettingsRequest;

    @Mock
    private PlacementSettingsV4Request placementSettingsRequest;

    @Mock
    private PlatformResourceClientService platformResourceClientService;

    @Mock
    private EnvironmentClientService environmentClientService;

    @InjectMocks
    private StackV4RequestValidator underTest;

    @Mock
    private Credential credential;

    private CredentialResponse credentialResponse;

    public StackAwsEncryptionValidatorTest() {
        super(LoggerFactory.getLogger(StackAwsEncryptionValidatorTest.class));
    }

    @Before
    public void setup() {
        credentialResponse = new CredentialResponse();
        credentialResponse.setName(CREDENTIAL_NAME);
        when(templateRequestValidator.validate(any())).thenReturn(ValidationResult.builder().build());
        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(1L);
        when(blueprintService.getByNameForWorkspaceId(anyString(), anyLong())).thenReturn(blueprint);
        when(subject.getEnvironmentCrn()).thenReturn("envCrn");
        when(subject.getPlacement()).thenReturn(placementSettingsRequest);
        when(subject.getCluster()).thenReturn(clusterRequest);
        when(clusterRequest.getBlueprintName()).thenReturn("dummy");
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setCredential(credentialResponse);
        when(environmentClientService.getByName(anyString())).thenReturn(environmentResponse);
        when(environmentClientService.getByCrn(anyString())).thenReturn(environmentResponse);
    }

    @Test
    public void testValidateEncryptionKeyWhenTemplateParametersHasTypeKeyAndItsTypeIsEncryptionTypeWithDefaultValueThenThereIsNoEncryptionKeyCheck() {
        AwsInstanceTemplateV4Parameters parameters = new AwsInstanceTemplateV4Parameters();
        parameters.setEncryption(encryption(EncryptionType.DEFAULT, null));
        when(subject.getInstanceGroups()).thenReturn(getInstanceGroupWithRequest(createRequestWithParameters(parameters)));

        ValidationResult result = underTest.validate(subject);

        assertValidationErrorIsEmpty(result.getErrors());
        verify(platformResourceClientService, times(0)).getEncryptionKeys(anyString(), anyString());
    }

    @Test
    public void testValidateEncryptionKeyWhenTemplateParametersHasTypeKeyAndItsTypeIsEncryptionTypeWithNoneValueThenThereIsNoEncryptionKeyCheck() {
        AwsInstanceTemplateV4Parameters parameters = new AwsInstanceTemplateV4Parameters();
        parameters.setEncryption(encryption(EncryptionType.NONE, null));
        when(subject.getInstanceGroups()).thenReturn(getInstanceGroupWithRequest(createRequestWithParameters(parameters)));

        ValidationResult result = underTest.validate(subject);

        assertValidationErrorIsEmpty(result.getErrors());
        verify(platformResourceClientService, times(0)).getEncryptionKeys(anyString(), anyString());
    }

    @Test
    public void testValidateEncryptionKeyWhenEncryptionKeysCouldNotBeRetrievedThenThereIsNoEncryptionKeyCheck() {
        AwsInstanceTemplateV4Parameters parameters = new AwsInstanceTemplateV4Parameters();
        parameters.setEncryption(encryption(EncryptionType.CUSTOM, null));

        when(subject.getInstanceGroups()).thenReturn(getInstanceGroupWithRequest(createRequestWithParameters(parameters)));

        ValidationResult result = underTest.validate(subject);

        assertValidationErrorIsEmpty(result.getErrors());
        verify(platformResourceClientService, times(1)).getEncryptionKeys(CREDENTIAL_NAME, null);
    }

    @Test
    public void testValidateEncryptionKeyWhenThereIsNoReturningEncryptionKeyFromControllerThenThereIsNoEncryptionKeyCheck() {
        AwsInstanceTemplateV4Parameters parameters = new AwsInstanceTemplateV4Parameters();
        parameters.setEncryption(encryption(EncryptionType.CUSTOM, null));

        when(subject.getInstanceGroups()).thenReturn(getInstanceGroupWithRequest(createRequestWithParameters(parameters)));

        ValidationResult result = underTest.validate(subject);

        assertValidationErrorIsEmpty(result.getErrors());
        verify(platformResourceClientService, times(1)).getEncryptionKeys(CREDENTIAL_NAME, null);
    }

    @Test
    public void testValidateEncryptionKeyWhenEncryptionKeysExistAndContainsKeyEntryAndItsValueIsInTheListedKeysThenEverythingShouldGoFine() {
        AwsInstanceTemplateV4Parameters parameters = new AwsInstanceTemplateV4Parameters();
        parameters.setEncryption(encryption(EncryptionType.CUSTOM, TEST_ENCRYPTION_KEY));
        when(subject.getInstanceGroups()).thenReturn(getInstanceGroupWithRequest(createRequestWithParameters(parameters)));

        ValidationResult result = underTest.validate(subject);

        assertValidationErrorIsEmpty(result.getErrors());
        verify(platformResourceClientService, times(1)).getEncryptionKeys(CREDENTIAL_NAME, null);
    }

    private InstanceGroupV4Request createRequestWithParameters(AwsInstanceTemplateV4Parameters parameters) {
        InstanceGroupV4Request request = new InstanceGroupV4Request();
        InstanceTemplateV4Request template = new InstanceTemplateV4Request();
        template.setAws(parameters);
        request.setTemplate(template);
        return request;
    }

    private List<InstanceGroupV4Request> getInstanceGroupWithRequest(InstanceGroupV4Request... requests) {
        return Arrays.asList(requests);
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

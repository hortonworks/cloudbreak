package com.sequenceiq.cloudbreak.controller.validation.stack;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.EncryptionType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsEncryptionV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.template.AwsInstanceTemplateV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ambari.AmbariV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.EnvironmentSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.environment.placement.PlacementSettingsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKey;
import com.sequenceiq.cloudbreak.cloud.model.CloudEncryptionKeys;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.controller.validation.ValidationResult;
import com.sequenceiq.cloudbreak.controller.validation.template.InstanceTemplateV4RequestValidator;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.PlatformResourceRequest;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.cloudbreak.service.platform.PlatformParameterService;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.service.clusterdefinition.ClusterDefinitionService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@RunWith(MockitoJUnitRunner.class)
public class StackAwsEncryptionValidatorTest extends StackRequestValidatorTestBase {

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
    private AmbariV4Request ambariRequest;

    @Mock
    private ClusterDefinitionService clusterDefinitionService;

    @Mock
    private ClusterDefinition clusterDefinition;

    @Mock
    private Json blueprintTags;

    @Mock
    private PlatformResourceRequest platformResourceRequest;

    @Mock
    private EnvironmentSettingsV4Request environmentSettingsRequest;

    @Mock
    private PlacementSettingsV4Request placementSettingsRequest;

    @Mock
    private PlatformParameterService platformParameterService;

    @Mock
    private CredentialService credentialService;

    @Mock
    private EnvironmentService environmentService;

    @InjectMocks
    private StackV4RequestValidator underTest;

    @Mock
    private Credential credential;

    public StackAwsEncryptionValidatorTest() {
        super(LoggerFactory.getLogger(StackAwsEncryptionValidatorTest.class));
    }

    @Before
    public void setup() {
        when(templateRequestValidator.validate(any())).thenReturn(ValidationResult.builder().build());
        when(restRequestThreadLocalService.getRequestedWorkspaceId()).thenReturn(1L);
        when(clusterDefinitionService.getByNameForWorkspaceId(anyString(), anyLong())).thenReturn(clusterDefinition);
        when(subject.getEnvironment()).thenReturn(environmentSettingsRequest);
        when(subject.getPlacement()).thenReturn(placementSettingsRequest);
        when(subject.getCluster()).thenReturn(clusterRequest);
        when(clusterRequest.getAmbari()).thenReturn(ambariRequest);
        String credentialName = "someCred";
        when(ambariRequest.getBlueprintName()).thenReturn("dummy");
        when(credential.cloudPlatform()).thenReturn("AWS");
        when(credentialService.getByNameForWorkspaceId(any(), any())).thenReturn(credential);
        when(platformParameterService.getPlatformResourceRequest(anyLong(), anyString(), eq(null), eq(null), eq(null)))
                .thenReturn(platformResourceRequest);
        when(environmentSettingsRequest.getCredentialName()).thenReturn(credentialName);
        when(platformParameterService.getRegionsByCredential(any())).thenReturn(mock(CloudRegions.class));
    }

    @Test
    public void testValidateEncryptionKeyWhenTemplateParametersHasTypeKeyAndItsTypeIsEncryptionTypeWithDefaultValueThenThereIsNoEncryptionKeyCheck() {
        AwsInstanceTemplateV4Parameters parameters = new AwsInstanceTemplateV4Parameters();
        parameters.setEncryption(encryption(EncryptionType.DEFAULT, null));
        when(subject.getInstanceGroups()).thenReturn(getInstanceGroupWithRequest(createRequestWithParameters(parameters)));

        ValidationResult result = underTest.validate(subject);

        assertValidationErrorIsEmpty(result.getErrors());
        verify(platformParameterService, times(0)).getEncryptionKeys(platformResourceRequest);
    }

    @Test
    public void testValidateEncryptionKeyWhenTemplateParametersHasTypeKeyAndItsTypeIsEncryptionTypeWithNoneValueThenThereIsNoEncryptionKeyCheck() {
        AwsInstanceTemplateV4Parameters parameters = new AwsInstanceTemplateV4Parameters();
        parameters.setEncryption(encryption(EncryptionType.NONE, null));
        when(subject.getInstanceGroups()).thenReturn(getInstanceGroupWithRequest(createRequestWithParameters(parameters)));

        ValidationResult result = underTest.validate(subject);

        assertValidationErrorIsEmpty(result.getErrors());
        verify(platformParameterService, times(0)).getEncryptionKeys(platformResourceRequest);
    }

    @Test
    public void testValidateEncryptionKeyWhenEncryptionKeysCouldNotBeRetrievedThenThereIsNoEncryptionKeyCheck() {
        AwsInstanceTemplateV4Parameters parameters = new AwsInstanceTemplateV4Parameters();
        parameters.setEncryption(encryption(EncryptionType.CUSTOM, null));

        when(subject.getInstanceGroups()).thenReturn(getInstanceGroupWithRequest(createRequestWithParameters(parameters)));
        when(platformParameterService.getEncryptionKeys(platformResourceRequest)).thenReturn(null);

        ValidationResult result = underTest.validate(subject);

        assertValidationErrorIsEmpty(result.getErrors());
        verify(platformParameterService, times(1)).getEncryptionKeys(platformResourceRequest);
    }

    @Test
    public void testValidateEncryptionKeyWhenThereIsNoReturningEncryptionKeyFromControllerThenThereIsNoEncryptionKeyCheck() {
        AwsInstanceTemplateV4Parameters parameters = new AwsInstanceTemplateV4Parameters();
        parameters.setEncryption(encryption(EncryptionType.CUSTOM, null));

        when(subject.getInstanceGroups()).thenReturn(getInstanceGroupWithRequest(createRequestWithParameters(parameters)));
        when(platformParameterService.getEncryptionKeys(platformResourceRequest)).thenReturn(new CloudEncryptionKeys(Collections.emptySet()));

        ValidationResult result = underTest.validate(subject);

        assertValidationErrorIsEmpty(result.getErrors());
        verify(platformParameterService, times(1)).getEncryptionKeys(platformResourceRequest);
    }

    @Test
    public void testValidateEncryptionKeyWhenEncryptionKeysAreExistsButDoesNotContainsKeyEntryThenValidationErrorShouldComeBack() {
        AwsInstanceTemplateV4Parameters parameters = new AwsInstanceTemplateV4Parameters();
        parameters.setEncryption(encryption(EncryptionType.CUSTOM, null));
        CloudEncryptionKeys encryptionKeysResponse = createPlatformEncryptionKeysResponseWithNameValue();

        when(subject.getInstanceGroups()).thenReturn(getInstanceGroupWithRequest(createRequestWithParameters(parameters)));
        when(platformParameterService.getEncryptionKeys(platformResourceRequest)).thenReturn(encryptionKeysResponse);

        ValidationResult result = underTest.validate(subject);

        assertFalse(result.getErrors().isEmpty());
        assertEquals(1, result.getErrors().size());
        assertEquals("There is no encryption key provided but CUSTOM type is given for encryption.", result.getErrors().get(0));
        verify(platformParameterService, times(1)).getEncryptionKeys(platformResourceRequest);
    }

    @Test
    public void testValidateEncryptionKeyWhenEncryptionKeysAreExistsAndContainsKeyEntryButItsValueIsNotInTheListedKeysThenValidationErrorShouldComeBack() {
        AwsInstanceTemplateV4Parameters parameters = new AwsInstanceTemplateV4Parameters();
        parameters.setEncryption(encryption(EncryptionType.CUSTOM, "some invalid value which does not exists in the listed encryption keys"));
        CloudEncryptionKeys encryptionKeysResponse = createPlatformEncryptionKeysResponseWithNameValue();

        when(subject.getInstanceGroups()).thenReturn(getInstanceGroupWithRequest(createRequestWithParameters(parameters)));
        when(platformParameterService.getEncryptionKeys(platformResourceRequest))
                .thenReturn(encryptionKeysResponse);

        ValidationResult result = underTest.validate(subject);

        assertFalse(result.getErrors().isEmpty());
        assertEquals(1, result.getErrors().size());
        assertEquals("The provided encryption key does not exists in the given region's encryption key list for this credential.", result.getErrors().get(0));
        verify(platformParameterService, times(1)).getEncryptionKeys(platformResourceRequest);
    }

    @Test
    public void testValidateEncryptionKeyWhenEncryptionKeysAreExistsAndContainsKeyEntryAndItsValueIsInTheListedKeysThenEverythingShouldGoFine() {
        AwsInstanceTemplateV4Parameters parameters = new AwsInstanceTemplateV4Parameters();
        parameters.setEncryption(encryption(EncryptionType.CUSTOM, TEST_ENCRYPTION_KEY));
        CloudEncryptionKeys encryptionKeysResponse = createPlatformEncryptionKeysResponseWithNameValue();
        when(subject.getInstanceGroups()).thenReturn(getInstanceGroupWithRequest(createRequestWithParameters(parameters)));
        when(platformParameterService.getEncryptionKeys(platformResourceRequest)).thenReturn(encryptionKeysResponse);

        ValidationResult result = underTest.validate(subject);

        assertValidationErrorIsEmpty(result.getErrors());
        verify(platformParameterService, times(1)).getEncryptionKeys(platformResourceRequest);
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
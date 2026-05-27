package com.sequenceiq.freeipa.service.stack.instance;

import static com.sequenceiq.common.model.Architecture.X86_64;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.instance.AwsInstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.instance.AzureInstanceGroupParameters;
import com.sequenceiq.cloudbreak.cloud.model.instance.AzureInstanceTemplate;
import com.sequenceiq.cloudbreak.common.converter.ResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;
import com.sequenceiq.common.api.type.EncryptionType;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.endpoint.service.azure.HostEncryptionCalculator;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.FreeIpaResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.LocationResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.AwsNetworkParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkRequest;
import com.sequenceiq.freeipa.entity.InstanceGroupNetwork;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.service.DefaultRootVolumeSizeProvider;

@ExtendWith(MockitoExtension.class)
class DefaultInstanceGroupProviderTest {

    private static final String ACCOUNT_ID = "accountId";

    private static final String STACK_NM = "stack";

    private static final String IG_NAME = "instanceGroup";

    private static final String INSTANCE_TYPE = "m5.2xlarge";

    private static final String AVAILABILITY_SET = "availabilitySet";

    @Mock
    private ResourceNameGenerator resourceNameGenerator;

    @Mock
    private DefaultRootVolumeSizeProvider defaultRootVolumeSizeProvider;

    @Mock
    private DefaultInstanceTypeProvider defaultInstanceTypeProvider;

    @Mock
    private DetailedEnvironmentResponse environmentResponse;

    @Mock
    private HostEncryptionCalculator hostEncryptionCalculator;

    @Mock
    private FreeIpaResponse freeIpaResponse;

    @InjectMocks
    private DefaultInstanceGroupProvider underTest;

    @Test
    void createDefaultTemplateTestNoVolumeEncryptionWhenAzure() {
        CredentialResponse credentialResponse = mock(CredentialResponse.class);
        when(credentialResponse.getCrn()).thenReturn("crn");
        LocationResponse locationResponse = mock(LocationResponse.class);
        when(locationResponse.getName()).thenReturn("region");
        when(environmentResponse.getLocation()).thenReturn(locationResponse);
        when(environmentResponse.getCredential()).thenReturn(credentialResponse);
        lenient().when(freeIpaResponse.getArchitecture()).thenReturn(X86_64.getName());
        lenient().when(environmentResponse.getFreeIpa()).thenReturn(freeIpaResponse);
        when(defaultInstanceTypeProvider.getForPlatform(anyString(), any(Platform.class), any(Region.class), any()))
                .thenReturn(List.of(INSTANCE_TYPE));
        Template result = underTest.createDefaultTemplate(environmentResponse, CloudPlatform.AZURE, ACCOUNT_ID, null, null, null, X86_64);

        assertThat(result).isNotNull();
        assertThat(result.getAttributes()).isNull();
    }

    @Test
    void createDefaultTemplateTestVolumeEncryptionAddedWhenAzure() {
        CredentialResponse credentialResponse = mock(CredentialResponse.class);
        when(credentialResponse.getCrn()).thenReturn("crn");
        LocationResponse locationResponse = mock(LocationResponse.class);
        when(locationResponse.getName()).thenReturn("region");
        when(environmentResponse.getLocation()).thenReturn(locationResponse);
        when(environmentResponse.getCredential()).thenReturn(credentialResponse);
        when(defaultInstanceTypeProvider.getForPlatform(anyString(), any(Platform.class), any(Region.class), any()))
                .thenReturn(List.of(INSTANCE_TYPE));
        Template result = underTest.createDefaultTemplate(environmentResponse, CloudPlatform.AZURE, ACCOUNT_ID, "dummyDiskEncryptionSet", null, null, X86_64);

        assertThat(result).isNotNull();
        Json attributes = result.getAttributes();
        assertThat(attributes).isNotNull();
        assertThat(attributes.getString(AzureInstanceTemplate.DISK_ENCRYPTION_SET_ID)).isEqualTo("dummyDiskEncryptionSet");
        assertThat(attributes.getBoolean(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED)).isEqualTo(Boolean.TRUE);
    }

    @Test
    void createDefaultTemplateTestVolumeEncryptionAddedWhenAzureAndNoDESAndEncryptionAtHostEnabled() {
        CredentialResponse credentialResponse = mock(CredentialResponse.class);
        when(credentialResponse.getCrn()).thenReturn("crn");
        LocationResponse locationResponse = mock(LocationResponse.class);
        when(locationResponse.getName()).thenReturn("region");
        when(environmentResponse.getLocation()).thenReturn(locationResponse);
        when(environmentResponse.getCredential()).thenReturn(credentialResponse);
        when(hostEncryptionCalculator.hostEncryptionRequired(any())).thenReturn(true);
        when(defaultInstanceTypeProvider.getForPlatform(anyString(), any(Platform.class), any(Region.class), any()))
                .thenReturn(List.of(INSTANCE_TYPE));
        Template result = underTest.createDefaultTemplate(environmentResponse, CloudPlatform.AZURE, ACCOUNT_ID, null, null, null, X86_64);

        assertThat(result).isNotNull();
        Json attributes = result.getAttributes();
        assertThat(attributes).isNotNull();
        assertThat(attributes.getBoolean(AzureInstanceTemplate.ENCRYPTION_AT_HOST_ENABLED)).isEqualTo(Boolean.TRUE);
    }

    @Test
    void createDefaultTemplateTestVolumeEncryptionAddedWhenAzureAndEncryptionAtHostEnabled() {
        CredentialResponse credentialResponse = mock(CredentialResponse.class);
        when(credentialResponse.getCrn()).thenReturn("crn");
        LocationResponse locationResponse = mock(LocationResponse.class);
        when(locationResponse.getName()).thenReturn("region");
        when(environmentResponse.getLocation()).thenReturn(locationResponse);
        when(environmentResponse.getCredential()).thenReturn(credentialResponse);
        when(hostEncryptionCalculator.hostEncryptionRequired(any())).thenReturn(true);
        when(defaultInstanceTypeProvider.getForPlatform(anyString(), any(Platform.class), any(Region.class), any()))
                .thenReturn(List.of(INSTANCE_TYPE));
        Template result = underTest.createDefaultTemplate(environmentResponse, CloudPlatform.AZURE, ACCOUNT_ID, "dummyDiskEncryptionSet", null, null, X86_64);

        assertThat(result).isNotNull();
        Json attributes = result.getAttributes();
        assertThat(attributes).isNotNull();
        assertThat(attributes.getString(AzureInstanceTemplate.DISK_ENCRYPTION_SET_ID)).isEqualTo("dummyDiskEncryptionSet");
        assertThat(attributes.getBoolean(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED)).isEqualTo(Boolean.TRUE);
        assertThat(attributes.getBoolean(AzureInstanceTemplate.ENCRYPTION_AT_HOST_ENABLED)).isEqualTo(Boolean.TRUE);
    }

    @Test
    void createDefaultTemplateTestDefaultVolumeEncryptionAddedWhenAwsCustomEncryptionKeyIsAbsent() {
        CredentialResponse credentialResponse = mock(CredentialResponse.class);
        when(credentialResponse.getCrn()).thenReturn("crn");
        LocationResponse locationResponse = mock(LocationResponse.class);
        when(locationResponse.getName()).thenReturn("region");
        when(environmentResponse.getLocation()).thenReturn(locationResponse);
        when(environmentResponse.getCredential()).thenReturn(credentialResponse);
        when(defaultInstanceTypeProvider.getForPlatform(anyString(), any(Platform.class), any(Region.class), any()))
                .thenReturn(List.of(INSTANCE_TYPE));
        Template result = underTest.createDefaultTemplate(environmentResponse, CloudPlatform.AWS, ACCOUNT_ID, null, null, null, X86_64);

        assertThat(result).isNotNull();
        Json attributes = result.getAttributes();
        assertThat(attributes).isNotNull();
        assertThat(attributes.getBoolean(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED)).isEqualTo(Boolean.TRUE);
        assertThat(attributes.getString(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE)).isEqualTo(EncryptionType.DEFAULT.name());
    }

    @Test
    void createDefaultTemplateTestCustomVolumeEncryptionWhenEncryptionIsPresent() {
        CredentialResponse credentialResponse = mock(CredentialResponse.class);
        when(credentialResponse.getCrn()).thenReturn("crn");
        LocationResponse locationResponse = mock(LocationResponse.class);
        when(locationResponse.getName()).thenReturn("region");
        when(environmentResponse.getLocation()).thenReturn(locationResponse);
        when(environmentResponse.getCredential()).thenReturn(credentialResponse);
        when(defaultInstanceTypeProvider.getForPlatform(anyString(), any(Platform.class), any(Region.class), any()))
                .thenReturn(List.of(INSTANCE_TYPE));
        Template result = underTest.createDefaultTemplate(environmentResponse, CloudPlatform.AWS, ACCOUNT_ID, null, null, "dummyAwsEncryptionKey", X86_64);

        assertThat(result).isNotNull();
        Json attributes = result.getAttributes();
        assertThat(attributes).isNotNull();
        assertThat(attributes.getBoolean(AwsInstanceTemplate.EBS_ENCRYPTION_ENABLED)).isEqualTo(Boolean.TRUE);
        assertThat(attributes.getString(InstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE)).isEqualTo(EncryptionType.CUSTOM.name());
    }

    @Test
    void createDefaultTemplateTestVolumeEncryptionAddedWhenGcp() {
        CredentialResponse credentialResponse = mock(CredentialResponse.class);
        when(credentialResponse.getCrn()).thenReturn("crn");
        LocationResponse locationResponse = mock(LocationResponse.class);
        when(locationResponse.getName()).thenReturn("region");
        when(environmentResponse.getLocation()).thenReturn(locationResponse);
        when(environmentResponse.getCredential()).thenReturn(credentialResponse);
        when(defaultInstanceTypeProvider.getForPlatform(anyString(), any(Platform.class), any(Region.class), any()))
                .thenReturn(List.of(INSTANCE_TYPE));
        Template result = underTest.createDefaultTemplate(environmentResponse, CloudPlatform.GCP, ACCOUNT_ID, null, "dummyEncryptionKey", null, X86_64);

        assertThat(result).isNotNull();
        Json attributes = result.getAttributes();
        assertThat(attributes).isNotNull();
        assertThat(attributes.getString(AzureInstanceTemplate.VOLUME_ENCRYPTION_KEY_ID)).isEqualTo("dummyEncryptionKey");
        assertThat(attributes.getString("keyEncryptionMethod")).isEqualTo("KMS");
    }

    @Test
    void createInstanceGroupAttributesTestAvailabilitySetsAddedWhenAzure() {
        Json attributes = underTest.createAttributes(CloudPlatform.AZURE, STACK_NM, IG_NAME);

        assertThat(attributes).isNotNull();
        JsonNode availabilitySet = attributes.getJsonNode(AVAILABILITY_SET);

        assertThat(availabilitySet.get(AzureInstanceGroupParameters.FAULT_DOMAIN_COUNT).asInt()).isEqualTo(2);
        assertThat(availabilitySet.get(AzureInstanceGroupParameters.UPDATE_DOMAIN_COUNT).asInt()).isEqualTo(20);
        assertThat(availabilitySet.get(AzureInstanceGroupParameters.NAME).asText()).isEqualTo(String.format("%s-%s-as-std", STACK_NM, IG_NAME));
    }

    @Test
    void createInstanceGroupAttributesTestEmptyWhenAws() {
        Json attributes = underTest.createAttributes(CloudPlatform.AWS, STACK_NM, IG_NAME);

        assertThat(attributes).isNull();
    }

    @Test
    void createDefaultNetworkWithAwsAttributesShouldReturnWithNetworkAttributes() {
        Json json = new Json(Map.of(NetworkConstants.SUBNET_IDS, Set.of("id")));
        NetworkRequest network = new NetworkRequest();
        AwsNetworkParameters awsNetworkParameters = new AwsNetworkParameters();
        awsNetworkParameters.setSubnetId("id");
        network.setAws(awsNetworkParameters);
        InstanceGroupNetwork defaultNetwork = underTest.createDefaultNetwork(CloudPlatform.AWS, network);

        assertThat(defaultNetwork.getAttributes()).isEqualTo(json);
    }

}
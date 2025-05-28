package com.sequenceiq.freeipa.converter.instance;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.MOCK;
import static com.sequenceiq.freeipa.util.CloudArgsForIgConverter.AWS_KMS_ENCRYPTION_KEY;
import static com.sequenceiq.freeipa.util.CloudArgsForIgConverter.DISK_ENCRYPTION_SET_ID;
import static com.sequenceiq.freeipa.util.CloudArgsForIgConverter.GCP_KMS_ENCRYPTION_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.instance.AwsInstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.model.instance.AzureInstanceTemplate;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.common.api.type.EncryptionType;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.FreeIpaServerRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceTemplateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.network.NetworkRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.security.SecurityGroupRequest;
import com.sequenceiq.freeipa.converter.instance.template.InstanceTemplateRequestToTemplateConverter;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.SecurityGroup;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.Template;
import com.sequenceiq.freeipa.service.multiaz.MultiAzCalculatorService;
import com.sequenceiq.freeipa.service.stack.instance.DefaultInstanceGroupProvider;
import com.sequenceiq.freeipa.util.CloudArgsForIgConverter;

@ExtendWith(MockitoExtension.class)
public class InstanceGroupRequestToInstanceGroupConverterTest {

    private static final String ACCOUNT_ID = "account_id";

    private static final String NAME = "NAME";

    private static final String HOSTNAME = "HOSTNAME";

    private static final String DOMAINNAME = "DOMAINNAME";

    @InjectMocks
    private InstanceGroupRequestToInstanceGroupConverter underTest;

    @Mock
    private InstanceTemplateRequestToTemplateConverter templateConverter;

    @Mock
    private SecurityGroupRequestToSecurityGroupConverter securityGroupConverter;

    @Mock
    private DefaultInstanceGroupProvider defaultInstanceGroupProvider;

    @Mock
    private MultiAzCalculatorService multiAzCalculatorService;

    @Test
    public void testConvertWithNullTemplate() {
        int nodeCount = 2;
        InstanceGroupRequest request = new InstanceGroupRequest();
        request.setName(NAME);
        request.setType(InstanceGroupType.MASTER);
        request.setNodeCount(nodeCount);

        Template template = mock(Template.class);
        SecurityGroupRequest securityGroupRequest = mock(SecurityGroupRequest.class);
        SecurityGroup securityGroup = mock(SecurityGroup.class);
        request.setSecurityGroup(securityGroupRequest);

        Stack stack = new Stack();
        stack.setAccountId(ACCOUNT_ID);
        stack.setCloudPlatform(MOCK.name());
        stack.setName(NAME);

        FreeIpaServerRequest freeIpaServerRequest = new FreeIpaServerRequest();
        freeIpaServerRequest.setHostname(HOSTNAME);
        freeIpaServerRequest.setDomain(DOMAINNAME);

        NetworkRequest networkRequest = new NetworkRequest();

        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();

        // GIVEN
        given(defaultInstanceGroupProvider.createDefaultTemplate(eq(detailedEnvironmentResponse), eq(MOCK), eq(ACCOUNT_ID),
                eq(null), eq(null), eq(null), any())).willReturn(template);
        given(securityGroupConverter.convert(eq(securityGroupRequest))).willReturn(securityGroup);
        // WHEN
        InstanceGroup result = underTest.convert(request, networkRequest, ACCOUNT_ID, stack, freeIpaServerRequest,
                detailedEnvironmentResponse, createAndGetCloudArgsForIgCoverterMap(null, null, null));

        // THEN
        assertThat(result).isNotNull();
        assertThat(result.getGroupName()).isEqualTo(NAME);
        assertThat(result.getInstanceGroupType()).isEqualTo(InstanceGroupType.MASTER);
        assertThat(result.getSecurityGroup()).isEqualTo(securityGroup);
        assertThat(result.getNodeCount()).isEqualTo(nodeCount);
        assertThat(result.getInstanceMetaData().size()).isEqualTo(nodeCount);
        int i = 0;
        for (InstanceMetaData instanceMetaData : result.getInstanceMetaData()) {
            assertThat(instanceMetaData.getInstanceGroup()).isEqualTo(result);
            assertThat(instanceMetaData.getDiscoveryFQDN()).startsWith(HOSTNAME + i);
            i++;
        }
        verify(multiAzCalculatorService, times(1)).populateAvailabilityZones(stack, detailedEnvironmentResponse, result);
    }

    @Test
    void convertTestTemplateConversion() {
        InstanceGroupRequest request = new InstanceGroupRequest();

        Stack stack = new Stack();
        stack.setAccountId(ACCOUNT_ID);
        stack.setCloudPlatform(MOCK.name());
        stack.setName(NAME);

        FreeIpaServerRequest freeIpaServerRequest = new FreeIpaServerRequest();
        freeIpaServerRequest.setHostname(HOSTNAME);
        freeIpaServerRequest.setDomain(DOMAINNAME);

        NetworkRequest networkRequest = new NetworkRequest();

        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();

        InstanceTemplateRequest instanceTemplateRequest = mock(InstanceTemplateRequest.class);
        request.setInstanceTemplateRequest(instanceTemplateRequest);
        Template template = mock(Template.class);
        when(templateConverter.convert(detailedEnvironmentResponse, instanceTemplateRequest, MOCK, ACCOUNT_ID,
                "dummyDiskEncryptionSetId", "encryptionKey", "awsEncryptionKeyArn", null)).thenReturn(template);

        InstanceGroup result = underTest.convert(
                request,
                networkRequest,
                ACCOUNT_ID,
                stack,
                freeIpaServerRequest,
                detailedEnvironmentResponse,
                createAndGetCloudArgsForIgCoverterMap("dummyDiskEncryptionSetId", "encryptionKey", "awsEncryptionKeyArn"));

        assertThat(result).isNotNull();
        assertThat(result.getTemplate()).isSameAs(template);
        verify(multiAzCalculatorService, times(1)).populateAvailabilityZones(stack, detailedEnvironmentResponse, result);
    }

    @Test
    void convertTestTemplateConversionWithDiskEncryptionSetId() {
        InstanceGroupRequest request = new InstanceGroupRequest();

        Stack stack = new Stack();
        stack.setAccountId(ACCOUNT_ID);
        stack.setCloudPlatform(AZURE.name());
        stack.setName(NAME);

        FreeIpaServerRequest freeIpaServerRequest = new FreeIpaServerRequest();
        freeIpaServerRequest.setHostname(HOSTNAME);
        freeIpaServerRequest.setDomain(DOMAINNAME);

        NetworkRequest networkRequest = new NetworkRequest();

        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();

        InstanceTemplateRequest instanceTemplateRequest = mock(InstanceTemplateRequest.class);
        request.setInstanceTemplateRequest(instanceTemplateRequest);
        Template template = new Template();
        Map<String, Object> json = new HashMap<>();
        json.put(AzureInstanceTemplate.DISK_ENCRYPTION_SET_ID, "dummyDiskEncryptionSetId");
        json.put(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED, Boolean.TRUE);
        template.setAttributes(new Json(json));
        when(templateConverter.convert(detailedEnvironmentResponse, instanceTemplateRequest, AZURE, ACCOUNT_ID,
                "dummyDiskEncryptionSetId", null, null, null)).thenReturn(template);

        InstanceGroup result = underTest.convert(
                request,
                networkRequest,
                ACCOUNT_ID,
                stack,
                freeIpaServerRequest,
                detailedEnvironmentResponse,
                createAndGetCloudArgsForIgCoverterMap("dummyDiskEncryptionSetId", null, null));

        assertThat(result).isNotNull();
        assertThat(result.getTemplate()).isSameAs(template);
        verify(multiAzCalculatorService, times(1)).populateAvailabilityZones(stack, detailedEnvironmentResponse, result);
    }

    @Test
    void convertTestDefaultTemplateConversionWithDiskEncryptionSetId() {
        InstanceGroupRequest request = new InstanceGroupRequest();

        Stack stack = new Stack();
        stack.setAccountId(ACCOUNT_ID);
        stack.setCloudPlatform(AZURE.name());
        stack.setName(NAME);

        FreeIpaServerRequest freeIpaServerRequest = new FreeIpaServerRequest();
        freeIpaServerRequest.setHostname(HOSTNAME);
        freeIpaServerRequest.setDomain(DOMAINNAME);

        NetworkRequest networkRequest = new NetworkRequest();

        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        Template template = new Template();
        Map<String, Object> json = new HashMap<>();
        json.put(AzureInstanceTemplate.DISK_ENCRYPTION_SET_ID, "dummyDiskEncryptionSetId");
        json.put(AzureInstanceTemplate.MANAGED_DISK_ENCRYPTION_WITH_CUSTOM_KEY_ENABLED, Boolean.TRUE);
        template.setAttributes(new Json(json));

        when(defaultInstanceGroupProvider.createDefaultTemplate(detailedEnvironmentResponse, AZURE, ACCOUNT_ID,
                "dummyDiskEncryptionSetId", null, null, null)).thenReturn(template);

        InstanceGroup result = underTest.convert(
                request,
                networkRequest,
                ACCOUNT_ID,
                stack,
                freeIpaServerRequest,
                detailedEnvironmentResponse,
                createAndGetCloudArgsForIgCoverterMap("dummyDiskEncryptionSetId", null, null));

        assertThat(result).isNotNull();
        assertThat(result.getTemplate()).isSameAs(template);
        verify(multiAzCalculatorService, times(1)).populateAvailabilityZones(stack, detailedEnvironmentResponse, result);
    }

    @Test
    void convertTestTemplateConversionWithGcpEncryptionKey() {
        InstanceGroupRequest request = new InstanceGroupRequest();

        InstanceTemplateRequest instanceTemplateRequest = mock(InstanceTemplateRequest.class);
        request.setInstanceTemplateRequest(instanceTemplateRequest);
        Template template = mock(Template.class);

        Stack stack = new Stack();
        stack.setAccountId(ACCOUNT_ID);
        stack.setCloudPlatform(MOCK.name());
        stack.setName(NAME);

        FreeIpaServerRequest freeIpaServerRequest = new FreeIpaServerRequest();
        freeIpaServerRequest.setHostname(HOSTNAME);
        freeIpaServerRequest.setDomain(DOMAINNAME);

        NetworkRequest networkRequest = new NetworkRequest();

        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();

        when(templateConverter.convert(detailedEnvironmentResponse, instanceTemplateRequest, MOCK, ACCOUNT_ID,
                null, "dummyEncryptionKey", null, null)).thenReturn(template);

        InstanceGroup result = underTest.convert(
                request,
                networkRequest,
                ACCOUNT_ID,
                stack,
                freeIpaServerRequest,
                detailedEnvironmentResponse,
                createAndGetCloudArgsForIgCoverterMap(null, "dummyEncryptionKey", null));

        assertThat(result).isNotNull();
        assertThat(result.getTemplate()).isSameAs(template);

        verify(multiAzCalculatorService, times(1)).populateAvailabilityZones(stack, detailedEnvironmentResponse, result);
    }

    @Test
    void convertTestDefaultTemplateConversionWithGcpEncryptionKey() {
        InstanceGroupRequest request = new InstanceGroupRequest();

        InstanceTemplateRequest instanceTemplateRequest = mock(InstanceTemplateRequest.class);
        request.setInstanceTemplateRequest(instanceTemplateRequest);
        Template template = mock(Template.class);

        Stack stack = new Stack();
        stack.setAccountId(ACCOUNT_ID);
        stack.setCloudPlatform(MOCK.name());
        stack.setName(NAME);

        FreeIpaServerRequest freeIpaServerRequest = new FreeIpaServerRequest();
        freeIpaServerRequest.setHostname(HOSTNAME);
        freeIpaServerRequest.setDomain(DOMAINNAME);

        NetworkRequest networkRequest = new NetworkRequest();

        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();

        when(templateConverter.convert(detailedEnvironmentResponse, instanceTemplateRequest, MOCK, ACCOUNT_ID,
                null, "dummyEncryptionKey", null, null)).thenReturn(template);

        InstanceGroup result = underTest.convert(
                request,
                networkRequest,
                ACCOUNT_ID,
                stack,
                freeIpaServerRequest,
                detailedEnvironmentResponse,
                createAndGetCloudArgsForIgCoverterMap(null, "dummyEncryptionKey", null));

        assertThat(result).isNotNull();
        assertThat(result.getTemplate()).isSameAs(template);

        verify(multiAzCalculatorService, times(1)).populateAvailabilityZones(stack, detailedEnvironmentResponse, result);
    }

    @Test
    void convertTestTemplateConversionWithAwsEncryptionKey() {
        InstanceGroupRequest request = new InstanceGroupRequest();

        Stack stack = new Stack();
        stack.setAccountId(ACCOUNT_ID);
        stack.setCloudPlatform(AWS.name());
        stack.setName(NAME);

        FreeIpaServerRequest freeIpaServerRequest = new FreeIpaServerRequest();
        freeIpaServerRequest.setHostname(HOSTNAME);
        freeIpaServerRequest.setDomain(DOMAINNAME);

        NetworkRequest networkRequest = new NetworkRequest();

        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();

        InstanceTemplateRequest instanceTemplateRequest = mock(InstanceTemplateRequest.class);
        request.setInstanceTemplateRequest(instanceTemplateRequest);
        Template template = new Template();
        Map<String, Object> json = new HashMap<>();
        json.put(AwsInstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, "dummyAwsDiskEncryptionKeyArn");
        json.put(AwsInstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.CUSTOM);
        template.setAttributes(new Json(json));
        when(templateConverter.convert(detailedEnvironmentResponse, instanceTemplateRequest, AWS, ACCOUNT_ID,
                null, null, "dummyAwsDiskEncryptionKeyArn", null)).thenReturn(template);

        InstanceGroup result = underTest.convert(
                request,
                networkRequest,
                ACCOUNT_ID,
                stack,
                freeIpaServerRequest,
                detailedEnvironmentResponse,
                createAndGetCloudArgsForIgCoverterMap(null, null, "dummyAwsDiskEncryptionKeyArn"));

        assertThat(result).isNotNull();
        assertThat(result.getTemplate()).isSameAs(template);

        verify(multiAzCalculatorService, times(1)).populateAvailabilityZones(stack, detailedEnvironmentResponse, result);

    }

    @Test
    void convertTestDefaultTemplateConversionWithAwsEncryptionKey() {
        InstanceGroupRequest request = new InstanceGroupRequest();

        Stack stack = new Stack();
        stack.setAccountId(ACCOUNT_ID);
        stack.setCloudPlatform(AWS.name());
        stack.setName(NAME);

        FreeIpaServerRequest freeIpaServerRequest = new FreeIpaServerRequest();
        freeIpaServerRequest.setHostname(HOSTNAME);
        freeIpaServerRequest.setDomain(DOMAINNAME);

        NetworkRequest networkRequest = new NetworkRequest();

        DetailedEnvironmentResponse detailedEnvironmentResponse = new DetailedEnvironmentResponse();
        Template template = new Template();
        Map<String, Object> json = new HashMap<>();
        json.put(AwsInstanceTemplate.VOLUME_ENCRYPTION_KEY_ID, "dummyAwsEncryptionKeyArn");
        json.put(AwsInstanceTemplate.VOLUME_ENCRYPTION_KEY_TYPE, EncryptionType.CUSTOM);
        template.setAttributes(new Json(json));

        when(defaultInstanceGroupProvider.createDefaultTemplate(detailedEnvironmentResponse, AWS, ACCOUNT_ID,
                null, null, "dummyAwsEncryptionKeyArn", null)).thenReturn(template);

        InstanceGroup result = underTest.convert(
                request,
                networkRequest,
                ACCOUNT_ID,
                stack,
                freeIpaServerRequest,
                detailedEnvironmentResponse,
                createAndGetCloudArgsForIgCoverterMap(null, null, "dummyAwsEncryptionKeyArn"));

        assertThat(result).isNotNull();
        assertThat(result.getTemplate()).isSameAs(template);

        verify(multiAzCalculatorService, times(1)).populateAvailabilityZones(stack, detailedEnvironmentResponse, result);
    }

    EnumMap<CloudArgsForIgConverter, String> createAndGetCloudArgsForIgCoverterMap(String diskEncryptionSetId,
            String encryptionKey, String awsEncryptionKeyArn) {
        EnumMap<CloudArgsForIgConverter, String> cloudArgsForIgConverterMap = new EnumMap<>(CloudArgsForIgConverter.class);

        cloudArgsForIgConverterMap.put(DISK_ENCRYPTION_SET_ID, diskEncryptionSetId);
        cloudArgsForIgConverterMap.put(GCP_KMS_ENCRYPTION_KEY, encryptionKey);
        cloudArgsForIgConverterMap.put(AWS_KMS_ENCRYPTION_KEY, awsEncryptionKeyArn);

        return cloudArgsForIgConverterMap;
    }
}
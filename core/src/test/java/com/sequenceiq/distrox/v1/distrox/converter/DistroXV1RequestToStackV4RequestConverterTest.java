package com.sequenceiq.distrox.v1.distrox.converter;

import static com.sequenceiq.cloudbreak.doc.ModelDescriptions.InstanceGroupModelDescription.INSTANCE_GROUP_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AzureNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.GcpNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RecoveryMode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.TagsV4Request;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetType;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.v4.stacks.TelemetryConverter;
import com.sequenceiq.cloudbreak.service.datalake.SdxClientService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAvailabilityType;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.InstanceGroupV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.instancegroup.template.InstanceTemplateV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.tags.TagsV1Request;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAzureParams;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkGcpParams;
import com.sequenceiq.environment.api.v1.environment.model.response.CompactRegionResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentNetworkResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;

@ExtendWith(MockitoExtension.class)
class DistroXV1RequestToStackV4RequestConverterTest {

    @Mock
    private DistroXAuthenticationToStaAuthenticationConverter authenticationConverter;

    @Mock
    private DistroXImageToImageSettingsConverter imageConverter;

    @Mock
    private DistroXClusterToClusterConverter clusterConverter;

    @Mock
    private InstanceGroupV1ToInstanceGroupV4Converter instanceGroupConverter;

    @Mock
    private NetworkV1ToNetworkV4Converter networkConverter;

    @Mock
    private DistroXParameterConverter stackParameterConverter;

    @Mock
    private EnvironmentClientService environmentClientService;

    @Mock
    private SdxConverter sdxConverter;

    @Mock
    private TelemetryConverter telemetryConverter;

    @Mock
    private SdxClientService sdxClientService;

    @Mock
    private DistroXDatabaseRequestToStackDatabaseRequestConverter databaseRequestConverter;

    @InjectMocks
    private DistroXV1RequestToStackV4RequestConverter underTest;

    @Test
    void convert() {
        when(environmentClientService.getByName(anyString())).thenReturn(createAwsEnvironment());
        when(networkConverter.convertToNetworkV4Request(any())).thenReturn(createAwsNetworkV4Request());
        when(databaseRequestConverter.convert(any(DistroXDatabaseRequest.class))).thenReturn(createDatabaseRequest());

        DistroXV1Request source = new DistroXV1Request();
        source.setEnvironmentName("envname");
        DistroXDatabaseRequest databaseRequest = new DistroXDatabaseRequest();
        databaseRequest.setAvailabilityType(DistroXDatabaseAvailabilityType.HA);
        source.setExternalDatabase(databaseRequest);
        StackV4Request convert = underTest.convert(source);
        assertThat(convert.getExternalDatabase()).isNotNull();
        assertThat(convert.getExternalDatabase().getAvailabilityType()).isEqualTo(DatabaseAvailabilityType.HA);
    }

    @Test
    void convertAvailabilityZoneComesFromEnv() {
        DistroXV1Request source = new DistroXV1Request();
        source.setEnvironmentName("envname");
        DistroXDatabaseRequest databaseRequest = new DistroXDatabaseRequest();
        databaseRequest.setAvailabilityType(DistroXDatabaseAvailabilityType.HA);
        source.setExternalDatabase(databaseRequest);

        DetailedEnvironmentResponse env = createAwsEnvironment();
        env.getNetwork().setSubnetMetas(Map.of("SubnetMeta", createCloudSubnet()));

        when(environmentClientService.getByName(source.getEnvironmentName())).thenReturn(env);
        when(networkConverter.convertToNetworkV4Request(any())).thenReturn(createAwsNetworkV4Request());
        when(databaseRequestConverter.convert(any(DistroXDatabaseRequest.class))).thenReturn(createDatabaseRequest());

        StackV4Request convert = underTest.convert(source);

        assertThat(convert.getExternalDatabase()).isNotNull();
        assertThat(convert.getExternalDatabase().getAvailabilityType()).isEqualTo(DatabaseAvailabilityType.HA);
    }

    @Test
    void convertAsTemplate() {
        when(environmentClientService.getByName(anyString())).thenReturn(createAwsEnvironment());
        when(networkConverter.convertToNetworkV4Request(any())).thenReturn(createAwsNetworkV4Request());
        when(databaseRequestConverter.convert(any(DistroXDatabaseRequest.class))).thenReturn(createDatabaseRequest());

        DistroXV1Request source = new DistroXV1Request();
        source.setEnvironmentName("envname");
        DistroXDatabaseRequest databaseRequest = new DistroXDatabaseRequest();
        databaseRequest.setAvailabilityType(DistroXDatabaseAvailabilityType.HA);
        source.setExternalDatabase(databaseRequest);

        StackV4Request convert = underTest.convertAsTemplate(source);

        assertThat(convert.getExternalDatabase()).isNotNull();
        assertThat(convert.getExternalDatabase().getAvailabilityType()).isEqualTo(DatabaseAvailabilityType.HA);
    }

    @Test
    void gatewayIGMustContain1Node() {
        when(environmentClientService.getByName(anyString())).thenReturn(createAwsEnvironment());
        when(networkConverter.convertToNetworkV4Request(any())).thenReturn(createAwsNetworkV4Request());
        when(databaseRequestConverter.convert(any(DistroXDatabaseRequest.class))).thenReturn(createDatabaseRequest());

        Set<InstanceGroupV1Request> instanceGroups1 = prepareInstanceGroupV1Requests(InstanceGroupType.CORE);
        Set<InstanceGroupV1Request> instanceGroups2 = prepareInstanceGroupV1Requests(InstanceGroupType.GATEWAY);


        DistroXV1Request source = new DistroXV1Request();
        source.setEnvironmentName("envname");
        DistroXDatabaseRequest databaseRequest = new DistroXDatabaseRequest();
        databaseRequest.setAvailabilityType(DistroXDatabaseAvailabilityType.HA);
        source.setExternalDatabase(databaseRequest);
        source.setInstanceGroups(instanceGroups1);
        source.setEnableLoadBalancer(true);
        underTest.convertAsTemplate(source);

        instanceGroups2.stream().findFirst().ifPresent(instanceGroup -> instanceGroup.setNodeCount(2));
        source.setInstanceGroups(instanceGroups2);

        underTest.convertAsTemplate(source);

        instanceGroups2.stream().findFirst().ifPresent(instanceGroup -> instanceGroup.setNodeCount(0));

        BadRequestException exception = Assertions.assertThrows(BadRequestException.class, () -> underTest.convertAsTemplate(source));
        assertEquals("Instance group with GATEWAY type must contain at least 1 node!", exception.getMessage());

        instanceGroups2.stream().findFirst().ifPresent(instanceGroup -> instanceGroup.setNodeCount(2));
        source.setEnableLoadBalancer(false);

        exception = Assertions.assertThrows(BadRequestException.class, () -> underTest.convertAsTemplate(source));
        assertEquals("Instance group with GATEWAY type must contain 1 node!", exception.getMessage());

        instanceGroups2.stream().findFirst().ifPresent(instanceGroup -> instanceGroup.setNodeCount(0));

        exception = Assertions.assertThrows(BadRequestException.class, () -> underTest.convertAsTemplate(source));
        assertEquals("Instance group with GATEWAY type must contain 1 node!", exception.getMessage());

        instanceGroups2.stream().findFirst().ifPresent(instanceGroup -> instanceGroup.setNodeCount(1));
        underTest.convertAsTemplate(source);
    }

    private Set<InstanceGroupV1Request> prepareInstanceGroupV1Requests(InstanceGroupType instanceGroupType) {
        InstanceGroupV1Request instanceGroup = new InstanceGroupV1Request();
        instanceGroup.setName(INSTANCE_GROUP_NAME);
        if (InstanceGroupType.GATEWAY.equals(instanceGroupType)) {
            instanceGroup.setNodeCount(1);
        } else {
            instanceGroup.setNodeCount(5);
        }
        instanceGroup.setRecoveryMode(RecoveryMode.AUTO);
        instanceGroup.setTemplate(new InstanceTemplateV1Request());
        instanceGroup.setType(instanceGroupType);
        return Set.of(instanceGroup);
    }

    @ParameterizedTest
    @EnumSource(EnvironmentStatus.class)
    void testStackV4RequestToDistroXV1RequestRegardlessOfTheStateOfTheEnvironment(EnvironmentStatus status) {
        StackV4Request source = new StackV4Request();
        source.setName("SomeStack");
        source.setEnvironmentCrn("SomeEnvCrn");

        DetailedEnvironmentResponse env = createAwsEnvironment();
        env.setCrn(source.getEnvironmentCrn());
        env.setEnvironmentStatus(status);

        when(environmentClientService.getByCrn(source.getEnvironmentCrn())).thenReturn(env);

        DistroXV1Request result = assertDoesNotThrow(() -> underTest.convert(source));

        verify(environmentClientService, times(1)).getByCrn(any());
        verify(environmentClientService, times(1)).getByCrn(source.getEnvironmentCrn());

        Assertions.assertEquals(env.getName(), result.getEnvironmentName());
    }

    @Test
    void testWhenEnvironmentStatusIsNotAvailableThenBadRequestExceptionComes() {
        DistroXV1Request source = new DistroXV1Request();
        source.setEnvironmentName("envname");
        DistroXDatabaseRequest databaseRequest = new DistroXDatabaseRequest();
        databaseRequest.setAvailabilityType(DistroXDatabaseAvailabilityType.HA);
        source.setExternalDatabase(databaseRequest);

        DetailedEnvironmentResponse env = createAwsEnvironment();
        env.setEnvironmentStatus(EnvironmentStatus.ENV_STOPPED);

        when(environmentClientService.getByName(source.getEnvironmentName())).thenReturn(env);

        assertThrows(BadRequestException.class, () -> underTest.convertAsTemplate(source));
    }

    @Test
    void convertAsTemplateForAzure() {
        DistroXV1Request source = new DistroXV1Request();
        source.setEnvironmentName("envname");
        DistroXDatabaseRequest databaseRequest = new DistroXDatabaseRequest();
        databaseRequest.setAvailabilityType(DistroXDatabaseAvailabilityType.HA);
        source.setExternalDatabase(databaseRequest);

        when(environmentClientService.getByName(source.getEnvironmentName())).thenReturn(createAzureEnvironment());
        when(networkConverter.convertToNetworkV4Request(any())).thenReturn(createAzureNetworkV4Request());
        when(databaseRequestConverter.convert(any(DistroXDatabaseRequest.class))).thenReturn(createDatabaseRequest());

        StackV4Request result = underTest.convertAsTemplate(source);

        assertNotNull(result.getExternalDatabase());
        assertEquals(DatabaseAvailabilityType.HA, result.getExternalDatabase().getAvailabilityType());
    }

    @Test
    void convertAsTemplateForGcp() {
        DistroXV1Request source = new DistroXV1Request();
        source.setEnvironmentName("envname");
        DistroXDatabaseRequest databaseRequest = new DistroXDatabaseRequest();
        databaseRequest.setAvailabilityType(DistroXDatabaseAvailabilityType.HA);
        source.setExternalDatabase(databaseRequest);

        when(environmentClientService.getByName(source.getEnvironmentName())).thenReturn(createGcpEnvironment());
        when(networkConverter.convertToNetworkV4Request(any())).thenReturn(createGcpNetworkV4Request());
        when(databaseRequestConverter.convert(any(DistroXDatabaseRequest.class))).thenReturn(createDatabaseRequest());

        StackV4Request result = underTest.convertAsTemplate(source);

        assertNotNull(result.getExternalDatabase());
        assertEquals(DatabaseAvailabilityType.HA, result.getExternalDatabase().getAvailabilityType());
    }

    @Test
    void testWhenTagsProvidedForDistroxConversionTheyWillBePassed() {
        DistroXV1Request source = new DistroXV1Request();
        source.setEnvironmentName("envname");
        DistroXDatabaseRequest databaseRequest = new DistroXDatabaseRequest();
        databaseRequest.setAvailabilityType(DistroXDatabaseAvailabilityType.HA);
        source.setExternalDatabase(databaseRequest);
        TagsV1Request tags = createTagsV1Request();
        source.setTags(tags);

        when(environmentClientService.getByName(source.getEnvironmentName())).thenReturn(createGcpEnvironment());
        when(networkConverter.convertToNetworkV4Request(any())).thenReturn(createGcpNetworkV4Request());
        when(databaseRequestConverter.convert(any(DistroXDatabaseRequest.class))).thenReturn(createDatabaseRequest());

        StackV4Request result = underTest.convertAsTemplate(source);

        assertNotNull(result.getExternalDatabase());
        assertEquals(DatabaseAvailabilityType.HA, result.getExternalDatabase().getAvailabilityType());
        checkTagsV1WithV4(tags, result.getTags());
    }

    @Test
    void testStackV4RequestToDistroXV1RequestWhenEnvironmentCrnIsNullThenNoCallHappensTowardsTheEnvironmentClientServiceAndNoEnvNameSetHappens() {
        StackV4Request source = new StackV4Request();
        source.setName("SomeStack");
        source.setEnvironmentCrn(null);

        DistroXV1Request result = assertDoesNotThrow(() -> underTest.convert(source));

        verify(environmentClientService, never()).getByCrn(any());

        Assertions.assertNull(result.getEnvironmentName());
    }

    @Test
    void convertStackRequest() {
        when(databaseRequestConverter.convert(any(DatabaseRequest.class))).thenReturn(createDistroXDatabaseRequest());

        StackV4Request source = new StackV4Request();
        source.setName("stackname");
        DatabaseRequest databaseRequest = new DatabaseRequest();
        databaseRequest.setAvailabilityType(DatabaseAvailabilityType.HA);
        source.setExternalDatabase(databaseRequest);
        DistroXV1Request convert = underTest.convert(source);
        assertThat(convert.getExternalDatabase()).isNotNull();
        assertThat(convert.getExternalDatabase().getAvailabilityType()).isEqualTo(DistroXDatabaseAvailabilityType.HA);
    }

    @Test
    void testWhenTagsProvidedTheyWillBePassedForStackConversion() {
        when(databaseRequestConverter.convert(any(DatabaseRequest.class))).thenReturn(createDistroXDatabaseRequest());

        StackV4Request source = new StackV4Request();
        source.setName("stackname");
        DatabaseRequest databaseRequest = new DatabaseRequest();
        databaseRequest.setAvailabilityType(DatabaseAvailabilityType.HA);
        source.setExternalDatabase(databaseRequest);
        TagsV4Request tags = createTagsV4Request();
        source.setTags(tags);

        DistroXV1Request result = underTest.convert(source);
        assertThat(result.getExternalDatabase()).isNotNull();
        assertThat(result.getExternalDatabase().getAvailabilityType()).isEqualTo(DistroXDatabaseAvailabilityType.HA);
        checkTagsV4WithV1(tags, result.getTags());
    }

    private void checkTagsV1WithV4(TagsV1Request input, TagsV4Request result) {
        assertEquals(input.getUserDefined().size(), result.getUserDefined().size());
        assertEquals(input.getApplication().size(), result.getApplication().size());
        assertEquals(input.getDefaults().size(), result.getDefaults().size());

        checkTagMap(input.getUserDefined(), result.getUserDefined());
        checkTagMap(input.getApplication(), result.getApplication());
        checkTagMap(input.getDefaults(), result.getDefaults());
    }

    private void checkTagsV4WithV1(TagsV4Request input, TagsV1Request result) {
        assertEquals(input.getUserDefined().size(), result.getUserDefined().size());
        assertEquals(input.getApplication().size(), result.getApplication().size());
        assertEquals(input.getDefaults().size(), result.getDefaults().size());

        checkTagMap(input.getUserDefined(), result.getUserDefined());
        checkTagMap(input.getApplication(), result.getApplication());
        checkTagMap(input.getDefaults(), result.getDefaults());
    }

    private void checkTagMap(Map<String, String> input, Map<String, String> result) {
        input.forEach((k, v) -> {
            assertTrue(result.containsKey(k));
            assertEquals(v, result.get(k));
        });
    }

    private DatabaseRequest createDatabaseRequest() {
        DatabaseRequest request = new DatabaseRequest();
        request.setAvailabilityType(DatabaseAvailabilityType.HA);
        return request;
    }

    private DistroXDatabaseRequest createDistroXDatabaseRequest() {
        DistroXDatabaseRequest request = new DistroXDatabaseRequest();
        request.setAvailabilityType(DistroXDatabaseAvailabilityType.HA);
        return request;
    }

    private NetworkV4Request createAwsNetworkV4Request() {
        NetworkV4Request network = new NetworkV4Request();
        network.setAws(createAwsNetworkV4Parameters());
        return network;
    }

    private NetworkV4Request createAzureNetworkV4Request() {
        NetworkV4Request network = new NetworkV4Request();
        network.setAzure(createAzureNetworkV4Parameters());
        return network;
    }

    private NetworkV4Request createGcpNetworkV4Request() {
        NetworkV4Request network = new NetworkV4Request();
        network.setGcp(createGcpNetworkV4Parameters());
        return network;
    }

    private TagsV1Request createTagsV1Request() {
        TagsV1Request r = new TagsV1Request();
        r.setUserDefined(Map.of("apple", "tree"));
        r.setDefaults(Map.of("default", "fruit"));
        r.setApplication(Map.of("peach", "tree"));
        return r;
    }

    private TagsV4Request createTagsV4Request() {
        TagsV4Request r = new TagsV4Request();
        r.setUserDefined(Map.of("apple", "tree"));
        r.setDefaults(Map.of("default", "fruit"));
        r.setApplication(Map.of("peach", "tree"));
        return r;
    }

    private AwsNetworkV4Parameters createAwsNetworkV4Parameters() {
        AwsNetworkV4Parameters awsNetwork = new AwsNetworkV4Parameters();
        awsNetwork.setSubnetId("mysubnetid");
        awsNetwork.setVpcId("myvpc");
        return awsNetwork;
    }

    private AzureNetworkV4Parameters createAzureNetworkV4Parameters() {
        AzureNetworkV4Parameters params = new AzureNetworkV4Parameters();
        params.setSubnetId("mysubnetid");
        params.setNetworkId("networkId");
        params.setNoPublicIp(false);
        params.setResourceGroupName("resourceGroup");
        return params;
    }

    private GcpNetworkV4Parameters createGcpNetworkV4Parameters() {
        GcpNetworkV4Parameters params = new GcpNetworkV4Parameters();
        params.setSubnetId("mysubnetid");
        params.setNoFirewallRules(true);
        params.setNoPublicIp(false);
        params.setSharedProjectId("projectId");
        return params;
    }

    private DetailedEnvironmentResponse createAzureEnvironment() {
        DetailedEnvironmentResponse env = new DetailedEnvironmentResponse();
        env.setEnvironmentStatus(EnvironmentStatus.AVAILABLE);
        env.setCloudPlatform("AZURE");
        env.setNetwork(createAzureNetwork());
        env.setName("SomeAwesomeEnv");
        env.setRegions(createCompactRegionResponse());
        return env;
    }

    private DetailedEnvironmentResponse createGcpEnvironment() {
        DetailedEnvironmentResponse env = new DetailedEnvironmentResponse();
        env.setEnvironmentStatus(EnvironmentStatus.AVAILABLE);
        env.setCloudPlatform("GCP");
        env.setNetwork(createGcpNetwork());
        env.setName("SomeAwesomeEnv");
        env.setRegions(createCompactRegionResponse());
        return env;
    }

    private DetailedEnvironmentResponse createAwsEnvironment() {
        DetailedEnvironmentResponse env = new DetailedEnvironmentResponse();
        env.setEnvironmentStatus(EnvironmentStatus.AVAILABLE);
        env.setCloudPlatform("AWS");
        env.setNetwork(createAwsNetwork());
        env.setName("SomeAwesomeEnv");
        env.setRegions(createCompactRegionResponse());
        return env;
    }

    private CompactRegionResponse createCompactRegionResponse() {
        CompactRegionResponse regionResponse = new CompactRegionResponse();
        regionResponse.setNames(List.of("myregion"));
        return regionResponse;
    }

    private EnvironmentNetworkResponse createAzureNetwork() {
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        network.setAzure(createAzureNetworkParams());
        network.setSubnetIds(Set.of("mysubnetid"));
        return network;
    }

    private EnvironmentNetworkAzureParams createAzureNetworkParams() {
        EnvironmentNetworkAzureParams azureParams = new EnvironmentNetworkAzureParams();
        azureParams.setNetworkId("someNetworkId");
        azureParams.setResourceGroupName("someResourceGroup");
        azureParams.setNoPublicIp(false);
        return azureParams;
    }

    private EnvironmentNetworkResponse createGcpNetwork() {
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        network.setGcp(createGcpNetworkParams());
        network.setSubnetIds(Set.of("mysubnetid"));
        return network;
    }

    private EnvironmentNetworkGcpParams createGcpNetworkParams() {
        EnvironmentNetworkGcpParams gcpParams = new EnvironmentNetworkGcpParams();
        gcpParams.setNetworkId("someNetworkId");
        gcpParams.setNoPublicIp(false);
        gcpParams.setNoFirewallRules(true);
        gcpParams.setSharedProjectId("someSharedProjectId");
        return gcpParams;
    }

    private EnvironmentNetworkResponse createAwsNetwork() {
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        network.setAws(createAwsNetworkParams());
        network.setSubnetIds(Set.of("mysubnetid"));
        return network;
    }

    private CloudSubnet createCloudSubnet() {
        CloudSubnet cs = new CloudSubnet();
        cs.setType(SubnetType.PUBLIC);
        cs.setName("someCloudSubnet");
        cs.setId("123");
        cs.setIgwAvailable(true);
        cs.setMapPublicIpOnLaunch(false);
        cs.setPrivateSubnet(false);
        cs.setCidr("0.0.0.0/0");
        return cs;
    }

    private EnvironmentNetworkAwsParams createAwsNetworkParams() {
        EnvironmentNetworkAwsParams awsNetwork = new EnvironmentNetworkAwsParams();
        awsNetwork.setVpcId("myvpc");
        return awsNetwork;
    }

}

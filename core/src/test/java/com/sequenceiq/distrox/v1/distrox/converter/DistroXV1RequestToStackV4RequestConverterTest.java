package com.sequenceiq.distrox.v1.distrox.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.TagsV4Request;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetType;
import com.sequenceiq.cloudbreak.converter.v4.stacks.TelemetryConverter;
import com.sequenceiq.cloudbreak.service.datalake.SdxClientService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAvailabilityType;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;
import com.sequenceiq.distrox.api.v1.distrox.model.tags.TagsV1Request;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkAwsParams;
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

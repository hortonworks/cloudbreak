package com.sequenceiq.distrox.v1.distrox.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.network.AwsNetworkV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkV4Request;
import com.sequenceiq.cloudbreak.converter.v4.stacks.TelemetryConverter;
import com.sequenceiq.cloudbreak.service.datalake.SdxClientService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseAvailabilityType;
import com.sequenceiq.distrox.api.v1.distrox.model.database.DistroXDatabaseRequest;
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
        when(environmentClientService.getByName(anyString())).thenReturn(createEnvironment());
        when(networkConverter.convertToNetworkV4Request(any())).thenReturn(createNetworkV4Request());
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
    void convertAsTemplate() {
        when(environmentClientService.getByName(anyString())).thenReturn(createEnvironment());
        when(networkConverter.convertToNetworkV4Request(any())).thenReturn(createNetworkV4Request());
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

    private NetworkV4Request createNetworkV4Request() {
        NetworkV4Request network = new NetworkV4Request();
        network.setAws(createAwsNetworkV4Parameters());
        return network;
    }

    private AwsNetworkV4Parameters createAwsNetworkV4Parameters() {
        AwsNetworkV4Parameters awsNetwork = new AwsNetworkV4Parameters();
        awsNetwork.setSubnetId("mysubnetid");
        awsNetwork.setVpcId("myvpc");
        return awsNetwork;
    }

    private DetailedEnvironmentResponse createEnvironment() {
        DetailedEnvironmentResponse env = new DetailedEnvironmentResponse();
        env.setEnvironmentStatus(EnvironmentStatus.AVAILABLE);
        env.setCloudPlatform("AWS");
        env.setNetwork(createNetwork());
        env.setRegions(createCompactRegionResponse());
        return env;
    }

    private CompactRegionResponse createCompactRegionResponse() {
        CompactRegionResponse regionResponse = new CompactRegionResponse();
        regionResponse.setNames(List.of("myregion"));
        return regionResponse;
    }

    private EnvironmentNetworkResponse createNetwork() {
        EnvironmentNetworkResponse network = new EnvironmentNetworkResponse();
        network.setAws(createAwsNetwork());
        network.setSubnetIds(Set.of("mysubnetid"));
        return network;
    }

    private EnvironmentNetworkAwsParams createAwsNetwork() {
        EnvironmentNetworkAwsParams awsNetwork = new EnvironmentNetworkAwsParams();
        awsNetwork.setVpcId("myvpc");
        return awsNetwork;
    }
}

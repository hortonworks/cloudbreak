package com.sequenceiq.environment.network;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Subnet;
import com.sequenceiq.cloudbreak.cloud.network.NetworkCidr;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.environment.model.base.CloudStorageValidation;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentBackup;
import com.sequenceiq.environment.environment.dto.EnvironmentEditDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.environment.validation.validators.NetworkValidator;
import com.sequenceiq.environment.network.dao.domain.AwsNetwork;
import com.sequenceiq.environment.network.dao.domain.AzureNetwork;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dao.domain.GcpNetwork;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dao.repository.BaseNetworkRepository;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.v1.converter.EnvironmentNetworkConverter;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;

@ExtendWith(MockitoExtension.class)
class NetworkServiceTest {

    private BaseNetworkRepository networkRepository = mock(BaseNetworkRepository.class);

    private Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap = mock(Map.class);

    private CloudNetworkService cloudNetworkService = mock(CloudNetworkService.class);

    private EnvironmentNetworkService environmentNetworkService = mock(EnvironmentNetworkService.class);

    private NetworkValidator networkCreationValidator = mock(NetworkValidator.class);

    private EnvironmentNetworkConverter environmentNetworkConverter = mock(EnvironmentNetworkConverter.class);

    private RegionAwareCrnGenerator regionAwareCrnGenerator = mock(RegionAwareCrnGenerator.class);

    private NetworkService underTest = new NetworkService(
            networkRepository,
            environmentNetworkConverterMap,
            cloudNetworkService,
            environmentNetworkService,
            networkCreationValidator,
            regionAwareCrnGenerator);

    @Test
    void testSaveNetworkIfNewNetwork() {
        NetworkDto networkDto = mock(NetworkDto.class);
        EnvironmentNetworkConverter environmentNetworkConverter = mock(EnvironmentNetworkConverter.class);
        Network network = mock(Network.class);
        Credential credential = mock(Credential.class);

        BaseNetwork baseNetwork = new AwsNetwork();
        baseNetwork.setRegistrationType(RegistrationType.CREATE_NEW);
        Environment environment = new Environment();
        environment.setCloudPlatform("AWS");
        environment.setCredential(credential);

        when(environmentNetworkConverterMap.get(any(CloudPlatform.class))).thenReturn(environmentNetworkConverter);
        when(environmentNetworkConverter.convert(environment, networkDto, Map.of(), Map.of())).thenReturn(baseNetwork);
        when(networkRepository.save(baseNetwork)).thenReturn(baseNetwork);

        BaseNetwork result = underTest.saveNetwork(environment, networkDto, "accountId", Map.of(), Map.of());

        Assertions.assertNull(result.getNetworkCidr());
        verify(environmentNetworkService, times(0)).getNetworkCidr(eq(network), anyString(), eq(credential));
    }

    @Test
    void testRefreshMetadataFromAwsCloudProviderMustUseSubnetId() {
        NetworkDto networkDto = mock(NetworkDto.class);
        AuthenticationDto authenticationDto = mock(AuthenticationDto.class);
        EnvironmentTelemetry environmentTelemetry = mock(EnvironmentTelemetry.class);
        EnvironmentBackup environmentBackup = mock(EnvironmentBackup.class);
        SecurityAccessDto securityAccessDto = mock(SecurityAccessDto.class);
        ParametersDto parametersDto = mock(ParametersDto.class);

        EnvironmentNetworkConverter environmentNetworkConverter = mock(EnvironmentNetworkConverter.class);
        Network network = mock(Network.class);
        Credential credential = mock(Credential.class);

        BaseNetwork baseNetwork = new GcpNetwork();
        baseNetwork.setRegistrationType(RegistrationType.EXISTING);

        Environment environment = new Environment();
        environment.setCloudPlatform("AWS");
        environment.setCredential(credential);

        EnvironmentEditDto environmentEditDto = EnvironmentEditDto.builder()
                .withDescription("description")
                .withAccountId("accountId")
                .withNetwork(networkDto)
                .withAuthentication(authenticationDto)
                .withTelemetry(environmentTelemetry)
                .withBackup(environmentBackup)
                .withSecurityAccess(securityAccessDto)
                .withTunnel(Tunnel.CCMV2)
                .withIdBrokerMappingSource(IdBrokerMappingSource.MOCK)
                .withCloudStorageValidation(CloudStorageValidation.ENABLED)
                .withAdminGroupName("adminGroupName")
                .withParameters(parametersDto)
                .withProxyConfig(new ProxyConfig())
                .build();

        when(environmentNetworkConverterMap.get(any(CloudPlatform.class)))
                .thenReturn(environmentNetworkConverter);
        when(environmentNetworkConverter.convertToDto(baseNetwork))
                .thenReturn(networkDto);
        when(cloudNetworkService.retrieveSubnetMetadata(any(Environment.class), any(NetworkDto.class)))
                .thenReturn(Map.of("s1", cloudSubnet("s1", "subnet1")));
        when(cloudNetworkService.retrieveEndpointGatewaySubnetMetadata(any(Environment.class), any(NetworkDto.class)))
                .thenReturn(Map.of("s1", cloudSubnet("s1", "subnet1")));
        when(environmentNetworkConverter.convertToNetwork(any(BaseNetwork.class)))
                .thenReturn(network);
        when(environmentNetworkService.getNetworkCidr(any(Network.class), anyString(), any(Credential.class)))
                .thenReturn(new NetworkCidr("10.0.0.0", new ArrayList<>()));

        BaseNetwork result = underTest.refreshMetadataFromCloudProvider(baseNetwork, environmentEditDto, environment);

        Assertions.assertEquals(result.getSubnetMetas().keySet().stream().findFirst().get(), "s1");
        Assertions.assertEquals(result.getSubnetMetas().keySet().size(), 1);
    }

    @Test
    void testRefreshMetadataFromGoogleCloudProviderMustUseSubnetName() {
        NetworkDto networkDto = mock(NetworkDto.class);
        AuthenticationDto authenticationDto = mock(AuthenticationDto.class);
        EnvironmentTelemetry environmentTelemetry = mock(EnvironmentTelemetry.class);
        EnvironmentBackup environmentBackup = mock(EnvironmentBackup.class);
        SecurityAccessDto securityAccessDto = mock(SecurityAccessDto.class);
        ParametersDto parametersDto = mock(ParametersDto.class);

        EnvironmentNetworkConverter environmentNetworkConverter = mock(EnvironmentNetworkConverter.class);
        Network network = mock(Network.class);
        Credential credential = mock(Credential.class);

        BaseNetwork baseNetwork = new GcpNetwork();
        baseNetwork.setRegistrationType(RegistrationType.EXISTING);

        Environment environment = new Environment();
        environment.setCloudPlatform("GCP");
        environment.setCredential(credential);

        EnvironmentEditDto environmentEditDto = EnvironmentEditDto.builder()
                .withDescription("description")
                .withAccountId("accountId")
                .withNetwork(networkDto)
                .withAuthentication(authenticationDto)
                .withTelemetry(environmentTelemetry)
                .withBackup(environmentBackup)
                .withSecurityAccess(securityAccessDto)
                .withTunnel(Tunnel.CCMV2)
                .withIdBrokerMappingSource(IdBrokerMappingSource.MOCK)
                .withCloudStorageValidation(CloudStorageValidation.ENABLED)
                .withAdminGroupName("adminGroupName")
                .withParameters(parametersDto)
                .withProxyConfig(new ProxyConfig())
                .build();

        when(environmentNetworkConverterMap.get(any(CloudPlatform.class)))
                .thenReturn(environmentNetworkConverter);
        when(environmentNetworkConverter.convertToDto(baseNetwork))
                .thenReturn(networkDto);
        when(cloudNetworkService.retrieveSubnetMetadata(any(Environment.class), any(NetworkDto.class)))
                .thenReturn(Map.of("s1", cloudSubnet("s1", "subnet1")));
        when(cloudNetworkService.retrieveEndpointGatewaySubnetMetadata(any(Environment.class), any(NetworkDto.class)))
                .thenReturn(Map.of("s1", cloudSubnet("s1", "subnet1")));
        when(environmentNetworkConverter.convertToNetwork(any(BaseNetwork.class)))
                .thenReturn(network);
        when(environmentNetworkService.getNetworkCidr(any(Network.class), anyString(), any(Credential.class)))
            .thenReturn(new NetworkCidr("10.0.0.0", new ArrayList<>()));

        BaseNetwork result = underTest.refreshMetadataFromCloudProvider(baseNetwork, environmentEditDto, environment);

        Assertions.assertEquals(result.getSubnetMetas().keySet().stream().findFirst().get(), "subnet1");
        Assertions.assertEquals(result.getSubnetMetas().keySet().size(), 1);
    }

    private CloudSubnet cloudSubnet(String id, String name) {
        CloudSubnet cloudSubnet = new CloudSubnet();
        cloudSubnet.setName(name);
        cloudSubnet.setId(id);
        return cloudSubnet;
    }

    @ParameterizedTest
    @EnumSource(CloudPlatform.class)
    void testMergeNetworkDtoWithNetworkIfNetworkCreateNew(CloudPlatform cloudPlatform) {
        AwsNetwork baseNetwork = new AwsNetwork();
        baseNetwork.setSubnetMetas(Collections.emptyMap());
        baseNetwork.setRegistrationType(RegistrationType.CREATE_NEW);
        Environment environment = new Environment();
        environment.setCloudPlatform(cloudPlatform.name());
        environment.setNetwork(baseNetwork);
        NetworkDto networkDto = NetworkDto.builder().withRegistrationType(RegistrationType.CREATE_NEW).build();
        EnvironmentEditDto environmentEditDto = EnvironmentEditDto.builder().withNetwork(networkDto).build();

        when(environmentNetworkConverterMap.get(cloudPlatform)).thenReturn(environmentNetworkConverter);
        when(environmentNetworkConverter.convertToDto(baseNetwork)).thenReturn(networkDto);
        when(networkCreationValidator.validateNetworkEdit(eq(environment), any(NetworkDto.class)))
                .thenReturn(new ValidationResult.ValidationResultBuilder());

        BadRequestException exception = Assertions.assertThrows(BadRequestException.class,
                () -> underTest.validate(baseNetwork, environmentEditDto, environment));
        String baseMessage = "Subnets of this environment could not be modified, because its network has been created by Cloudera. " +
                "You need to re-install the environment into an existing VPC/VNet.";
        String providerSpecificLink = "";
        switch (cloudPlatform) {
            case AWS:
                providerSpecificLink = " Refer to Cloudera documentation at "
                        + "https://docs.cloudera.com/cdp-public-cloud/cloud/requirements-aws/topics/mc-aws-req-vpc.html for more information.";
                break;
            case AZURE:
                providerSpecificLink = " Refer to Cloudera documentation at "
                        + "https://docs.cloudera.com/cdp-public-cloud/cloud/requirements-azure/topics/mc-azure-vnet-and-subnets.html for more information.";
                break;
            default:
                break;
        }
        Assertions.assertEquals(String.format("%s%s", baseMessage, providerSpecificLink), exception.getMessage());
    }

    @Test
    void testMergeNetworkDtoWithNetworkForAvailabilityZonesInAzure() {
        BaseNetwork baseNetwork = new AzureNetwork();
        baseNetwork.setSubnetMetas(Collections.emptyMap());
        Environment environment = new Environment();
        environment.setCloudPlatform(CloudPlatform.AZURE.name());
        environment.setNetwork(baseNetwork);
        Set<String> availabilityZones = Set.of("1", "2");
        NetworkDto networkDto = NetworkDto.builder()
                .withAzure(AzureParams.builder()
                        .withAvailabilityZones(availabilityZones)
                        .build())
                .build();
        EnvironmentEditDto environmentEditDto = EnvironmentEditDto.builder().withNetwork(networkDto).build();
        when(environmentNetworkConverterMap.get(CloudPlatform.AZURE)).thenReturn(environmentNetworkConverter);
        when(environmentNetworkConverter.convertToDto(baseNetwork)).thenReturn(networkDto);
        when(environmentNetworkConverter.extendBuilderWithProviderSpecificParameters(any(NetworkDto.Builder.class), any(NetworkDto.class),
                any(NetworkDto.class))).thenAnswer(AdditionalAnswers.returnsFirstArg());
        when(networkCreationValidator.validateNetworkEdit(eq(environment), any(NetworkDto.class)))
                .thenReturn(new ValidationResult.ValidationResultBuilder());
        underTest.validate(baseNetwork, environmentEditDto, environment);
        verify(environmentNetworkConverter, times(1)).updateAvailabilityZones(baseNetwork, availabilityZones);
    }

    @Test
    void testRefreshMetadataFromCloudProviderWhenVpcHasMultipleCidrs() {
        String primaryCidr = "10.0.0.0/16";
        String secondaryCidr = "10.2.0.0/16";
        AwsNetwork baseNetwork = new AwsNetwork();
        baseNetwork.setSubnetMetas(Collections.emptyMap());
        baseNetwork.setRegistrationType(RegistrationType.CREATE_NEW);
        Environment environment = new Environment();
        environment.setCloudPlatform("AWS");
        environment.setNetwork(baseNetwork);
        NetworkDto networkDto = NetworkDto.builder().withRegistrationType(RegistrationType.CREATE_NEW).build();
        EnvironmentEditDto environmentEditDto = EnvironmentEditDto.builder().withNetwork(networkDto).build();
        Network network = new Network(new Subnet(primaryCidr));
        NetworkCidr networkCidr = new NetworkCidr(primaryCidr, List.of(primaryCidr, secondaryCidr));

        when(environmentNetworkConverterMap.get(CloudPlatform.AWS)).thenReturn(environmentNetworkConverter);
        when(environmentNetworkConverter.convertToDto(baseNetwork)).thenReturn(networkDto);
        when(environmentNetworkConverter.convertToNetwork(baseNetwork)).thenReturn(network);
        when(environmentNetworkService.getNetworkCidr(network, environment.getCloudPlatform(), environment.getCredential())).thenReturn(networkCidr);

        BaseNetwork actualNetwork = underTest.refreshMetadataFromCloudProvider(baseNetwork, environmentEditDto, environment);

        verify(cloudNetworkService, times(1)).retrieveSubnetMetadata(eq(environment), any(NetworkDto.class));
        verify(environmentNetworkService, times(1)).getNetworkCidr(network, environment.getCloudPlatform(), environment.getCredential());
        Assertions.assertEquals(primaryCidr, actualNetwork.getNetworkCidr());
        Assertions.assertEquals(StringUtils.join(networkCidr.getCidrs(), ","), actualNetwork.getNetworkCidrs());
    }

    @Test
    void testClonedNetworkDtoHasSubnet() {
        NetworkDto editNetworkDto = NetworkDto.builder()
                .withSubnetMetas(Map.of("editedSubnet1", new CloudSubnet()))
                .build();
        NetworkDto capturedNetwork = captureNetworkFromSubnetEditValidate(editNetworkDto);
        assertThat(capturedNetwork.getSubnetIds()).hasSameElementsAs(Set.of("editedSubnet1"));
    }

    @Test
    void testClonedNetworkDtoHasEndpointGatewaySubnet() {
        NetworkDto editNetworkDto = NetworkDto.builder()
                .withEndpointGatewaySubnetMetas(Map.of("editedGwSubnet1", new CloudSubnet()))
                .build();
        NetworkDto capturedNetwork = captureNetworkFromSubnetEditValidate(editNetworkDto);
        assertThat(capturedNetwork.getEndpointGatewaySubnetIds()).hasSameElementsAs(Set.of("editedGwSubnet1"));
    }

    private NetworkDto captureNetworkFromSubnetEditValidate(NetworkDto editNetworkDto) {
        BaseNetwork baseNetwork = new AwsNetwork();
        baseNetwork.setSubnetMetas(Collections.emptyMap());
        Environment environment = new Environment();
        environment.setCloudPlatform(CloudPlatform.AWS.name());
        environment.setNetwork(baseNetwork);
        NetworkDto savedNetwork = NetworkDto.builder().build();
        EnvironmentEditDto environmentEditDto = EnvironmentEditDto.builder().withNetwork(editNetworkDto).build();
        when(environmentNetworkConverterMap.get(CloudPlatform.AWS)).thenReturn(environmentNetworkConverter);
        when(environmentNetworkConverter.convertToDto(baseNetwork)).thenReturn(savedNetwork);
        when(networkCreationValidator.validateNetworkEdit(eq(environment), any(NetworkDto.class)))
                .thenReturn(new ValidationResult.ValidationResultBuilder());
        underTest.validate(baseNetwork, environmentEditDto, environment);
        ArgumentCaptor<NetworkDto> networkCaptor = ArgumentCaptor.forClass(NetworkDto.class);
        verify(networkCreationValidator).validateNetworkEdit(eq(environment), networkCaptor.capture());
        return networkCaptor.getValue();
    }
}

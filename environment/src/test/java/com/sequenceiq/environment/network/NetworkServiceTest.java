package com.sequenceiq.environment.network;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentEditDto;
import com.sequenceiq.environment.environment.validation.validators.NetworkCreationValidator;
import com.sequenceiq.environment.network.dao.domain.AwsNetwork;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dao.repository.BaseNetworkRepository;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.v1.converter.EnvironmentNetworkConverter;

@ExtendWith(MockitoExtension.class)
public class NetworkServiceTest {

    private BaseNetworkRepository networkRepository = mock(BaseNetworkRepository.class);

    private Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap = mock(Map.class);

    private CloudNetworkService cloudNetworkService = mock(CloudNetworkService.class);

    private EnvironmentNetworkService environmentNetworkService = mock(EnvironmentNetworkService.class);

    private NetworkCreationValidator networkCreationValidator = mock(NetworkCreationValidator.class);

    private EnvironmentNetworkConverter environmentNetworkConverter = mock(EnvironmentNetworkConverter.class);

    private NetworkService underTest = new NetworkService(
            networkRepository,
            environmentNetworkConverterMap,
            cloudNetworkService,
            environmentNetworkService,
            networkCreationValidator);

    @Test
    public void testSaveNetworkIfExistingNetwork() {
        NetworkDto networkDto = mock(NetworkDto.class);
        EnvironmentNetworkConverter environmentNetworkConverter = mock(EnvironmentNetworkConverter.class);
        Network network = mock(Network.class);
        Credential credential = mock(Credential.class);

        String cidr = "10.0.0.0/16";
        BaseNetwork baseNetwork = new AwsNetwork();
        baseNetwork.setRegistrationType(RegistrationType.EXISTING);
        Environment environment = new Environment();
        environment.setCloudPlatform("AWS");
        environment.setCredential(credential);

        when(environmentNetworkConverterMap.get(any(CloudPlatform.class))).thenReturn(environmentNetworkConverter);
        when(environmentNetworkConverter.convert(environment, networkDto, Collections.emptyMap())).thenReturn(baseNetwork);
        when(environmentNetworkConverter.convertToNetwork(baseNetwork)).thenReturn(network);
        when(environmentNetworkService.getNetworkCidr(eq(network), anyString(), eq(credential))).thenReturn(cidr);
        when(networkRepository.save(baseNetwork)).thenReturn(baseNetwork);

        BaseNetwork result = underTest.saveNetwork(environment, networkDto, "accountId", Collections.emptyMap());

        Assertions.assertEquals(cidr, result.getNetworkCidr());
        verify(environmentNetworkService, times(1)).getNetworkCidr(eq(network), anyString(), eq(credential));
    }

    @Test
    public void testSaveNetworkIfNewNetwork() {
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
        when(environmentNetworkConverter.convert(environment, networkDto, Collections.emptyMap())).thenReturn(baseNetwork);
        when(networkRepository.save(baseNetwork)).thenReturn(baseNetwork);

        BaseNetwork result = underTest.saveNetwork(environment, networkDto, "accountId", Collections.emptyMap());

        Assertions.assertNull(result.getNetworkCidr());
        verify(environmentNetworkService, times(0)).getNetworkCidr(eq(network), anyString(), eq(credential));
    }

    // TODO: 2020. 01. 31. move these tests to the new CloudNetworkServiceTest class
    /*@Test
    public void testRetrieveSubnetMetadataIfNetworkNull() {
        Map<String, CloudSubnet> actual = underTest.retrieveSubnetMetadata(null, null);
        Assertions.assertTrue(actual.isEmpty());
    }

    @Test
    public void testRetrieveSubnetMetadataWhenAwsAndNoExistedSubnetOnProvider() {
        Environment environment = new Environment();
        environment.setCloudPlatform("AWS");
        Region region = new Region();
        region.setName("region1");
        environment.setRegions(Set.of(region));
        NetworkDto networkDto = NetworkDto.builder().withSubnetMetas(Map.of("sub", new CloudSubnet())).build();
        when(cloudNetworkService.getCloudNetworks(any(PlatformResourceRequest.class))).thenReturn(new CloudNetworks(Map.of("region1", Set.of())));
        Map<String, CloudSubnet> actual = underTest.retrieveSubnetMetadata(environment, networkDto);
        Assertions.assertTrue(actual.isEmpty());
    }

    @Test
    public void testRetrieveSubnetMetadataWhenAwsAndHasSubnetOnProviderAndMatch() {
        Environment environment = new Environment();
        environment.setCloudPlatform("AWS");
        Region region = new Region();
        region.setName("region1");
        environment.setRegions(Set.of(region));
        CloudSubnet editSubnet = new CloudSubnet();
        NetworkDto networkDto = NetworkDto.builder().withSubnetMetas(Map.of("sub", editSubnet)).build();
        CloudSubnet providerSubnet = new CloudSubnet();
        providerSubnet.setId("sub");
        CloudNetwork cloudNetwork = new CloudNetwork("network", "network", Set.of(providerSubnet), Collections.emptyMap());
        when(cloudNetworkService.getCloudNetworks(any(PlatformResourceRequest.class))).thenReturn(new CloudNetworks(Map.of("region1",
                Set.of(cloudNetwork))));
        Map<String, CloudSubnet> actual = underTest.retrieveSubnetMetadata(environment, networkDto);
        Assertions.assertEquals(1, actual.size());
        Assertions.assertEquals(actual.get("sub"), providerSubnet);
    }

    @Test
    public void testRetrieveSubnetMetadataWhenAwsAndHasSubnetOnProviderAndNotMatch() {
        Environment environment = new Environment();
        environment.setCloudPlatform("AWS");
        Region region = new Region();
        region.setName("region1");
        environment.setRegions(Set.of(region));
        CloudSubnet editSubnet = new CloudSubnet();
        NetworkDto networkDto = NetworkDto.builder().withSubnetMetas(Map.of("sub", editSubnet)).build();
        CloudSubnet providerSubnet = new CloudSubnet();
        providerSubnet.setId("diff-sub");
        CloudNetwork cloudNetwork = new CloudNetwork("network", "network", Set.of(providerSubnet), Collections.emptyMap());
        when(cloudNetworkService.getCloudNetworks(any(PlatformResourceRequest.class))).thenReturn(new CloudNetworks(Map.of("region1",
                Set.of(cloudNetwork))));
        Map<String, CloudSubnet> actual = underTest.retrieveSubnetMetadata(environment, networkDto);
        Assertions.assertEquals(0, actual.size());
    }*/

    @Test
    public void testMergeNetworkDtoWithNetworkIfNetworkCreateNew() {
        AwsNetwork baseNetwork = new AwsNetwork();
        baseNetwork.setSubnetMetas(Collections.emptyMap());
        baseNetwork.setRegistrationType(RegistrationType.CREATE_NEW);
        Environment environment = new Environment();
        environment.setCloudPlatform("AWS");
        environment.setNetwork(baseNetwork);
        NetworkDto networkDto = NetworkDto.builder().withRegistrationType(RegistrationType.CREATE_NEW).build();
        EnvironmentEditDto environmentEditDto = EnvironmentEditDto.builder().withNetwork(networkDto).build();

        when(environmentNetworkConverterMap.get(CloudPlatform.AWS)).thenReturn(environmentNetworkConverter);
        when(environmentNetworkConverter.convertToDto(baseNetwork)).thenReturn(networkDto);
        when(networkCreationValidator.validateNetworkEdit(eq(environment), any(NetworkDto.class)))
                .thenReturn(new ValidationResult.ValidationResultBuilder());

        BadRequestException exception = Assertions.assertThrows(BadRequestException.class,
                () -> underTest.validateAndReplaceSubnets(baseNetwork, environmentEditDto, environment));
        Assertions.assertEquals("Subnet could not be attached to this environment, because it is newly created by Cloudbreak. " +
                "You need to re-install the the environment into an existing VPC", exception.getMessage());
    }
}

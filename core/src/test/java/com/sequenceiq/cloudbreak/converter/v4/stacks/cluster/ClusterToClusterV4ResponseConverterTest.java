package com.sequenceiq.cloudbreak.converter.v4.stacks.cluster;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.ConversionService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.ConfigStrategy;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.ClusterV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.GatewayV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.cluster.gateway.topology.ClusterExposedServiceV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.efs.CloudEfsConfiguration;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.efs.PerformanceMode;
import com.sequenceiq.cloudbreak.cloud.model.filesystem.efs.ThroughputMode;
import com.sequenceiq.cloudbreak.cmtemplate.validation.StackServiceComponentDescriptor;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.converter.AbstractEntityConverterTest;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.cloudstorage.CloudStorage;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.dto.ProxyConfig;
import com.sequenceiq.cloudbreak.service.ServiceEndpointCollector;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.proxy.ProxyConfigDtoService;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.common.api.cloudstorage.AwsEfsParameters;
import com.sequenceiq.common.api.cloudstorage.CloudStorageResponse;
import com.sequenceiq.common.api.type.CertExpirationState;
import com.sequenceiq.common.model.FileSystemType;

@ExtendWith(MockitoExtension.class)
public class ClusterToClusterV4ResponseConverterTest extends AbstractEntityConverterTest<Cluster> {

    @InjectMocks
    private ClusterToClusterV4ResponseConverter underTest;

    @Mock
    private CloudStorageConverter cloudStorageConverter;

    @Mock
    private ConversionService conversionService;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private ServiceEndpointCollector serviceEndpointCollector;

    @Mock
    private BlueprintService blueprintService;

    @Mock
    private ConverterUtil converterUtil;

    @Mock
    private ProxyConfigDtoService proxyConfigDtoService;

    @BeforeEach
    public void setUp() {
        given(conversionService.convert(any(Object.class), eq(WorkspaceResourceV4Response.class)))
                .willReturn(new WorkspaceResourceV4Response());
        given(conversionService.convert(any(Object.class), eq(GatewayV4Response.class)))
                .willReturn(new GatewayV4Response());
    }

    @Test
    public void testConvert() {
        // GIVEN
        getSource().setConfigStrategy(ConfigStrategy.NEVER_APPLY);
        getSource().setBlueprint(new Blueprint());
        getSource().setExtendedBlueprintText("asdf");
        getSource().setFqdn("some.fqdn");
        getSource().setCertExpirationState(CertExpirationState.HOST_CERT_EXPIRING);
        given(stackUtil.extractClusterManagerIp(any(Stack.class))).willReturn("10.0.0.1");
        given(stackUtil.extractClusterManagerAddress(any(Stack.class))).willReturn("some.fqdn");
        Cluster source = getSource();
        TestUtil.setSecretField(Cluster.class, "cloudbreakAmbariUser", source, "user", "secret/path");
        TestUtil.setSecretField(Cluster.class, "cloudbreakAmbariPassword", source, "pass", "secret/path");
        TestUtil.setSecretField(Cluster.class, "dpAmbariUser", source, "user", "secret/path");
        TestUtil.setSecretField(Cluster.class, "dpAmbariPassword", source, "pass", "secret/path");
        when(conversionService.convert("secret/path", SecretResponse.class)).thenReturn(new SecretResponse("kv", "pass"));
        when(conversionService.convert(getSource().getBlueprint(), BlueprintV4Response.class)).thenReturn(new BlueprintV4Response());
        when(serviceEndpointCollector.getManagerServerUrl(any(Cluster.class), anyString())).thenReturn("http://server/");
        given(proxyConfigDtoService.getByCrn(anyString())).willReturn(ProxyConfig.builder().withCrn("crn").withName("name").build());
        // WHEN
        ClusterV4Response result = underTest.convert(source);
        // THEN
        assertEquals(1L, (long) result.getId());
        assertEquals(getSource().getExtendedBlueprintText(), result.getExtendedBlueprintText());
        assertEquals(CertExpirationState.HOST_CERT_EXPIRING, result.getCertExpirationState());

        List<String> skippedFields = Lists.newArrayList("customContainers", "cm", "creationFinished", "cloudStorage", "gateway");
        assertAllFieldsNotNull(result, skippedFields);
    }

    @Test
    public void testConvertAdditionalFileSystem() {
        // GIVEN
        getSource().setConfigStrategy(ConfigStrategy.NEVER_APPLY);
        getSource().setBlueprint(new Blueprint());
        getSource().setExtendedBlueprintText("asdf");
        getSource().setFqdn("some.fqdn");
        getSource().setCertExpirationState(CertExpirationState.HOST_CERT_EXPIRING);
        getSource().setAdditionalFileSystem(getEfsFileSystem());
        given(stackUtil.extractClusterManagerIp(any(Stack.class))).willReturn("10.0.0.1");
        given(stackUtil.extractClusterManagerAddress(any(Stack.class))).willReturn("some.fqdn");
        Cluster source = getSource();
        TestUtil.setSecretField(Cluster.class, "cloudbreakAmbariUser", source, "user", "secret/path");
        TestUtil.setSecretField(Cluster.class, "cloudbreakAmbariPassword", source, "pass", "secret/path");
        TestUtil.setSecretField(Cluster.class, "dpAmbariUser", source, "user", "secret/path");
        TestUtil.setSecretField(Cluster.class, "dpAmbariPassword", source, "pass", "secret/path");
        when(conversionService.convert("secret/path", SecretResponse.class)).thenReturn(new SecretResponse("kv", "pass"));
        when(conversionService.convert(getSource().getBlueprint(), BlueprintV4Response.class)).thenReturn(new BlueprintV4Response());
        when(serviceEndpointCollector.getManagerServerUrl(any(Cluster.class), anyString())).thenReturn("http://server/");
        given(proxyConfigDtoService.getByCrn(anyString())).willReturn(ProxyConfig.builder().withCrn("crn").withName("name").build());
        when(cloudStorageConverter.fileSystemToResponse(any(FileSystem.class))).thenReturn(new CloudStorageResponse());
        when(cloudStorageConverter.fileSystemToEfsParameters(any(FileSystem.class))).thenReturn(getEfsParameters());
        // WHEN
        ClusterV4Response result = underTest.convert(source);
        // THEN
        assertEquals(1L, (long) result.getId());
        assertEquals(getSource().getExtendedBlueprintText(), result.getExtendedBlueprintText());
        assertEquals(CertExpirationState.HOST_CERT_EXPIRING, result.getCertExpirationState());

        List<String> skippedFields = Lists.newArrayList("customContainers", "cm", "creationFinished", "gateway");
        assertAllFieldsNotNull(result, skippedFields);
    }

    @Test
    public void testConvertWithoutUpSinceField() {
        // GIVEN
        given(proxyConfigDtoService.getByCrn(anyString())).willReturn(ProxyConfig.builder().withCrn("crn").withName("name").build());
        getSource().setUpSince(null);
        // WHEN
        ClusterV4Response result = underTest.convert(getSource());
        // THEN
        assertEquals(0L, result.getMinutesUp());
    }

    @Test
    public void testConvertWithoutMasterComponent() {
        // GIVEN
        given(proxyConfigDtoService.getByCrn(anyString())).willReturn(ProxyConfig.builder().withCrn("crn").withName("name").build());
        // WHEN
        ClusterV4Response result = underTest.convert(getSource());
        // THEN
        assertEquals(1L, (long) result.getId());
    }

    @Test
    public void testExposedServices() {
        Map<String, Collection<ClusterExposedServiceV4Response>> exposedServiceResponseMap = new HashMap<>();
        exposedServiceResponseMap.put("topology1", getExpiosedServices());
        given(serviceEndpointCollector.prepareClusterExposedServices(any(), anyString())).willReturn(exposedServiceResponseMap);
        given(proxyConfigDtoService.getByCrn(anyString())).willReturn(ProxyConfig.builder().withCrn("crn").withName("name").build());

        given(stackUtil.extractClusterManagerIp(any(Stack.class))).willReturn("10.0.0.1");
        given(stackUtil.extractClusterManagerAddress(any(Stack.class))).willReturn("some.fqdn");
        ClusterV4Response clusterResponse = underTest.convert(getSource());
        Map<String, Collection<ClusterExposedServiceV4Response>> clusterExposedServicesForTopologies = clusterResponse.getExposedServices();
        assertEquals(1L, clusterExposedServicesForTopologies.keySet().size());
        Collection<ClusterExposedServiceV4Response> topology1ServiceList = clusterExposedServicesForTopologies.get("topology1");
        assertEquals(2L, topology1ServiceList.size());
    }

    @Override
    public Cluster createSource() {
        Stack stack = TestUtil.stack();
        Blueprint blueprint = TestUtil.blueprint();
        Cluster cluster = TestUtil.cluster(blueprint, stack, 1L);
        cluster.setProxyConfigCrn("test-CRN");
        stack.setCluster(cluster);
        Gateway gateway = new Gateway();
        cluster.setGateway(gateway);
        return cluster;
    }

    private List<ClusterExposedServiceV4Response> getExpiosedServices() {
        List<ClusterExposedServiceV4Response> clusterExposedServiceResponseList = new ArrayList<>();
        ClusterExposedServiceV4Response firstClusterExposedServiceV4Response = new ClusterExposedServiceV4Response();
        firstClusterExposedServiceV4Response.setOpen(true);
        firstClusterExposedServiceV4Response.setServiceUrl("http://service1");
        firstClusterExposedServiceV4Response.setServiceName("serviceName1");
        firstClusterExposedServiceV4Response.setKnoxService("knoxService1");
        firstClusterExposedServiceV4Response.setDisplayName("displayName1");
        ClusterExposedServiceV4Response secondClusterExposedServiceV4Response = new ClusterExposedServiceV4Response();
        clusterExposedServiceResponseList.add(firstClusterExposedServiceV4Response);
        secondClusterExposedServiceV4Response.setOpen(false);
        secondClusterExposedServiceV4Response.setServiceUrl("http://service2");
        secondClusterExposedServiceV4Response.setServiceName("serviceName2");
        secondClusterExposedServiceV4Response.setKnoxService("knoxService2");
        secondClusterExposedServiceV4Response.setDisplayName("displayName2");
        clusterExposedServiceResponseList.add(secondClusterExposedServiceV4Response);
        return clusterExposedServiceResponseList;
    }

    private StackServiceComponentDescriptor createStackServiceComponentDescriptor() {
        return new StackServiceComponentDescriptor("ELASTIC_SEARCH", "MASTER", 1, 1);
    }

    private FileSystem getEfsFileSystem() {
        FileSystem fileSystem = new FileSystem();
        AwsEfsParameters efsParameters = getEfsParameters();

        fileSystem.setName(efsParameters.getName());
        FileSystemType fileSystemType = FileSystemType.EFS;
        fileSystem.setType(fileSystemType);

        Map<String, Object> configurations = new HashMap<>();
        configurations.put(CloudEfsConfiguration.KEY_BACKUP_POLICY_STATUS, efsParameters.getBackupPolicyStatus());
        configurations.put(CloudEfsConfiguration.KEY_ENCRYPTED, efsParameters.getEncrypted());
        configurations.put(CloudEfsConfiguration.KEY_FILESYSTEM_POLICY, efsParameters.getFileSystemPolicy());
        configurations.put(CloudEfsConfiguration.KEY_FILESYSTEM_TAGS, efsParameters.getFileSystemTags());
        configurations.put(CloudEfsConfiguration.KEY_KMSKEYID, efsParameters.getKmsKeyId());
        configurations.put(CloudEfsConfiguration.KEY_LIFECYCLE_POLICIES, efsParameters.getLifeCyclePolicies());
        configurations.put(CloudEfsConfiguration.KEY_PERFORMANCE_MODE, efsParameters.getPerformanceMode());
        configurations.put(CloudEfsConfiguration.KEY_PROVISIONED_THROUGHPUT_INMIBPS, efsParameters.getProvisionedThroughputInMibps());
        configurations.put(CloudEfsConfiguration.KEY_THROUGHPUT_MODE, efsParameters.getThroughputMode());
        configurations.put(CloudEfsConfiguration.KEY_ASSOCIATED_INSTANCE_GROUP_NAMES, efsParameters.getAssociatedInstanceGroupNames());

        String configString;
        try {
            configString = JsonUtil.writeValueAsString(configurations);
        } catch (JsonProcessingException ignored) {
            configString = configurations.toString();
        }

        fileSystem.setConfigurations(new Json(configString));

        CloudStorage cloudStorage = new CloudStorage();
        fileSystem.setCloudStorage(cloudStorage);

        return fileSystem;
    }

    AwsEfsParameters getEfsParameters() {
        String fileSystemName = "lina-efs-0127-1";
        Map<String, String> tags = new HashMap<>();
        tags.put(CloudEfsConfiguration.KEY_FILESYSTEM_TAGS_NAME, fileSystemName);

        AwsEfsParameters efsParameters = new AwsEfsParameters();
        efsParameters.setName(fileSystemName);
        efsParameters.setEncrypted(true);
        efsParameters.setFileSystemTags(tags);
        efsParameters.setPerformanceMode(PerformanceMode.GENERALPURPOSE.toString());
        efsParameters.setThroughputMode(ThroughputMode.BURSTING.toString());
        efsParameters.setAssociatedInstanceGroupNames(List.of("core", "master"));

        return efsParameters;
    }
}

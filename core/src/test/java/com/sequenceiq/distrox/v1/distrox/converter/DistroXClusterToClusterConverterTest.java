package com.sequenceiq.distrox.v1.distrox.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.ClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.common.api.cloudstorage.CloudStorageRequest;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.DistroXClusterV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.cm.ClouderaManagerV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.cm.product.ClouderaManagerProductV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.cm.repository.ClouderaManagerRepositoryV1Request;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.environment.api.v1.proxy.endpoint.ProxyEndpoint;

@ExtendWith(MockitoExtension.class)
class DistroXClusterToClusterConverterTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:accid:user:mockuser@cloudera.com";

    @Mock
    private ClouderaManagerV1ToClouderaManagerV4Converter cmConverter;

    @Mock
    private GatewayV1ToGatewayV4Converter gatewayConverter;

    @Mock
    private CloudStorageDecorator cloudStorageDecorator;

    @Mock
    private ProxyEndpoint proxyEndpoint;

    @InjectMocks
    private DistroXClusterToClusterConverter underTest;

    private DistroXV1Request distroXV1RequestInput;

    private ClusterV4Request clusterV4RequestInput;

    private DetailedEnvironmentResponse env;

    @BeforeEach
    void setUp() {
        env = new DetailedEnvironmentResponse();
        clusterV4RequestInput = createClusterV4Request();
        distroXV1RequestInput = createDistroXV1Request();
    }

    @Test
    void testConvertWithoutEnvWhenExposedServiceIsEmptyThenAllShouldBeConverted() {
        distroXV1RequestInput.getCluster().setExposedServices(new ArrayList<>(0));
        GatewayV4Request gr = new GatewayV4Request();
        when(gatewayConverter.convert(List.of("ALL"))).thenReturn(gr);

        ClusterV4Request result = testConvertDistroXV1Request();

        assertNotNull(result);
        assertEquals(gr, result.getGateway());
        verify(gatewayConverter, times(1)).convert(anyList());
        verify(gatewayConverter, times(1)).convert(List.of("ALL"));
    }

    @Test
    void testConvertWithoutEnvWhenExposedServiceIsNotEmptyThenItShouldBeConverted() {
        GatewayV4Request gr = new GatewayV4Request();
        when(gatewayConverter.convert(distroXV1RequestInput.getCluster().getExposedServices())).thenReturn(gr);
        ClusterV4Request result = testConvertDistroXV1Request();

        assertNotNull(result);
        assertEquals(gr, result.getGateway());
        verify(gatewayConverter, times(1)).convert(anyList());
        verify(gatewayConverter, times(1)).convert(distroXV1RequestInput.getCluster().getExposedServices());
    }

    @Test
    void testConvertWithoutEnvTheNameShouldBeSetToNull() {
        ClusterV4Request result = testConvertDistroXV1Request();

        assertNotNull(result);
        assertNull(result.getName());
    }

    @Test
    void testConvertWithoutEnvTheCustomContainerShouldBeSetToNull() {
        ClusterV4Request result = testConvertDistroXV1Request();

        assertNotNull(result);
        assertNull(result.getCustomContainer());
    }

    @Test
    void testConvertWithoutEnvTheCustomQueueShouldBeSetToNull() {
        ClusterV4Request result = testConvertDistroXV1Request();

        assertNotNull(result);
        assertNull(result.getCustomQueue());
    }

    @Test
    void testConvertWithoutEnvTheUsernameShouldBeSet() {
        ClusterV4Request result = testConvert();

        assertNotNull(result);
        assertEquals(distroXV1RequestInput.getCluster().getUserName(), result.getUserName());
    }

    private ClusterV4Request testConvert() {
        return testConvertDistroXV1Request();
    }

    @Test
    void testConvertWithoutEnvThePasswordShouldBeSet() {
        ClusterV4Request result = testConvertDistroXV1Request();

        assertNotNull(result);
        assertEquals(distroXV1RequestInput.getCluster().getPassword(), result.getPassword());
    }

    @Test
    void testConvertWithoutEnvTheValidateBlueprintShouldBeSet() {
        ClusterV4Request result = testConvertDistroXV1Request();

        assertNotNull(result);
        assertEquals(distroXV1RequestInput.getCluster().getValidateBlueprint(), result.getValidateBlueprint());
    }

    @Test
    void testConvertWithoutEnvTheBlueprintNameShouldBeSet() {
        ClusterV4Request result = testConvertDistroXV1Request();

        assertNotNull(result);
        assertEquals(distroXV1RequestInput.getCluster().getBlueprintName(), result.getBlueprintName());
    }

    @Test
    void testConvertWithoutEnvTheDatabasesShouldBeSet() {
        ClusterV4Request result = testConvertDistroXV1Request();

        assertNotNull(result);
        assertEquals(distroXV1RequestInput.getCluster().getDatabases(), result.getDatabases());
    }

    @Test
    void testConvertWithoutEnvCmConversionShouldHappenIfInputCmIsNotNull() {
        ClouderaManagerV4Request cmConversionResult = new ClouderaManagerV4Request();
        when(cmConverter.convert(distroXV1RequestInput.getCluster().getCm())).thenReturn(cmConversionResult);
        ClusterV4Request result = testConvertDistroXV1Request();

        assertNotNull(result);
        assertEquals(cmConversionResult, result.getCm());

        verify(cmConverter, times(1)).convert(any(ClouderaManagerV1Request.class));
        verify(cmConverter, times(1)).convert(distroXV1RequestInput.getCluster().getCm());
    }

    @Test
    void testConvertWithoutEnvCmConversionShouldNotHappenIfInputCmIsNull() {
        distroXV1RequestInput.getCluster().setCm(null);

        ClusterV4Request result = testConvertDistroXV1Request();

        assertNotNull(result);
        assertNull(result.getCm());

        verify(cmConverter, never()).convert(any(ClouderaManagerV1Request.class));
    }

    @Test
    void testConvertWithoutEnvTheCloudStorageShouldBeSetBasedOnTheInputs() {
        CloudStorageRequest decoratorResult = new CloudStorageRequest();
        when(cloudStorageDecorator.decorate(
                distroXV1RequestInput.getCluster().getBlueprintName(),
                distroXV1RequestInput.getName(),
                distroXV1RequestInput.getCluster().getCloudStorage(),
                null
        )).thenReturn(decoratorResult);
        ClusterV4Request result = testConvertDistroXV1Request();

        assertNotNull(result);
        assertEquals(decoratorResult, result.getCloudStorage());

        verify(cloudStorageDecorator, times(1)).decorate(any(), any(), any(), any());
        verify(cloudStorageDecorator, times(1)).decorate(
                distroXV1RequestInput.getCluster().getBlueprintName(),
                distroXV1RequestInput.getName(),
                distroXV1RequestInput.getCluster().getCloudStorage(),
                null
        );
    }

    @Test
    void testConvertWithEnvWhenExposedServiceIsEmptyThenAllShouldBeConverted() {
        distroXV1RequestInput.getCluster().setExposedServices(new ArrayList<>(0));
        GatewayV4Request gr = new GatewayV4Request();

        when(gatewayConverter.convert(List.of("ALL"))).thenReturn(gr);

        ClusterV4Request result = testConvertDistroXV1RequestWithEnvironment();

        assertNotNull(result);
        assertEquals(gr, result.getGateway());
        verify(gatewayConverter, times(1)).convert(anyList());
        verify(gatewayConverter, times(1)).convert(List.of("ALL"));
    }

    @Test
    void testConvertWithEnvWhenExposedServiceIsNotEmptyThenItShouldBeConverted() {
        GatewayV4Request gr = new GatewayV4Request();
        when(gatewayConverter.convert(distroXV1RequestInput.getCluster().getExposedServices())).thenReturn(gr);
        ClusterV4Request result = testConvertDistroXV1RequestWithEnvironment();

        assertNotNull(result);
        assertEquals(gr, result.getGateway());
        verify(gatewayConverter, times(1)).convert(anyList());
        verify(gatewayConverter, times(1)).convert(distroXV1RequestInput.getCluster().getExposedServices());
    }

    @Test
    void testConvertWithEnvTheNameShouldBeSetToNull() {
        ClusterV4Request result = testConvertDistroXV1RequestWithEnvironment();

        assertNotNull(result);
        assertNull(result.getName());
    }

    @Test
    void testConvertWithEnvTheCustomContainerShouldBeSetToNull() {
        ClusterV4Request result = testConvertDistroXV1RequestWithEnvironment();

        assertNotNull(result);
        assertNull(result.getCustomContainer());
    }

    @Test
    void testConvertWithEnvTheCustomQueueShouldBeSetToNull() {
        ClusterV4Request result = testConvertDistroXV1RequestWithEnvironment();

        assertNotNull(result);
        assertNull(result.getCustomQueue());
    }

    @Test
    void testConvertWithEnvTheUsernameShouldBeSet() {
        ClusterV4Request result = testConvertDistroXV1RequestWithEnvironment();

        assertNotNull(result);
        assertEquals(distroXV1RequestInput.getCluster().getUserName(), result.getUserName());
    }

    @Test
    void testConvertWithEnvThePasswordShouldBeSet() {
        ClusterV4Request result = testConvertDistroXV1RequestWithEnvironment();

        assertNotNull(result);
        assertEquals(distroXV1RequestInput.getCluster().getPassword(), result.getPassword());
    }

    @Test
    void testConvertWithEnvTheValidateBlueprintShouldBeSet() {
        ClusterV4Request result = testConvertDistroXV1RequestWithEnvironment();

        assertNotNull(result);
        assertEquals(distroXV1RequestInput.getCluster().getValidateBlueprint(), result.getValidateBlueprint());
    }

    @Test
    void testConvertWithEnvTheBlueprintNameShouldBeSet() {
        ClusterV4Request result = testConvertDistroXV1RequestWithEnvironment();

        assertNotNull(result);
        assertEquals(distroXV1RequestInput.getCluster().getBlueprintName(), result.getBlueprintName());
    }

    @Test
    void testConvertWithEnvTheDatabasesShouldBeSet() {
        ClusterV4Request result = testConvertDistroXV1RequestWithEnvironment();

        assertNotNull(result);
        assertEquals(distroXV1RequestInput.getCluster().getDatabases(), result.getDatabases());
    }

    @Test
    void testConvertWithEnvCmConversionShouldHappenIfInputCmIsNotNull() {
        ClouderaManagerV4Request cmConversionResult = new ClouderaManagerV4Request();
        when(cmConverter.convert(distroXV1RequestInput.getCluster().getCm())).thenReturn(cmConversionResult);
        ClusterV4Request result = testConvertDistroXV1RequestWithEnvironment();

        assertNotNull(result);
        assertEquals(cmConversionResult, result.getCm());

        verify(cmConverter, times(1)).convert(any(ClouderaManagerV1Request.class));
        verify(cmConverter, times(1)).convert(distroXV1RequestInput.getCluster().getCm());
    }

    @Test
    void testConvertWithEnvCmConversionShouldNotHappenIfInputCmIsNull() {
        distroXV1RequestInput.getCluster().setCm(null);
        ClusterV4Request result = testConvertDistroXV1RequestWithEnvironment();

        assertNotNull(result);
        assertNull(result.getCm());

        verify(cmConverter, never()).convert(any(ClouderaManagerV1Request.class));
    }

    @Test
    void testConvertWithEnvTheCloudStorageShouldBeSetBasedOnTheInputs() {
        CloudStorageRequest decoratorResult = new CloudStorageRequest();
        when(cloudStorageDecorator.decorate(
                distroXV1RequestInput.getCluster().getBlueprintName(),
                distroXV1RequestInput.getName(),
                distroXV1RequestInput.getCluster().getCloudStorage(),
                env
        )).thenReturn(decoratorResult);
        ClusterV4Request result = testConvertDistroXV1RequestWithEnvironment();

        assertNotNull(result);
        assertEquals(decoratorResult, result.getCloudStorage());

        verify(cloudStorageDecorator, times(1)).decorate(any(), any(), any(), any());
        verify(cloudStorageDecorator, times(1)).decorate(
                distroXV1RequestInput.getCluster().getBlueprintName(),
                distroXV1RequestInput.getName(),
                distroXV1RequestInput.getCluster().getCloudStorage(),
                env
        );
    }

    @Test
    void testConvertClusterV4RequestToDistroXClusterV1RequestWhenExposedServicesAreNullThenItShouldNotBeSet() {
        clusterV4RequestInput.setGateway(null);

        DistroXClusterV1Request result = underTest.convert(clusterV4RequestInput);

        assertNotNull(result);
        assertNull(result.getExposedServices());

        verify(gatewayConverter, never()).exposedService(any());
    }

    @Test
    void testConvertClusterV4RequestToDistroXClusterV1RequestWhenExposedServicesAreNotNullThenItShouldBeConverted() {
        List<String> gatewayConversionResult = new ArrayList<>();

        when(gatewayConverter.exposedService(any())).thenReturn(gatewayConversionResult);

        DistroXClusterV1Request result = underTest.convert(clusterV4RequestInput);

        assertNotNull(result);
        assertEquals(gatewayConversionResult, result.getExposedServices());

        verify(gatewayConverter, times(1)).exposedService(any());
        verify(gatewayConverter, times(1)).exposedService(clusterV4RequestInput.getGateway());
    }

    @Test
    void testConvertClusterV4RequestToDistroXClusterV1RequestThenDatabasesShouldBeSet() {
        DistroXClusterV1Request result = underTest.convert(clusterV4RequestInput);

        assertNotNull(result);
        assertEquals(clusterV4RequestInput.getDatabases(), result.getDatabases());
    }

    @Test
    void testConvertClusterV4RequestToDistroXClusterV1RequestThenBlueprintNameShouldBeSet() {
        DistroXClusterV1Request result = underTest.convert(clusterV4RequestInput);

        assertNotNull(result);
        assertEquals(clusterV4RequestInput.getBlueprintName(), result.getBlueprintName());
    }

    @Test
    void testConvertClusterV4RequestToDistroXClusterV1RequestThenCloudStorageShouldBeSet() {
        DistroXClusterV1Request result = underTest.convert(clusterV4RequestInput);

        assertNotNull(result);
        assertEquals(clusterV4RequestInput.getCloudStorage(), result.getCloudStorage());
    }

    @Test
    void testConvertClusterV4RequestToDistroXClusterV1RequestThenProxyShouldBeSet() {
        DistroXClusterV1Request result = underTest.convert(clusterV4RequestInput);

        assertNotNull(result);
        assertEquals(clusterV4RequestInput.getProxyConfigCrn(), result.getProxy());
    }

    @Test
    void testConvertClusterV4RequestToDistroXClusterV1RequestThenUserNameShouldNotBeSet() {
        DistroXClusterV1Request result = underTest.convert(clusterV4RequestInput);

        assertNotNull(result);
        assertNotEquals(clusterV4RequestInput.getUserName(), result.getUserName());
        assertNull(result.getUserName());
    }

    @Test
    void testConvertClusterV4RequestToDistroXClusterV1RequestThenPasswordShouldNotBeSet() {
        DistroXClusterV1Request result = underTest.convert(clusterV4RequestInput);

        assertNotNull(result);
        assertNotEquals(clusterV4RequestInput.getPassword(), result.getPassword());
        assertNull(result.getPassword());
    }

    @Test
    void testConvertClusterV4RequestToDistroXClusterV1RequestWhenCmIsNullThenItShouldNotBeSet() {
        clusterV4RequestInput.setCm(null);

        DistroXClusterV1Request result = underTest.convert(clusterV4RequestInput);

        assertNotNull(result);
        assertNull(result.getCm());

        verify(cmConverter, never()).convert(any(ClouderaManagerV4Request.class));
    }

    @Test
    void testConvertClusterV4RequestToDistroXClusterV1RequestWhenCmIsNotNullThenItShouldBeSet() {
        ClouderaManagerV1Request cmConverterResult = new ClouderaManagerV1Request();
        when(cmConverter.convert(clusterV4RequestInput.getCm())).thenReturn(cmConverterResult);
        DistroXClusterV1Request result = underTest.convert(clusterV4RequestInput);

        assertNotNull(result);
        assertEquals(cmConverterResult, result.getCm());

        verify(cmConverter, times(1)).convert(any(ClouderaManagerV4Request.class));
        verify(cmConverter, times(1)).convert(clusterV4RequestInput.getCm());
    }

    @Test
    public void testConvertToClusterV4RequestWhenEncryptionProfileIsNotNull() {
        DistroXClusterV1Request distroXClusterV1Request = new DistroXClusterV1Request();
        distroXClusterV1Request.setEncryptionProfileName("epName");
        distroXV1RequestInput.setCluster(distroXClusterV1Request);

        ClusterV4Request result = underTest.convert(distroXV1RequestInput);

        assertEquals(distroXClusterV1Request.getEncryptionProfileName(), result.getEncryptionProfileName());
    }

    @Test
    public void testConvertToDistroXClusterV1RequestWhenEncryptionProfileIsNotNull() {
        clusterV4RequestInput.setEncryptionProfileName("epName");

        DistroXClusterV1Request result = underTest.convert(clusterV4RequestInput);

        assertEquals(clusterV4RequestInput.getEncryptionProfileName(), result.getEncryptionProfileName());
    }

    private DistroXV1Request createDistroXV1Request() {
        DistroXV1Request r = new DistroXV1Request();
        r.setCluster(createDistroXClusterV1Request());
        r.setName("SomeDistroX");
        return r;
    }

    private DistroXClusterV1Request createDistroXClusterV1Request() {
        DistroXClusterV1Request r = new DistroXClusterV1Request();
        r.setBlueprintName("someBP");
        r.setValidateBlueprint(true);
        r.setDatabases(Set.of("DB1"));
        r.setExposedServices(List.of("service1", "service2", "service3"));
        r.setPassword("somePW");
        r.setProxy("someProxy");
        r.setUserName("someUserName");
        r.setCm(createClouderaManagerV1Request());
        r.setCloudStorage(createCloudStorageRequest());
        return r;
    }

    private ClouderaManagerV1Request createClouderaManagerV1Request() {
        ClouderaManagerV1Request r = new ClouderaManagerV1Request();
        r.setRepository(createClouderaManagerRepositoryV1Request());
        r.setProducts(List.of(createClouderaManagerProductV1Request()));
        r.setEnableAutoTls(true);
        return r;
    }

    private ClouderaManagerRepositoryV1Request createClouderaManagerRepositoryV1Request() {
        ClouderaManagerRepositoryV1Request r = new ClouderaManagerRepositoryV1Request();
        r.setBaseUrl("someBaeUrl");
        r.setGpgKeyUrl("someGpgKeyUrl");
        r.setVersion("someVersion");
        return r;
    }

    private ClouderaManagerProductV1Request createClouderaManagerProductV1Request() {
        ClouderaManagerProductV1Request r = new ClouderaManagerProductV1Request();
        r.setName("someName");
        r.setCsd(List.of("someCsd"));
        r.setParcel("someParcel");
        r.setVersion("someVersion");
        return r;
    }

    private ClusterV4Request createClusterV4Request() {
        ClusterV4Request r = new ClusterV4Request();
        r.setBlueprintName("someBlueprint");
        r.setGateway(createGatewayV4Request());
        r.setDatabases(Set.of("DB1", "DB2"));
        r.setUserName("someUserName");
        r.setPassword("somePassword");
        r.setCm(createClouderaManagerV4Request());
        r.setCloudStorage(createCloudStorageRequest());
        r.setProxyConfigCrn("someProxyConfigCrn");
        return r;
    }

    private CloudStorageRequest createCloudStorageRequest() {
        return new CloudStorageRequest();
    }

    private ClouderaManagerV4Request createClouderaManagerV4Request() {
        return new ClouderaManagerV4Request();
    }

    private GatewayV4Request createGatewayV4Request() {
        return new GatewayV4Request();
    }

    private ClusterV4Request testConvertDistroXV1Request() {
        return ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.convert(distroXV1RequestInput));
    }

    private ClusterV4Request testConvertDistroXV1RequestWithEnvironment() {
        return ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.convert(distroXV1RequestInput, env));
    }
}
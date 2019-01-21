package com.sequenceiq.cloudbreak.converter.v2;

import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.ConfigStrategy;
import com.sequenceiq.cloudbreak.api.model.ConnectedClusterRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ExecutorType;
import com.sequenceiq.cloudbreak.api.model.SharedServiceRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.stack.cluster.gateway.GatewayJson;
import com.sequenceiq.cloudbreak.api.model.v2.AmbariV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.ClusterV2Request;
import com.sequenceiq.cloudbreak.common.model.user.CloudbreakUser;
import com.sequenceiq.cloudbreak.converter.util.CloudStorageValidationUtil;
import com.sequenceiq.cloudbreak.service.sharedservice.SharedServiceConfigProvider;

public class ClusterV2RequestToClusterRequestConverterTest {

    @InjectMocks
    private ClusterV2RequestToClusterRequestConverter underTest;

    @Mock
    private SharedServiceConfigProvider sharedServiceConfigProvider;

    @Mock
    private CloudbreakUser user;

    @Mock
    private ConversionService conversionService;

    @Mock
    private CloudStorageValidationUtil cloudStorageValidationUtil;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConvertWhenRdsConfigNamesIsEmptyThenRelatedFieldShouldBeEmpty() {
        ClusterV2Request source = createClusterV2Request(Collections.emptySet(), null);

        ClusterRequest result = underTest.convert(source);

        Assert.assertTrue(result.getRdsConfigNames().isEmpty());
    }

    @Test
    public void testConvertWhenRdsConfigNamesIsNullThenRelatedFieldShouldBeEmpty() {
        ClusterV2Request source = createClusterV2Request(null, null);

        ClusterRequest result = underTest.convert(source);

        Assert.assertTrue(result.getRdsConfigNames().isEmpty());
    }

    @Test
    public void testConvertWhenRdsConfigNamesIsNotEmptyThenThePassedValuesShouldBeInTheReturnObject() {
        String firstRdsConfigName = "first";
        String secondRdsConfigName = "second";
        Set<String> rdsConfigNames = createRdsConfigNames(firstRdsConfigName, secondRdsConfigName);
        ClusterV2Request source = createClusterV2Request(rdsConfigNames, null);
        source.setAmbari(null);

        ClusterRequest result = underTest.convert(source);

        Assert.assertFalse(result.getRdsConfigNames().isEmpty());
        Assert.assertEquals(rdsConfigNames.size(), result.getRdsConfigNames().size());
        rdsConfigNames.forEach(s -> Assert.assertTrue(result.getRdsConfigNames().contains(s)));
    }

    @Test
    public void testConvertWhenHasAmbariRequestButSharedServiceIsNotConfiguredThenSharedServiceRelatedFieldsShouldntBeConfigured() {
        AmbariV2Request ambariV2Request = createAmbariV2Request();
        ClusterV2Request source = createClusterV2Request(Collections.emptySet(), ambariV2Request);
        when(sharedServiceConfigProvider.isConfigured(source)).thenReturn(false);

        ClusterRequest result = underTest.convert(source);

        Assert.assertEquals(source.getAmbari().getAmbariDatabaseDetails(), result.getAmbariDatabaseDetails());
        Assert.assertEquals(source.getAmbari().getAmbariRepoDetailsJson(), result.getAmbariRepoDetailsJson());
        Assert.assertEquals(source.getAmbari().getAmbariStackDetails(), result.getAmbariStackDetails());
        Assert.assertEquals(source.getAmbari().getBlueprintId(), result.getBlueprintId());
        Assert.assertEquals(source.getAmbari().getBlueprintName(), result.getBlueprintName());
    }

    @Test
    public void testConvertWhenRdsConfigNamesIsEmptyAndHasAmbariRequestAndSharedServiceIsConfiguredThenSharedServiceRelatedFieldsShouldtBeConfiguredCorrectly() {
        AmbariV2Request ambariV2Request = createAmbariV2Request();
        ClusterV2Request source = createClusterV2Request(Collections.emptySet(), ambariV2Request);
        when(sharedServiceConfigProvider.isConfigured(source)).thenReturn(true);

        ClusterRequest result = underTest.convert(source);

        Assert.assertEquals(source.getAmbari().getAmbariDatabaseDetails(), result.getAmbariDatabaseDetails());
        Assert.assertEquals(source.getAmbari().getAmbariRepoDetailsJson(), result.getAmbariRepoDetailsJson());
        Assert.assertEquals(source.getAmbari().getAmbariStackDetails(), result.getAmbariStackDetails());
        Assert.assertEquals(source.getAmbari().getBlueprintId(), result.getBlueprintId());
        Assert.assertEquals(source.getAmbari().getBlueprintName(), result.getBlueprintName());
        Assert.assertNotEquals(source.getAmbari().getConnectedCluster(), result.getConnectedCluster());
        Assert.assertEquals(source.getSharedService().getSharedCluster(), result.getConnectedCluster().getSourceClusterName());
    }

    @Test
    public void testConvertWhenEveryDataWhichDoesNotDependsOnAnyCheckingLogicHasSet() {
        ClusterV2Request source = createClusterV2Request(Collections.emptySet(), null);

        ClusterRequest result = underTest.convert(source);

        Assert.assertEquals(source.getExecutorType(), result.getExecutorType());
        Assert.assertEquals(source.getName(), result.getName());
        Assert.assertEquals(source.getProxyName(), result.getProxyName());
        Assert.assertEquals(source.getLdapConfigName(), result.getLdapConfigName());
        Assert.assertNotNull(result.getHostGroups());
        Assert.assertTrue(result.getHostGroups().isEmpty());
    }

    @Test
    public void testConvertWhenKerberosConfigNameHasGivenThenThisValueShouldBePassedToTheResultClusterRequest() {
        String kerberosConfigName = "someKerberosConfig";
        AmbariV2Request ambariV2Request = createAmbariV2Request();
        ambariV2Request.setKerberosConfigName(kerberosConfigName);
        ClusterV2Request source = createClusterV2Request(Collections.emptySet(), ambariV2Request);

        ClusterRequest result = underTest.convert(source);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getKerberosConfigName());
        Assert.assertEquals(kerberosConfigName, result.getKerberosConfigName());

    }

    private ClusterV2Request createClusterV2Request(Set<String> rdsConfigNames, AmbariV2Request ambariV2Request) {
        ClusterV2Request request = new ClusterV2Request();
        request.setExecutorType(ExecutorType.CONTAINER);
        request.setLdapConfigName("nameOfTheLdapConfig");
        request.setName("some name");
        request.setProxyName("proxy name");
        request.setSharedService(createSharedServiceRequest());
        request.setRdsConfigNames(rdsConfigNames == null ? Collections.emptySet() : rdsConfigNames);
        request.setAmbari(ambariV2Request);
        return request;
    }

    private SharedServiceRequest createSharedServiceRequest() {
        SharedServiceRequest request = new SharedServiceRequest();
        request.setSharedCluster("sharedCluster");
        return request;
    }

    private Set<String> createRdsConfigNames(String... names) {
        Set<String> rdsConfigNames = new LinkedHashSet<>(names.length);
        Collections.addAll(rdsConfigNames, names);
        return rdsConfigNames;
    }

    private AmbariV2Request createAmbariV2Request() {
        AmbariV2Request request = new AmbariV2Request();
        request.setAmbariDatabaseDetails(new AmbariDatabaseDetailsJson());
        request.setAmbariRepoDetailsJson(new AmbariRepoDetailsJson());
        request.setAmbariStackDetails(new AmbariStackDetailsJson());
        request.setBlueprintId(1L);
        request.setBlueprintName("blueprintName");
        request.setConfigStrategy(ConfigStrategy.ALWAYS_APPLY);
        request.setConnectedCluster(new ConnectedClusterRequest());
        request.setGateway(new GatewayJson());
        request.setKerberosConfigName(null);
        request.setPassword("somePwd");
        request.setUserName("someUserName");
        request.setValidateBlueprint(true);
        request.setAmbariSecurityMasterKey("masterKey");
        return request;
    }

}

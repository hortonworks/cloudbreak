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

import com.sequenceiq.cloudbreak.api.model.AmbariDatabaseDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariRepoDetailsJson;
import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson;
import com.sequenceiq.cloudbreak.api.model.BlueprintInputJson;
import com.sequenceiq.cloudbreak.api.model.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.ConfigStrategy;
import com.sequenceiq.cloudbreak.api.model.ConnectedClusterRequest;
import com.sequenceiq.cloudbreak.api.model.ExecutorType;
import com.sequenceiq.cloudbreak.api.model.FileSystemRequest;
import com.sequenceiq.cloudbreak.api.model.GatewayJson;
import com.sequenceiq.cloudbreak.api.model.KerberosRequest;
import com.sequenceiq.cloudbreak.api.model.SharedServiceRequest;
import com.sequenceiq.cloudbreak.api.model.v2.AmbariV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.ClusterV2Request;
import com.sequenceiq.cloudbreak.service.sharedservice.SharedServiceConfigProvider;

public class ClusterV2RequestToClusterRequestConverterTest {

    @InjectMocks
    private ClusterV2RequestToClusterRequestConverter underTest;

    @Mock
    private SharedServiceConfigProvider sharedServiceConfigProvider;

    @Before
    public void setUp() throws Exception {
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
        int blueprintInputQuantity = 2;
        AmbariV2Request ambariV2Request = createAmbariV2Request(blueprintInputQuantity);
        ClusterV2Request source = createClusterV2Request(Collections.emptySet(), ambariV2Request);
        when(sharedServiceConfigProvider.isConfigured(source)).thenReturn(false);

        ClusterRequest result = underTest.convert(source);

        Assert.assertEquals(source.getAmbari().getAmbariDatabaseDetails(), result.getAmbariDatabaseDetails());
        Assert.assertEquals(source.getAmbari().getAmbariRepoDetailsJson(), result.getAmbariRepoDetailsJson());
        Assert.assertEquals(source.getAmbari().getAmbariStackDetails(), result.getAmbariStackDetails());
        Assert.assertEquals(source.getAmbari().getBlueprintCustomProperties(), result.getBlueprintCustomProperties());
        Assert.assertEquals(source.getAmbari().getBlueprintId(), result.getBlueprintId());
        Assert.assertEquals(source.getAmbari().getBlueprintName(), result.getBlueprintName());
        Assert.assertFalse(result.getBlueprintInputs().isEmpty());
        Assert.assertEquals(ambariV2Request.getBlueprintInputs().size(), result.getBlueprintInputs().size());
        ambariV2Request.getBlueprintInputs().forEach(blueprintInputJson -> Assert.assertTrue(result.getBlueprintInputs().contains(blueprintInputJson)));
    }

    @Test
    public void testConvertWhenRdsConfigNamesIsEmptyAndHasAmbariRequestAndSharedServiceIsConfiguredThenSharedServiceRelatedFieldsShouldtBeConfiguredCorrectly() {
        int blueprintInputQuantity = 2;
        AmbariV2Request ambariV2Request = createAmbariV2Request(blueprintInputQuantity);
        ClusterV2Request source = createClusterV2Request(Collections.emptySet(), ambariV2Request);
        when(sharedServiceConfigProvider.isConfigured(source)).thenReturn(true);

        ClusterRequest result = underTest.convert(source);

        Assert.assertEquals(source.getAmbari().getAmbariDatabaseDetails(), result.getAmbariDatabaseDetails());
        Assert.assertEquals(source.getAmbari().getAmbariRepoDetailsJson(), result.getAmbariRepoDetailsJson());
        Assert.assertEquals(source.getAmbari().getAmbariStackDetails(), result.getAmbariStackDetails());
        Assert.assertEquals(source.getAmbari().getBlueprintCustomProperties(), result.getBlueprintCustomProperties());
        Assert.assertEquals(source.getAmbari().getBlueprintId(), result.getBlueprintId());
        Assert.assertEquals(source.getAmbari().getBlueprintName(), result.getBlueprintName());

        Assert.assertFalse(result.getBlueprintInputs().isEmpty());
        Assert.assertEquals(ambariV2Request.getBlueprintInputs().size(), result.getBlueprintInputs().size());
        ambariV2Request.getBlueprintInputs().forEach(blueprintInputJson -> Assert.assertTrue(result.getBlueprintInputs().contains(blueprintInputJson)));

        Assert.assertNotEquals(source.getAmbari().getConnectedCluster(), result.getConnectedCluster());
        Assert.assertEquals(source.getSharedService().getSharedCluster(), result.getConnectedCluster().getSourceClusterName());
    }

    @Test
    public void testConvertWhenEveryDataWhichDoesNotDependsOnAnyCheckingLogicHasSet() {
        ClusterV2Request source = createClusterV2Request(Collections.emptySet(), null);

        ClusterRequest result = underTest.convert(source);

        Assert.assertEquals(source.getExecutorType(), result.getExecutorType());
        Assert.assertEquals(source.getEmailNeeded(), result.getEmailNeeded());
        Assert.assertEquals(source.getEmailTo(), result.getEmailTo());
        Assert.assertEquals(source.getFileSystem(), result.getFileSystem());
        Assert.assertEquals(source.getName(), result.getName());
        Assert.assertEquals(source.getProxyName(), result.getProxyName());
        Assert.assertEquals(source.getLdapConfigName(), result.getLdapConfigName());
        Assert.assertNotNull(result.getHostGroups());
        Assert.assertTrue(result.getHostGroups().isEmpty());
    }

    private ClusterV2Request createClusterV2Request(Set<String> rdsConfigNames, AmbariV2Request ambariV2Request) {
        ClusterV2Request request = new ClusterV2Request();
        request.setEmailNeeded(true);
        request.setEmailTo("customemailaddress@someemailprovider.com");
        request.setExecutorType(ExecutorType.CONTAINER);
        request.setFileSystem(new FileSystemRequest());
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

    private AmbariV2Request createAmbariV2Request(int blueprintInputsQuantity) {
        AmbariV2Request request = new AmbariV2Request();
        request.setAmbariDatabaseDetails(new AmbariDatabaseDetailsJson());
        request.setAmbariRepoDetailsJson(new AmbariRepoDetailsJson());
        request.setAmbariStackDetails(new AmbariStackDetailsJson());
        request.setBlueprintCustomProperties("custom properties");
        request.setBlueprintId(1L);
        request.setBlueprintName("blueprintName");
        request.setBlueprintInputs(blueprintInputJsons(blueprintInputsQuantity));
        request.setConfigStrategy(ConfigStrategy.ALWAYS_APPLY);
        request.setConnectedCluster(new ConnectedClusterRequest());
        request.setEnableSecurity(true);
        request.setGateway(new GatewayJson());
        request.setKerberos(new KerberosRequest());
        request.setPassword("somePwd");
        request.setUserName("someUserName");
        request.setValidateBlueprint(true);
        request.setAmbariSecurityMasterKey("masterKey");
        return request;
    }

    private Set<BlueprintInputJson> blueprintInputJsons(int quantity) {
        if (quantity > 0) {
            Set<BlueprintInputJson> jsons = new LinkedHashSet<>(quantity);
            for (int i = 0; i < quantity; i++) {
                jsons.add(new BlueprintInputJson());
            }
            return jsons;
        } else {
            return Collections.emptySet();
        }
    }

}

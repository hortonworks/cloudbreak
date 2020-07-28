package com.sequenceiq.periscope.controller;

import static com.sequenceiq.periscope.common.MessageCode.AUTOSCALING_CONFIG_NOT_FOUND;
import static com.sequenceiq.periscope.common.MessageCode.AUTOSCALING_ENTITLEMENT_NOT_ENABLED;
import static com.sequenceiq.periscope.common.MessageCode.UNSUPPORTED_AUTOSCALING_HOSTGROUP;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.BadRequestException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.AutoscaleRecommendationV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.periscope.api.model.AlertType;
import com.sequenceiq.periscope.api.model.LoadAlertConfigurationRequest;
import com.sequenceiq.periscope.api.model.LoadAlertRequest;
import com.sequenceiq.periscope.api.model.ScalingPolicyRequest;
import com.sequenceiq.periscope.api.model.TimeAlertRequest;
import com.sequenceiq.periscope.converter.LoadAlertConfigurationRequestConverter;
import com.sequenceiq.periscope.converter.LoadAlertRequestConverter;
import com.sequenceiq.periscope.converter.LoadAlertResponseConverter;
import com.sequenceiq.periscope.converter.ScalingPolicyRequestConverter;
import com.sequenceiq.periscope.converter.TimeAlertRequestConverter;
import com.sequenceiq.periscope.converter.TimeAlertResponseConverter;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.LoadAlert;
import com.sequenceiq.periscope.domain.LoadAlertConfiguration;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.service.AlertService;
import com.sequenceiq.periscope.service.AutoscaleRecommendationService;
import com.sequenceiq.periscope.service.AutoscaleRestRequestThreadLocalService;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.DateService;
import com.sequenceiq.periscope.service.EntitlementValidationService;
import com.sequenceiq.periscope.service.NotFoundException;
import com.sequenceiq.periscope.service.configuration.ClusterProxyConfigurationService;

@RunWith(MockitoJUnitRunner.class)
public class AlertControllerTest {

    private static final String TEST_ACCOUNT_ID = "accid";

    private static final String TEST_USER_CRN = String.format("crn:cdp:iam:us-west-1:%s:user:mockuser@cloudera.com", TEST_ACCOUNT_ID);

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @InjectMocks
    private AlertController underTest;

    @Mock
    private AlertService alertService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private LoadAlertRequestConverter loadAlertRequestConverter;

    @Mock
    private LoadAlertResponseConverter loadAlertResponseConverter;

    @Mock
    private ScalingPolicyRequestConverter scalingPolicyRequestConverter;

    @Mock
    private LoadAlertConfigurationRequestConverter loadAlertConfigurationRequestConverter;

    @Mock
    private TimeAlertRequestConverter timeAlertRequestConverter;

    @Mock
    private TimeAlertResponseConverter timeAlertResponseConverter;

    @Mock
    private AutoscaleRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private ClusterProxyConfigurationService clusterProxyConfigurationService;

    @Mock
    private EntitlementValidationService entitlementValidationService;

    @Mock
    private AutoscaleRecommendationService recommendationService;

    @Mock
    private CloudbreakMessagesService messagesService;

    private DateService dateService = new DateService();

    private Long clusterId = 10L;

    private String tenant = "testTenant";

    private Long alertId = 20L;

    private Cluster aCluster;

    @BeforeClass
    public static void setupAll() {
        ThreadBasedUserCrnProvider.setUserCrn(TEST_USER_CRN);
    }

    @Before
    public void setup() {
        underTest.setDateService(dateService);
        aCluster = getACluster();
        when(entitlementValidationService.autoscalingEntitlementEnabled(anyString(), anyString(), anyString())).thenReturn(true);
        when(recommendationService.getAutoscaleRecommendations(anyString()))
                .thenReturn(new AutoscaleRecommendationV4Response(Set.of("compute"), Set.of("compute")));
        when(restRequestThreadLocalService.getCloudbreakTenant()).thenReturn(tenant);
        when(clusterService.findOneByClusterIdAndTenant(clusterId, tenant)).thenReturn(Optional.of(aCluster));
    }

    @Test
    public void testLoadAlertUpdateNotFound() {
        LoadAlertRequest request = getALoadAlertRequest();

        when(messagesService.getMessage(AUTOSCALING_CONFIG_NOT_FOUND,
                List.of(AlertType.LOAD, alertId, aCluster.getStackName()))).thenReturn("load.alert.not.found");

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage("load.alert.not.found");

        underTest.updateLoadAlert(clusterId, alertId, request);
    }

    @Test
    public void testLoadAlertUpdate() {
        LoadAlertRequest request = getALoadAlertRequest();
        LoadAlert alert = getALoadAlert();

        aCluster.setLoadAlerts(Set.of(alert));
        when(loadAlertRequestConverter.convert(request)).thenReturn(alert);

        underTest.updateLoadAlert(clusterId, alertId, request);
        verify(alertService).updateLoadAlert(anyLong(), anyLong(), any(LoadAlert.class));
    }

    @Test
    public void testLoadAlertCreate() {
        LoadAlertRequest request = getALoadAlertRequest();

        when(loadAlertRequestConverter.convert(request)).thenReturn(getALoadAlert());

        underTest.createLoadAlert(clusterId, request);
        verify(alertService).createLoadAlert(anyLong(), any(LoadAlert.class));
    }

    @Test
    public void testLoadAlertCreateWhenHostGroupNotSupported() {
        LoadAlertRequest request = getALoadAlertRequest();

        when(recommendationService.getAutoscaleRecommendations(anyString()))
                .thenReturn(new AutoscaleRecommendationV4Response(Set.of(""), Set.of("")));
        when(messagesService.getMessage(UNSUPPORTED_AUTOSCALING_HOSTGROUP,
                List.of("compute", AlertType.LOAD, aCluster.getStackName(), Set.of("")))).thenReturn("duplicate.hostgroup");

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("duplicate.hostgroup");

        underTest.createLoadAlert(clusterId, request);
    }

    @Test
    public void testAccountNotEntitledForPlatform() {
        LoadAlertRequest request = getALoadAlertRequest();
        aCluster.setCloudPlatform("Yarn");

        when(entitlementValidationService.autoscalingEntitlementEnabled(ThreadBasedUserCrnProvider.getUserCrn(),
                ThreadBasedUserCrnProvider.getAccountId(), "Yarn")).thenReturn(false);
        when(restRequestThreadLocalService.getCloudbreakTenant()).thenReturn(tenant);
        when(messagesService.getMessage(AUTOSCALING_ENTITLEMENT_NOT_ENABLED,
                List.of(aCluster.getCloudPlatform(), aCluster.getStackName()))).thenReturn("account.not.entitled.for.platform");

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("account.not.entitled.for.platform");

        underTest.createLoadAlert(clusterId, request);
    }

    @Test
    public void testTimeAlertCreate() {
        TimeAlertRequest request = new TimeAlertRequest();
        request.setCron("1 0 1 1 1 1");
        request.setTimeZone("GMT");
        ScalingPolicyRequest scalingPolicy = new ScalingPolicyRequest();
        scalingPolicy.setHostGroup("compute");
        request.setScalingPolicy(scalingPolicy);

        when(timeAlertRequestConverter.convert(request)).thenReturn(new TimeAlert());

        underTest.createTimeAlert(clusterId, request);
        verify(alertService).createTimeAlert(anyLong(), any(TimeAlert.class));
    }

    @Test
    public void testTimeAlertUpdateNotFound() {
        TimeAlertRequest request = new TimeAlertRequest();

        when(messagesService.getMessage(AUTOSCALING_CONFIG_NOT_FOUND,
                List.of(AlertType.TIME, alertId, aCluster.getStackName()))).thenReturn("time.alert.not.found");

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage("time.alert.not.found");

        underTest.updateTimeAlert(clusterId, alertId, request);
    }

    @Test
    public void testTimeAlertUpdate() {
        TimeAlertRequest request = new TimeAlertRequest();
        request.setCron("1 0 1 1 1 1");
        request.setTimeZone("GMT");
        ScalingPolicyRequest scalingPolicy = new ScalingPolicyRequest();
        scalingPolicy.setHostGroup("compute");
        request.setScalingPolicy(scalingPolicy);
        TimeAlert alert = new TimeAlert();
        alert.setId(alertId);

        aCluster.setTimeAlerts(Set.of(alert));
        when(timeAlertRequestConverter.convert(request)).thenReturn(alert);

        underTest.updateTimeAlert(clusterId, alertId, request);
        verify(alertService).updateTimeAlert(anyLong(), anyLong(), any(TimeAlert.class));
    }

    private LoadAlert getALoadAlert() {
        LoadAlert alert = new LoadAlert();
        alert.setId(alertId);
        LoadAlertConfiguration testConfiguration = new LoadAlertConfiguration();
        testConfiguration.setMaxResourceValue(200);
        testConfiguration.setMinResourceValue(0);
        alert.setLoadAlertConfiguration(testConfiguration);

        ScalingPolicy scalingPolicy = new ScalingPolicy();
        scalingPolicy.setHostGroup("compute");
        alert.setScalingPolicy(scalingPolicy);
        return alert;
    }

    private LoadAlertRequest getALoadAlertRequest() {
        LoadAlertRequest loadAlertRequest = new LoadAlertRequest();
        LoadAlertConfigurationRequest testConfiguration = new LoadAlertConfigurationRequest();
        testConfiguration.setMaxResourceValue(200);
        testConfiguration.setMinResourceValue(0);
        loadAlertRequest.setLoadAlertConfiguration(testConfiguration);
        ScalingPolicyRequest scalingPolicy = new ScalingPolicyRequest();
        scalingPolicy.setHostGroup("compute");
        loadAlertRequest.setScalingPolicy(scalingPolicy);
        return loadAlertRequest;
    }

    private Cluster getACluster() {
        Cluster cluster = new Cluster();
        cluster.setStackCrn("testcrn");
        cluster.setStackName("teststack");
        cluster.setCloudPlatform("AWS");
        cluster.setTunnel(Tunnel.CLUSTER_PROXY);
        return cluster;
    }
}

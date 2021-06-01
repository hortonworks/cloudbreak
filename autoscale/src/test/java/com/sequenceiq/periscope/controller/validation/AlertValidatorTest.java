package com.sequenceiq.periscope.controller.validation;

import static com.sequenceiq.periscope.common.MessageCode.AUTOSCALING_ENTITLEMENT_NOT_ENABLED;
import static com.sequenceiq.periscope.common.MessageCode.UNSUPPORTED_AUTOSCALING_HOSTGROUP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
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
import com.sequenceiq.cloudbreak.common.ScalingHardLimitsService;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.periscope.api.model.AdjustmentType;
import com.sequenceiq.periscope.api.model.AlertType;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterRequest;
import com.sequenceiq.periscope.api.model.LoadAlertRequest;
import com.sequenceiq.periscope.api.model.ScalingPolicyRequest;
import com.sequenceiq.periscope.api.model.TimeAlertRequest;
import com.sequenceiq.periscope.controller.AutoScaleClusterCommonService;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.service.AutoscaleRecommendationService;
import com.sequenceiq.periscope.service.AutoscaleRestRequestThreadLocalService;
import com.sequenceiq.periscope.service.DateService;
import com.sequenceiq.periscope.service.EntitlementValidationService;

@RunWith(MockitoJUnitRunner.class)
public class AlertValidatorTest {

    private static final String TEST_ACCOUNT_ID = "accid";

    private static final String TEST_USER_CRN = String.format("crn:cdp:iam:us-west-1:%s:user:mockuser@cloudera.com", TEST_ACCOUNT_ID);

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @InjectMocks
    private AlertValidator underTest;

    @Mock
    private EntitlementValidationService entitlementValidationService;

    @Mock
    private AutoscaleRecommendationService recommendationService;

    @Mock
    private CloudbreakMessagesService messagesService;

    @Mock
    private AutoScaleClusterCommonService asClusterCommonService;

    @Mock
    private ScalingHardLimitsService scalingHardLimitsService;

    @Mock
    private AutoscaleRestRequestThreadLocalService autoscaleRestRequestThreadLocalService;

    private DateService dateService = new DateService();

    private Cluster aCluster;

    @BeforeClass
    public static void setupAll() {
        ThreadBasedUserCrnProvider.setUserCrn(TEST_USER_CRN);
    }

    @Before
    public void setup() {
        underTest.setDateService(dateService);
        aCluster = getACluster();
        when(entitlementValidationService.autoscalingEntitlementEnabled(anyString(), anyString())).thenReturn(true);
        when(autoscaleRestRequestThreadLocalService.getCloudbreakTenant()).thenReturn("tenant");
        when(entitlementValidationService.scalingStepEntitlementEnabled(anyString())).thenReturn(true);
        when(scalingHardLimitsService.getMaxUpscaleStepInNodeCountWhenScalingStepEntitled()).thenCallRealMethod();
    }

    @Test
    public void testValidateEntitlementAndDisableIfNotEntitledWhenAccountNotEntitledForPlatform() {
        aCluster.setCloudPlatform("Yarn");
        aCluster.setAutoscalingEnabled(false);

        when(entitlementValidationService.autoscalingEntitlementEnabled(ThreadBasedUserCrnProvider.getAccountId(), "Yarn")).thenReturn(false);
        when(messagesService.getMessage(AUTOSCALING_ENTITLEMENT_NOT_ENABLED,
                List.of(aCluster.getCloudPlatform(), aCluster.getStackName()))).thenReturn("account.not.entitled.for.platform");

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("account.not.entitled.for.platform");

        underTest.validateEntitlementAndDisableIfNotEntitled(aCluster);
        verify(asClusterCommonService, never()).setAutoscaleState(aCluster.getId(), false);
    }

    @Test
    public void testValidateEntitlementAndDisableIfNotEntitledWhenAccountNotEntitledThenDisableAutoscaling() {
        aCluster.setCloudPlatform("AWS");

        when(entitlementValidationService.autoscalingEntitlementEnabled(ThreadBasedUserCrnProvider.getAccountId(), "AWS")).thenReturn(false);
        when(messagesService.getMessage(AUTOSCALING_ENTITLEMENT_NOT_ENABLED,
                List.of(aCluster.getCloudPlatform(), aCluster.getStackName()))).thenReturn("account.not.entitled.for.platform");

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("account.not.entitled.for.platform");

        underTest.validateEntitlementAndDisableIfNotEntitled(aCluster);
        verify(asClusterCommonService, times(1)).setAutoscaleState(aCluster.getId(), false);
    }

    @Test
    public void testValidateEntitlementAndDisableIfNotEntitledWhenAccountEntitledThenValidationSuccess() {
        aCluster.setCloudPlatform("AWS");

        when(entitlementValidationService.autoscalingEntitlementEnabled(ThreadBasedUserCrnProvider.getAccountId(), "AWS")).thenReturn(true);

        underTest.validateEntitlementAndDisableIfNotEntitled(aCluster);
        verify(asClusterCommonService, never()).setAutoscaleState(aCluster.getId(), false);
    }

    @Test
    public void testValidateScheduleWhenValidThenSuccess() {
        TimeAlertRequest request = new TimeAlertRequest();
        request.setCron("1 0 1 1 1 1");
        request.setTimeZone("GMT");

        underTest.validateSchedule(request);
    }

    @Test
    public void testValidateScheduleWhenInvalidCron() {
        TimeAlertRequest request = new TimeAlertRequest();
        request.setCron("2 22 22243 333");

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("Range exceeds maximum");

        underTest.validateSchedule(request);
    }

    @Test
    public void testValidateScheduleWhenInvalidTimeZone() {
        TimeAlertRequest request = new TimeAlertRequest();
        request.setCron("1 0 1 1 1 1");
        request.setTimeZone("GMT-4343");

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("Invalid ID for offset-based ZoneId: GMT-4343");

        underTest.validateSchedule(request);
    }

    @Test
    public void testLoadAlertCreateWhenHostGroupNotSupported() {
        Set requestHostGroups = Set.of("compute", "compute1");
        Set supportedHostGroups = Set.of("compute1");

        when(recommendationService.getAutoscaleRecommendations(anyString()))
                .thenReturn(new AutoscaleRecommendationV4Response(supportedHostGroups, supportedHostGroups));
        when(messagesService.getMessage(UNSUPPORTED_AUTOSCALING_HOSTGROUP,
                List.of(requestHostGroups, AlertType.LOAD, aCluster.getStackName(), supportedHostGroups)))
                .thenReturn("unsupported.hostgroup");

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("unsupported.hostgroup");

        underTest.validateSupportedHostGroup(aCluster, requestHostGroups, AlertType.LOAD);
    }

    @Test
    public void testTimeAlertCreateWhenHostGroupNotSupported() {
        Set requestHostGroups = Set.of("compute", "compute1");
        Set supportedHostGroups = Set.of("compute1");

        when(recommendationService.getAutoscaleRecommendations(anyString()))
                .thenReturn(new AutoscaleRecommendationV4Response(supportedHostGroups, supportedHostGroups));
        when(messagesService.getMessage(UNSUPPORTED_AUTOSCALING_HOSTGROUP,
                List.of(requestHostGroups, AlertType.TIME, aCluster.getStackName(), supportedHostGroups)))
                .thenReturn("unsupported.hostgroup");

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("unsupported.hostgroup");

        underTest.validateSupportedHostGroup(aCluster, requestHostGroups, AlertType.TIME);
    }

    @Test
    public void testLoadAlertCreateWhenHostGroupSupported() {
        Set requestHostGroups = Set.of("compute", "compute1");
        Set supportedHostGroups = Set.of("compute", "compute1", "compute3");

        when(recommendationService.getAutoscaleRecommendations(anyString()))
                .thenReturn(new AutoscaleRecommendationV4Response(supportedHostGroups, supportedHostGroups));

        underTest.validateSupportedHostGroup(aCluster, requestHostGroups, AlertType.LOAD);
    }

    @Test
    public void testTimeAlertCreateWhenHostGroupSupported() {
        Set requestHostGroups = Set.of("compute", "compute1");
        Set supportedHostGroups = Set.of("compute", "compute1");

        when(recommendationService.getAutoscaleRecommendations(anyString()))
                .thenReturn(new AutoscaleRecommendationV4Response(supportedHostGroups, supportedHostGroups));

        underTest.validateSupportedHostGroup(aCluster, requestHostGroups, AlertType.TIME);
    }

    @Test
    public void testValidateDistroXAutoscaleClusterRequestWhenValidLoadAlertRequest() {
        DistroXAutoscaleClusterRequest request = new DistroXAutoscaleClusterRequest();
        ScalingPolicyRequest sp1 = new ScalingPolicyRequest();
        sp1.setHostGroup("compute");
        LoadAlertRequest loadAlertRequest = new LoadAlertRequest();
        loadAlertRequest.setScalingPolicy(sp1);
        request.setLoadAlertRequests(List.of(loadAlertRequest));

        Set supportedHostGroups = Set.of("compute", "compute1", "compute3");
        when(recommendationService.getAutoscaleRecommendations(anyString()))
                .thenReturn(new AutoscaleRecommendationV4Response(supportedHostGroups, supportedHostGroups));

        underTest.validateDistroXAutoscaleClusterRequest(aCluster, request);
    }

    @Test
    public void testValidateDistroXAutoscaleClusterRequestWhenHostGroupNotSupported() {
        DistroXAutoscaleClusterRequest request = new DistroXAutoscaleClusterRequest();
        ScalingPolicyRequest sp1 = new ScalingPolicyRequest();
        sp1.setHostGroup("notsupported");
        LoadAlertRequest loadAlertRequest = new LoadAlertRequest();
        loadAlertRequest.setScalingPolicy(sp1);
        request.setLoadAlertRequests(List.of(loadAlertRequest));

        Set supportedHostGroups = Set.of("compute", "compute1", "compute3");
        when(recommendationService.getAutoscaleRecommendations(anyString()))
                .thenReturn(new AutoscaleRecommendationV4Response(supportedHostGroups, supportedHostGroups));
        when(messagesService.getMessage(UNSUPPORTED_AUTOSCALING_HOSTGROUP,
                List.of(Set.of("notsupported"), AlertType.LOAD, aCluster.getStackName(), supportedHostGroups)))
                .thenReturn("unsupported.hostgroup");

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("unsupported.hostgroup");

        underTest.validateDistroXAutoscaleClusterRequest(aCluster, request);
    }

    @Test
    public void testValidateDistroXAutoscaleClusterRequestWhenValidTimeAlertRequest() {
        DistroXAutoscaleClusterRequest request = new DistroXAutoscaleClusterRequest();
        List<TimeAlertRequest> timeAlertRequests = new ArrayList<>();
        List.of("compute", "compute", "compute1").forEach(
                hostGroup -> {
                    ScalingPolicyRequest sp1 = new ScalingPolicyRequest();
                    sp1.setHostGroup("compute");
                    TimeAlertRequest timeAlertRequest1 = new TimeAlertRequest();
                    timeAlertRequest1.setScalingPolicy(sp1);
                    timeAlertRequest1.setCron("1 0 1 1 1 1");
                    timeAlertRequest1.setTimeZone("GMT");
                    timeAlertRequests.add(timeAlertRequest1);
                }
        );

        request.setTimeAlertRequests(timeAlertRequests);

        Set supportedHostGroups = Set.of("compute", "compute1", "compute3");
        when(recommendationService.getAutoscaleRecommendations(anyString()))
                .thenReturn(new AutoscaleRecommendationV4Response(supportedHostGroups, supportedHostGroups));

        underTest.validateDistroXAutoscaleClusterRequest(aCluster, request);
    }

    @Test
    public void testValidateDistroXAutoscaleClusterRequestWhenInvalidTimeZone() {
        DistroXAutoscaleClusterRequest request = new DistroXAutoscaleClusterRequest();
        List<TimeAlertRequest> timeAlertRequests = new ArrayList<>();
        List.of("compute", "compute", "compute1").forEach(
                hostGroup -> {
                    ScalingPolicyRequest sp1 = new ScalingPolicyRequest();
                    sp1.setHostGroup("compute");
                    TimeAlertRequest timeAlertRequest1 = new TimeAlertRequest();
                    timeAlertRequest1.setScalingPolicy(sp1);
                    timeAlertRequest1.setCron("1 0 1 1 1 1");
                    timeAlertRequest1.setTimeZone("GMT");
                    timeAlertRequests.add(timeAlertRequest1);
                }
        );
        ScalingPolicyRequest sp2 = new ScalingPolicyRequest();
        sp2.setHostGroup("compute3");
        TimeAlertRequest timeAlertRequest2 = new TimeAlertRequest();
        timeAlertRequest2.setScalingPolicy(sp2);
        timeAlertRequest2.setCron("1 0 1 1 1 1");
        timeAlertRequest2.setTimeZone("GMT-434343");
        timeAlertRequests.add(timeAlertRequest2);

        request.setTimeAlertRequests(timeAlertRequests);

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("Invalid ID for offset-based ZoneId: GMT-4343");

        underTest.validateDistroXAutoscaleClusterRequest(aCluster, request);
    }

    @Test
    public void testValidateDistroXAutoscaleClusterRequestWhenInvalidHostGroup() {
        DistroXAutoscaleClusterRequest request = new DistroXAutoscaleClusterRequest();
        List<TimeAlertRequest> timeAlertRequests = new ArrayList<>();
        List.of("compute", "compute", "compute1").forEach(
                hostGroup -> {
                    ScalingPolicyRequest sp1 = new ScalingPolicyRequest();
                    sp1.setHostGroup("compute");
                    TimeAlertRequest timeAlertRequest1 = new TimeAlertRequest();
                    timeAlertRequest1.setScalingPolicy(sp1);
                    timeAlertRequest1.setCron("1 0 1 1 1 1");
                    timeAlertRequest1.setTimeZone("GMT");
                    timeAlertRequests.add(timeAlertRequest1);
                }
        );
        ScalingPolicyRequest sp2 = new ScalingPolicyRequest();
        sp2.setHostGroup("computeNotSupported");
        TimeAlertRequest timeAlertRequest2 = new TimeAlertRequest();
        timeAlertRequest2.setScalingPolicy(sp2);
        timeAlertRequest2.setCron("1 0 1 1 1 1");
        timeAlertRequest2.setTimeZone("GMT");
        timeAlertRequests.add(timeAlertRequest2);
        request.setTimeAlertRequests(timeAlertRequests);

        Set supportedHostGroups = Set.of("compute", "compute1", "compute3");
        when(recommendationService.getAutoscaleRecommendations(anyString()))
                .thenReturn(new AutoscaleRecommendationV4Response(supportedHostGroups, supportedHostGroups));
        when(messagesService.getMessage(UNSUPPORTED_AUTOSCALING_HOSTGROUP,
                List.of(Set.of("compute", "computeNotSupported"), AlertType.TIME, aCluster.getStackName(), supportedHostGroups)))
                .thenReturn("unsupported.hostgroup");

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("unsupported.hostgroup");

        underTest.validateDistroXAutoscaleClusterRequest(aCluster, request);
    }

    @Test
    public void testValidateScalingAdjustmentWhenScalingStepEntitledAndScalingBeyond200() {
        ScalingPolicyRequest scalingPolicyRequest = new ScalingPolicyRequest();
        scalingPolicyRequest.setAdjustmentType(AdjustmentType.NODE_COUNT);
        scalingPolicyRequest.setScalingAdjustment(250);

        when(entitlementValidationService.scalingStepEntitlementEnabled("tenant")).thenReturn(true);
        when(scalingHardLimitsService.getMaxUpscaleStepInNodeCountWhenScalingStepEntitled()).thenReturn(200);
        when(scalingHardLimitsService.isViolatingMaxUpscaleStepInNodeCount(anyInt(), anyInt())).thenCallRealMethod();

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateScalingAdjustment(scalingPolicyRequest));

        assertEquals("Maximum upscale step is 200 node(s)", exception.getMessage());
    }

    @Test
    public void testValidateScalingAdjustmentWhenScalingStepEntitledAndValidScaling() {
        ScalingPolicyRequest scalingPolicyRequest = new ScalingPolicyRequest();
        scalingPolicyRequest.setAdjustmentType(AdjustmentType.NODE_COUNT);
        scalingPolicyRequest.setScalingAdjustment(100);

        when(entitlementValidationService.scalingStepEntitlementEnabled("tenant")).thenReturn(true);
        when(scalingHardLimitsService.getMaxUpscaleStepInNodeCountWhenScalingStepEntitled()).thenReturn(200);
        when(scalingHardLimitsService.isViolatingMaxUpscaleStepInNodeCount(anyInt(), anyInt())).thenCallRealMethod();

        underTest.validateScalingAdjustment(scalingPolicyRequest);
    }

    @Test
    public void testValidateScalingAdjustmentWhenScalingStepNotEntitledAndScalingBeyond200() {
        ScalingPolicyRequest scalingPolicyRequest = new ScalingPolicyRequest();
        scalingPolicyRequest.setAdjustmentType(AdjustmentType.NODE_COUNT);
        scalingPolicyRequest.setScalingAdjustment(150);

        when(entitlementValidationService.scalingStepEntitlementEnabled("tenant")).thenReturn(false);
        when(scalingHardLimitsService.getMaxUpscaleStepInNodeCount()).thenReturn(100);
        when(scalingHardLimitsService.isViolatingMaxUpscaleStepInNodeCount(anyInt(), anyInt())).thenCallRealMethod();

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.validateScalingAdjustment(scalingPolicyRequest));

        assertEquals("Maximum upscale step is 100 node(s)", exception.getMessage());
    }

    @Test
    public void testValidateScalingAdjustmentWhenScalingStepNotEntitledAndValidScaling() {
        ScalingPolicyRequest scalingPolicyRequest = new ScalingPolicyRequest();
        scalingPolicyRequest.setAdjustmentType(AdjustmentType.NODE_COUNT);
        scalingPolicyRequest.setScalingAdjustment(50);

        when(entitlementValidationService.scalingStepEntitlementEnabled("tenant")).thenReturn(false);
        when(scalingHardLimitsService.getMaxUpscaleStepInNodeCount()).thenReturn(100);
        when(scalingHardLimitsService.isViolatingMaxUpscaleStepInNodeCount(anyInt(), anyInt())).thenCallRealMethod();

        underTest.validateScalingAdjustment(scalingPolicyRequest);
    }

    private Cluster getACluster() {
        Cluster cluster = new Cluster();
        cluster.setStackCrn("testcrn");
        cluster.setStackName("teststack");
        cluster.setCloudPlatform("AWS");
        cluster.setTunnel(Tunnel.CLUSTER_PROXY);
        cluster.setAutoscalingEnabled(true);
        return cluster;
    }
}

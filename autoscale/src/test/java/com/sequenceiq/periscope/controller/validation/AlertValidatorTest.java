package com.sequenceiq.periscope.controller.validation;

import static com.sequenceiq.periscope.common.MessageCode.AUTOSCALING_CLUSTER_LIMIT_EXCEEDED;
import static com.sequenceiq.periscope.common.MessageCode.AUTOSCALING_ENTITLEMENT_NOT_ENABLED;
import static com.sequenceiq.periscope.common.MessageCode.AUTOSCALING_STOP_START_ENTITLEMENT_NOT_ENABLED;
import static com.sequenceiq.periscope.common.MessageCode.UNSUPPORTED_AUTOSCALING_HOSTGROUP;
import static com.sequenceiq.periscope.common.MessageCode.VALIDATION_TIME_STOP_START_UNSUPPORTED;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.ws.rs.BadRequestException;

import org.junit.Before;
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
import com.sequenceiq.periscope.api.model.AdjustmentType;
import com.sequenceiq.periscope.api.model.AlertType;
import com.sequenceiq.periscope.api.model.AutoscaleClusterState;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterRequest;
import com.sequenceiq.periscope.api.model.LoadAlertConfigurationRequest;
import com.sequenceiq.periscope.api.model.LoadAlertRequest;
import com.sequenceiq.periscope.api.model.ScalingPolicyRequest;
import com.sequenceiq.periscope.api.model.TimeAlertRequest;
import com.sequenceiq.periscope.controller.AutoScaleClusterCommonService;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.service.AutoscaleRecommendationService;
import com.sequenceiq.periscope.service.DateService;
import com.sequenceiq.periscope.service.EntitlementValidationService;
import com.sequenceiq.periscope.service.configuration.LimitsConfigurationService;

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
    private LimitsConfigurationService limitsConfigurationService;

    private DateService dateService = new DateService();

    private Cluster aCluster;

    @Before
    public void setup() {
        underTest.setDateService(dateService);
        aCluster = getACluster();
        when(entitlementValidationService.autoscalingEntitlementEnabled(anyString(), anyString())).thenReturn(true);
    }

    @Test
    public void testValidateEntitlementAndDisableIfNotEntitledWhenAccountNotEntitledForPlatform() {
        aCluster.setCloudPlatform("Yarn");
        aCluster.setAutoscalingEnabled(false);

        when(entitlementValidationService.autoscalingEntitlementEnabled(TEST_ACCOUNT_ID, "Yarn")).thenReturn(false);
        when(messagesService.getMessage(AUTOSCALING_ENTITLEMENT_NOT_ENABLED,
                List.of(aCluster.getCloudPlatform(), aCluster.getStackName()))).thenReturn("account.not.entitled.for.platform");

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("account.not.entitled.for.platform");

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validateEntitlementAndDisableIfNotEntitled(aCluster));
        verify(asClusterCommonService, never()).setAutoscaleState(aCluster.getId(), false);
    }

    @Test
    public void testValidateEntitlementAndDisableIfNotEntitledWhenAccountNotEntitledThenDisableAutoscaling() {
        aCluster.setCloudPlatform("AWS");

        when(entitlementValidationService.autoscalingEntitlementEnabled(TEST_ACCOUNT_ID, "AWS")).thenReturn(false);
        when(messagesService.getMessage(AUTOSCALING_ENTITLEMENT_NOT_ENABLED,
                List.of(aCluster.getCloudPlatform(), aCluster.getStackName()))).thenReturn("account.not.entitled.for.platform");

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("account.not.entitled.for.platform");

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validateEntitlementAndDisableIfNotEntitled(aCluster));
        verify(asClusterCommonService, times(1)).setAutoscaleState(aCluster.getId(), false);
    }

    @Test
    public void testValidateEntitlementAndDisableIfNotEntitledWhenAccountEntitledThenValidationSuccess() {
        aCluster.setCloudPlatform("AWS");

        when(entitlementValidationService.autoscalingEntitlementEnabled(TEST_ACCOUNT_ID, "AWS")).thenReturn(true);

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validateEntitlementAndDisableIfNotEntitled(aCluster));
        verify(asClusterCommonService, never()).setAutoscaleState(aCluster.getId(), false);
    }

    @Test
    public void testValidateStopStartEntitlementNotEnabledForAccount() {
        aCluster.setCloudPlatform("AWS");

        when(entitlementValidationService.stopStartAutoscalingEntitlementEnabled(TEST_ACCOUNT_ID, "AWS")).thenReturn(false);
        when(messagesService.getMessage(AUTOSCALING_STOP_START_ENTITLEMENT_NOT_ENABLED,
                List.of(aCluster.getCloudPlatform(), aCluster.getStackName()))).thenReturn("account.not.entitled");

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("account.not.entitled");

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validateStopStartEntitlementAndDisableIfNotEntitled(aCluster));
        verify(asClusterCommonService, times(1)).setStopStartScalingState(aCluster.getId(), false);
    }

    @Test
    public void testValidateStopStartEntitlementEnabledForAccount() {
        aCluster.setCloudPlatform("AWS");

        when(entitlementValidationService.stopStartAutoscalingEntitlementEnabled(TEST_ACCOUNT_ID, "AWS")).thenReturn(true);

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validateStopStartEntitlementAndDisableIfNotEntitled(aCluster));
        verify(asClusterCommonService, never()).setStopStartScalingState(aCluster.getId(), false);
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
        expectedException.expectMessage("Cron expression must consist of 6 fields (found 4 in \"2 22 22243 333\")");

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
        Set<String> requestHostGroups = Set.of("compute", "compute1");
        Set<String> supportedHostGroups = Set.of("compute1");

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
        Set<String> requestHostGroups = Set.of("compute", "compute1");
        Set<String> supportedHostGroups = Set.of("compute1");

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
    public void testTimeAlertCreateWhenStopStartEnabledInRequest() {
        DistroXAutoscaleClusterRequest autoscaleClusterRequest = new DistroXAutoscaleClusterRequest();
        List<TimeAlertRequest> timeAlerts = new ArrayList<>();
        List.of("compute", "compute", "compute1").forEach(
                hostGroup -> {
                    ScalingPolicyRequest sp1 = new ScalingPolicyRequest();
                    sp1.setHostGroup("compute");
                    TimeAlertRequest timeAlertRequest1 = new TimeAlertRequest();
                    timeAlertRequest1.setScalingPolicy(sp1);
                    timeAlertRequest1.setCron("1 0 1 1 1 1");
                    timeAlertRequest1.setTimeZone("GMT");
                    timeAlerts.add(timeAlertRequest1);
                }
        );
        autoscaleClusterRequest.setUseStopStartMechanism(Boolean.TRUE);
        autoscaleClusterRequest.setTimeAlertRequests(timeAlerts);

        when(messagesService.getMessage(VALIDATION_TIME_STOP_START_UNSUPPORTED))
                .thenReturn("Schedule-Based Autoscaling does not support the stop / start scaling mechanism.");

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("Schedule-Based Autoscaling does not support the stop / start scaling mechanism.");

        underTest.validateScheduleWithStopStart(aCluster, autoscaleClusterRequest);
    }

    @Test
    public void testTimeAlertCreateWhenStopStartEnabledInClusterEntity() {
        DistroXAutoscaleClusterRequest autoscaleClusterRequest = new DistroXAutoscaleClusterRequest();
        List<TimeAlertRequest> timeAlerts = new ArrayList<>();
        List.of("compute", "compute", "compute1").forEach(
                hostGroup -> {
                    ScalingPolicyRequest sp1 = new ScalingPolicyRequest();
                    sp1.setHostGroup("compute");
                    TimeAlertRequest timeAlertRequest1 = new TimeAlertRequest();
                    timeAlertRequest1.setScalingPolicy(sp1);
                    timeAlertRequest1.setCron("1 0 1 1 1 1");
                    timeAlertRequest1.setTimeZone("GMT");
                    timeAlerts.add(timeAlertRequest1);
                }
        );
        aCluster.setStopStartScalingEnabled(Boolean.TRUE);
        autoscaleClusterRequest.setTimeAlertRequests(timeAlerts);

        when(messagesService.getMessage(VALIDATION_TIME_STOP_START_UNSUPPORTED))
                .thenReturn("Schedule-Based Autoscaling does not support the stop / start scaling mechanism.");

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("Schedule-Based Autoscaling does not support the stop / start scaling mechanism.");

        underTest.validateScheduleWithStopStart(aCluster, autoscaleClusterRequest);
    }

    @Test
    public void testEnableStopStartWithTimeAlertPreCreated() {
        AutoscaleClusterState autoscaleState = new AutoscaleClusterState();
        autoscaleState.setUseStopStartMechanism(Boolean.TRUE);
        TimeAlert timeAlert = new TimeAlert();
        timeAlert.setTimeZone("Asia/Calcutta");
        timeAlert.setCluster(aCluster);
        timeAlert.setCron("1 0 1 1 1 1");
        aCluster.setTimeAlerts(Set.of(timeAlert));

        when(messagesService.getMessage(VALIDATION_TIME_STOP_START_UNSUPPORTED))
                .thenReturn("Schedule-Based Autoscaling does not support the stop / start scaling mechanism.");

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("Schedule-Based Autoscaling does not support the stop / start scaling mechanism.");

        underTest.validateScheduleWithStopStart(aCluster, autoscaleState);
    }

    @Test
    public void testEnableStopStartInRequestWithTimeAlertPreCreated() {
        DistroXAutoscaleClusterRequest autoscaleRequest = new DistroXAutoscaleClusterRequest();
        autoscaleRequest.setEnableAutoscaling(Boolean.TRUE);
        autoscaleRequest.setUseStopStartMechanism(Boolean.TRUE);
        TimeAlert timeAlert = new TimeAlert();
        timeAlert.setTimeZone("Asia/Calcutta");
        timeAlert.setCluster(aCluster);
        timeAlert.setCron("1 0 1 1 1 1");
        aCluster.setTimeAlerts(Set.of(timeAlert));

        when(messagesService.getMessage(VALIDATION_TIME_STOP_START_UNSUPPORTED))
                .thenReturn("Schedule-Based Autoscaling does not support the stop / start scaling mechanism.");

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("Schedule-Based Autoscaling does not support the stop / start scaling mechanism.");

        underTest.validateScheduleWithStopStart(aCluster, autoscaleRequest);
    }

    @Test
    public void testCreateTimeAlertSuccessWithStopStartDisabled() {
        DistroXAutoscaleClusterRequest autoscaleClusterRequest = new DistroXAutoscaleClusterRequest();
        List<TimeAlertRequest> timeAlerts = new ArrayList<>();
        List.of("compute", "compute", "compute1").forEach(
                hostGroup -> {
                    ScalingPolicyRequest sp1 = new ScalingPolicyRequest();
                    sp1.setHostGroup("compute");
                    TimeAlertRequest timeAlertRequest1 = new TimeAlertRequest();
                    timeAlertRequest1.setScalingPolicy(sp1);
                    timeAlertRequest1.setCron("1 0 1 1 1 1");
                    timeAlertRequest1.setTimeZone("GMT");
                    timeAlerts.add(timeAlertRequest1);
                }
        );
        autoscaleClusterRequest.setUseStopStartMechanism(Boolean.FALSE);
        autoscaleClusterRequest.setTimeAlertRequests(timeAlerts);

        assertDoesNotThrow(() -> underTest.validateScheduleWithStopStart(aCluster, autoscaleClusterRequest));
    }

    @Test
    public void testEnableStopStartSuccessWithNoTimeAlertPreCreated() {
        AutoscaleClusterState autoscaleState = new AutoscaleClusterState();
        aCluster.setTimeAlerts(Collections.emptySet());
        autoscaleState.setUseStopStartMechanism(Boolean.TRUE);

        assertDoesNotThrow(() -> underTest.validateScheduleWithStopStart(aCluster, autoscaleState));
    }

    @Test
    public void testCreateTimeAlertSuccessIfStopStartDisabledInRequestButEnabledInClusterEntity() {
        DistroXAutoscaleClusterRequest autoscaleClusterRequest = new DistroXAutoscaleClusterRequest();
        List<TimeAlertRequest> timeAlerts = new ArrayList<>();
        List.of("compute", "compute", "compute1").forEach(
                hostGroup -> {
                    ScalingPolicyRequest sp1 = new ScalingPolicyRequest();
                    sp1.setHostGroup("compute");
                    TimeAlertRequest timeAlertRequest1 = new TimeAlertRequest();
                    timeAlertRequest1.setScalingPolicy(sp1);
                    timeAlertRequest1.setCron("1 0 1 1 1 1");
                    timeAlertRequest1.setTimeZone("GMT");
                    timeAlerts.add(timeAlertRequest1);
                }
        );
        autoscaleClusterRequest.setUseStopStartMechanism(Boolean.FALSE);
        autoscaleClusterRequest.setTimeAlertRequests(timeAlerts);
        aCluster.setStopStartScalingEnabled(Boolean.TRUE);

        assertDoesNotThrow(() -> underTest.validateScheduleWithStopStart(aCluster, autoscaleClusterRequest));
    }

    @Test
    public void testLoadAlertCreateWhenHostGroupSupported() {
        Set<String> requestHostGroups = Set.of("compute", "compute1");
        Set<String> supportedHostGroups = Set.of("compute", "compute1", "compute3");

        when(recommendationService.getAutoscaleRecommendations(anyString()))
                .thenReturn(new AutoscaleRecommendationV4Response(supportedHostGroups, supportedHostGroups));

        underTest.validateSupportedHostGroup(aCluster, requestHostGroups, AlertType.LOAD);
    }

    @Test
    public void testTimeAlertCreateWhenHostGroupSupported() {
        Set<String> requestHostGroups = Set.of("compute", "compute1");
        Set<String> supportedHostGroups = Set.of("compute", "compute1");

        when(recommendationService.getAutoscaleRecommendations(anyString()))
                .thenReturn(new AutoscaleRecommendationV4Response(supportedHostGroups, supportedHostGroups));

        underTest.validateSupportedHostGroup(aCluster, requestHostGroups, AlertType.TIME);
    }

    @Test
    public void testValidateDistroXAutoscaleClusterRequestWhenValidLoadAlertRequest() {
        DistroXAutoscaleClusterRequest request = new DistroXAutoscaleClusterRequest();
        ScalingPolicyRequest sp1 = new ScalingPolicyRequest();
        sp1.setHostGroup("compute");
        LoadAlertConfigurationRequest loadAlertConfigurationRequest = new LoadAlertConfigurationRequest();
        loadAlertConfigurationRequest.setMaxResourceValue(10);
        LoadAlertRequest loadAlertRequest = new LoadAlertRequest();
        loadAlertRequest.setScalingPolicy(sp1);
        loadAlertRequest.setLoadAlertConfiguration(loadAlertConfigurationRequest);
        request.setLoadAlertRequests(List.of(loadAlertRequest));

        Set<String> supportedHostGroups = Set.of("compute", "compute1", "compute3");
        when(recommendationService.getAutoscaleRecommendations(anyString()))
                .thenReturn(new AutoscaleRecommendationV4Response(supportedHostGroups, supportedHostGroups));

        underTest.validateDistroXAutoscaleClusterRequest(aCluster, request);
    }

    @Test
    public void testValidateDistroXAutoscaleClusterRequestWhenHostGroupNotSupported() {
        DistroXAutoscaleClusterRequest request = new DistroXAutoscaleClusterRequest();
        ScalingPolicyRequest sp1 = new ScalingPolicyRequest();
        sp1.setHostGroup("notsupported");
        LoadAlertConfigurationRequest loadAlertConfigurationRequest = new LoadAlertConfigurationRequest();
        loadAlertConfigurationRequest.setMaxResourceValue(10);
        LoadAlertRequest loadAlertRequest = new LoadAlertRequest();
        loadAlertRequest.setScalingPolicy(sp1);
        loadAlertRequest.setLoadAlertConfiguration(loadAlertConfigurationRequest);
        request.setLoadAlertRequests(List.of(loadAlertRequest));

        Set<String> supportedHostGroups = Set.of("compute", "compute1", "compute3");
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
    public void testValidateDistroXAutoscaleClusterRequestWhenClusterSizeExceeded() {
        DistroXAutoscaleClusterRequest request = new DistroXAutoscaleClusterRequest();
        ScalingPolicyRequest sp1 = new ScalingPolicyRequest();
        sp1.setAdjustmentType(AdjustmentType.LOAD_BASED);
        sp1.setHostGroup("compute");
        LoadAlertConfigurationRequest loadAlertConfigurationRequest = new LoadAlertConfigurationRequest();
        loadAlertConfigurationRequest.setMaxResourceValue(500);
        LoadAlertRequest loadAlertRequest = new LoadAlertRequest();
        loadAlertRequest.setScalingPolicy(sp1);
        loadAlertRequest.setLoadAlertConfiguration(loadAlertConfigurationRequest);
        request.setLoadAlertRequests(List.of(loadAlertRequest));

        when(limitsConfigurationService.getMaxNodeCountLimit()).thenReturn(400);
        when(messagesService.getMessage(AUTOSCALING_CLUSTER_LIMIT_EXCEEDED, List.of(400)))
                .thenReturn("cluster limit exceeded");

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("cluster limit exceeded");

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

        Set<String> supportedHostGroups = Set.of("compute", "compute1", "compute3");
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

        Set<String> supportedHostGroups = Set.of("compute", "compute1", "compute3");
        when(recommendationService.getAutoscaleRecommendations(anyString()))
                .thenReturn(new AutoscaleRecommendationV4Response(supportedHostGroups, supportedHostGroups));
        when(messagesService.getMessage(UNSUPPORTED_AUTOSCALING_HOSTGROUP,
                List.of(Set.of("compute", "computeNotSupported"), AlertType.TIME, aCluster.getStackName(), supportedHostGroups)))
                .thenReturn("unsupported.hostgroup");

        expectedException.expect(BadRequestException.class);
        expectedException.expectMessage("unsupported.hostgroup");

        underTest.validateDistroXAutoscaleClusterRequest(aCluster, request);
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

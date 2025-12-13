package com.sequenceiq.periscope.controller.validation;

import static com.sequenceiq.periscope.common.MessageCode.AUTOSCALE_CLUSTER_NOT_AVAILABLE;
import static com.sequenceiq.periscope.common.MessageCode.AUTOSCALING_CLUSTER_LIMIT_EXCEEDED;
import static com.sequenceiq.periscope.common.MessageCode.AUTOSCALING_ENTITLEMENT_NOT_ENABLED;
import static com.sequenceiq.periscope.common.MessageCode.AUTOSCALING_STOP_START_ENTITLEMENT_NOT_ENABLED;
import static com.sequenceiq.periscope.common.MessageCode.UNSUPPORTED_AUTOSCALING_HOSTGROUP;
import static com.sequenceiq.periscope.common.MessageCode.VALIDATION_TIME_STOP_START_UNSUPPORTED;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import jakarta.ws.rs.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.AutoscaleRecommendationV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.periscope.api.model.AdjustmentType;
import com.sequenceiq.periscope.api.model.AlertType;
import com.sequenceiq.periscope.api.model.AutoscaleClusterState;
import com.sequenceiq.periscope.api.model.ClusterState;
import com.sequenceiq.periscope.api.model.DistroXAutoscaleClusterRequest;
import com.sequenceiq.periscope.api.model.LoadAlertConfigurationRequest;
import com.sequenceiq.periscope.api.model.LoadAlertRequest;
import com.sequenceiq.periscope.api.model.ScalingPolicyRequest;
import com.sequenceiq.periscope.api.model.TimeAlertRequest;
import com.sequenceiq.periscope.controller.AutoScaleClusterCommonService;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.ScalingPolicy;
import com.sequenceiq.periscope.domain.TimeAlert;
import com.sequenceiq.periscope.service.AutoscaleRecommendationService;
import com.sequenceiq.periscope.service.DateService;
import com.sequenceiq.periscope.service.EntitlementValidationService;
import com.sequenceiq.periscope.service.configuration.LimitsConfigurationService;

@ExtendWith(MockitoExtension.class)
class AlertValidatorTest {

    private static final String TEST_ACCOUNT_ID = "accid";

    private static final String TEST_USER_CRN = String.format("crn:cdp:iam:us-west-1:%s:user:mockuser@cloudera.com", TEST_ACCOUNT_ID);

    private static final String TEST_STACK_CRN = String.format("crn:cdp:datahub:us-west-1:%s:cluster:cluster", TEST_ACCOUNT_ID);

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

    @BeforeEach
    void setup() {
        underTest.setDateService(dateService);
        aCluster = getACluster();
        lenient().when(entitlementValidationService.autoscalingEntitlementEnabled(anyString(), anyString())).thenReturn(true);
    }

    @Test
    void testValidateEntitlementAndDisableIfNotEntitledWhenAccountNotEntitledForPlatform() {
        aCluster.setCloudPlatform("Yarn");
        aCluster.setAutoscalingEnabled(false);

        when(entitlementValidationService.autoscalingEntitlementEnabled(TEST_ACCOUNT_ID, "Yarn")).thenReturn(false);
        when(messagesService.getMessage(AUTOSCALING_ENTITLEMENT_NOT_ENABLED,
                List.of(aCluster.getCloudPlatform(), aCluster.getStackName()))).thenReturn("account.not.entitled.for.platform");

        assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validateEntitlementAndDisableIfNotEntitled(aCluster)),
                "account.not.entitled.for.platform");

        verify(asClusterCommonService, never()).setAutoscaleState(aCluster.getId(), false);
    }

    @Test
    void testValidateEntitlementAndDisableIfNotEntitledWhenAccountNotEntitledThenDisableAutoscaling() {
        aCluster.setCloudPlatform("AWS");

        when(entitlementValidationService.autoscalingEntitlementEnabled(TEST_ACCOUNT_ID, "AWS")).thenReturn(false);
        when(messagesService.getMessage(AUTOSCALING_ENTITLEMENT_NOT_ENABLED,
                List.of(aCluster.getCloudPlatform(), aCluster.getStackName()))).thenReturn("account.not.entitled.for.platform");

        assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validateEntitlementAndDisableIfNotEntitled(aCluster)),
                "account.not.entitled.for.platform");

        verify(asClusterCommonService, times(1)).setAutoscaleState(aCluster.getId(), false);
    }

    @Test
    void testValidateEntitlementAndDisableIfNotEntitledWhenAccountEntitledThenValidationSuccess() {
        aCluster.setCloudPlatform("AWS");

        when(entitlementValidationService.autoscalingEntitlementEnabled(TEST_ACCOUNT_ID, "AWS")).thenReturn(true);

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validateEntitlementAndDisableIfNotEntitled(aCluster));
        verify(asClusterCommonService, never()).setAutoscaleState(aCluster.getId(), false);
    }

    @Test
    void testValidateIfStackIsNotAvailable() {
        aCluster = getACluster();
        aCluster.setState(ClusterState.SUSPENDED);

        when(messagesService.getMessage(AUTOSCALE_CLUSTER_NOT_AVAILABLE, List.of("teststack"))).thenReturn("autoscale.cluster.not.available");

        assertThrows(BadRequestException.class, () -> underTest.validateIfStackIsAvailable(aCluster), "autoscale.cluster.not.available");
    }

    @Test
    void testValidateIfStackIsAvailable() {
        aCluster = getACluster();
        aCluster.setState(ClusterState.RUNNING);

        assertDoesNotThrow(() -> underTest.validateIfStackIsAvailable(aCluster));
    }

    @Test
    void testValidateStopStartEntitlementNotEnabledForAccount() {
        aCluster.setCloudPlatform("AWS");

        when(entitlementValidationService.stopStartAutoscalingEntitlementEnabled(TEST_ACCOUNT_ID, "AWS")).thenReturn(false);
        when(messagesService.getMessage(AUTOSCALING_STOP_START_ENTITLEMENT_NOT_ENABLED,
                List.of(aCluster.getCloudPlatform(), aCluster.getStackName()))).thenReturn("account.not.entitled");

        assertThrows(BadRequestException.class,
                () -> ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validateStopStartEntitlementAndDisableIfNotEntitled(aCluster)),
                "account.not.entitled");
    }

    @Test
    void testValidateStopStartEntitlementEnabledForAccount() {
        aCluster.setCloudPlatform("AWS");

        when(entitlementValidationService.stopStartAutoscalingEntitlementEnabled(TEST_ACCOUNT_ID, "AWS")).thenReturn(true);

        ThreadBasedUserCrnProvider.doAs(TEST_USER_CRN, () -> underTest.validateStopStartEntitlementAndDisableIfNotEntitled(aCluster));
        verify(asClusterCommonService, never()).setStopStartScalingState(aCluster.getId(), false, false, true);
    }

    @Test
    void testValidateScheduleWhenValidThenSuccess() {
        TimeAlertRequest request = new TimeAlertRequest();
        request.setCron("1 0 1 1 1 1");
        request.setTimeZone("GMT");

        underTest.validateSchedule(request);
    }

    @Test
    void testValidateScheduleWhenInvalidCron() {
        TimeAlertRequest request = new TimeAlertRequest();
        request.setCron("2 22 22243 333");
        request.setTimeZone("GMT");

        assertThrows(BadRequestException.class, () -> underTest.validateSchedule(request),
                "Cron expression must consist of 6 fields (found 4 in \"2 22 22243 333\")");
    }

    @Test
    void testValidateScheduleWhenInvalidTimeZone() {
        TimeAlertRequest request = new TimeAlertRequest();
        request.setCron("1 0 1 1 1 1");
        request.setTimeZone("GMT-4343");

        assertThrows(BadRequestException.class, () -> underTest.validateSchedule(request), "Invalid ID for offset-based ZoneId: GMT-4343");
    }

    @Test
    void testLoadAlertCreateWhenHostGroupNotSupported() {
        Set<String> requestHostGroups = Set.of("compute", "compute1");
        Set<String> supportedHostGroups = Set.of("compute1");

        when(recommendationService.getAutoscaleRecommendations(anyString()))
                .thenReturn(new AutoscaleRecommendationV4Response(supportedHostGroups, supportedHostGroups));
        when(messagesService.getMessage(UNSUPPORTED_AUTOSCALING_HOSTGROUP,
                List.of(requestHostGroups, AlertType.LOAD, aCluster.getStackName(), supportedHostGroups)))
                .thenReturn("unsupported.hostgroup");

        assertThrows(BadRequestException.class, () -> underTest.validateSupportedHostGroup(aCluster, requestHostGroups, AlertType.LOAD),
                "unsupported.hostgroup");
    }

    @Test
    void testTimeAlertCreateWhenHostGroupNotSupported() {
        Set<String> requestHostGroups = Set.of("compute", "compute1");
        Set<String> supportedHostGroups = Set.of("compute1");

        when(recommendationService.getAutoscaleRecommendations(anyString()))
                .thenReturn(new AutoscaleRecommendationV4Response(supportedHostGroups, supportedHostGroups));
        when(messagesService.getMessage(UNSUPPORTED_AUTOSCALING_HOSTGROUP,
                List.of(requestHostGroups, AlertType.TIME, aCluster.getStackName(), supportedHostGroups)))
                .thenReturn("unsupported.hostgroup");

        assertThrows(BadRequestException.class, () -> underTest.validateSupportedHostGroup(aCluster, requestHostGroups, AlertType.TIME),
                "unsupported.hostgroup");
    }

    @Test
    void testTimeAlertCreateWhenStopStartEnabledInRequest() {
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

        assertThrows(BadRequestException.class, () -> underTest.validateScheduleWithStopStart(autoscaleClusterRequest),
                "Schedule-Based Autoscaling does not support the stop / start scaling mechanism.");
    }

    @Test
    void testTimeAlertCreateWhenStopStartEnabledInClusterEntity() {
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
        autoscaleClusterRequest.setUseStopStartMechanism(Boolean.TRUE);

        when(messagesService.getMessage(VALIDATION_TIME_STOP_START_UNSUPPORTED))
                .thenReturn("Schedule-Based Autoscaling does not support the stop / start scaling mechanism.");

        assertThrows(BadRequestException.class, () -> underTest.validateScheduleWithStopStart(autoscaleClusterRequest),
                "Schedule-Based Autoscaling does not support the stop / start scaling mechanism.");
    }

    @Test
    void testEnableStopStartWithTimeAlertPreCreated() {
        AutoscaleClusterState autoscaleState = new AutoscaleClusterState();
        autoscaleState.setUseStopStartMechanism(Boolean.TRUE);
        TimeAlert timeAlert = new TimeAlert();
        timeAlert.setTimeZone("Asia/Calcutta");
        timeAlert.setCluster(aCluster);
        timeAlert.setCron("1 0 1 1 1 1");
        aCluster.setTimeAlerts(Set.of(timeAlert));

        when(messagesService.getMessage(VALIDATION_TIME_STOP_START_UNSUPPORTED))
                .thenReturn("Schedule-Based Autoscaling does not support the stop / start scaling mechanism.");

        assertThrows(BadRequestException.class, () -> underTest.validateScheduleWithStopStart(aCluster, autoscaleState),
                "Schedule-Based Autoscaling does not support the stop / start scaling mechanism.");
    }

    @Test
    void testEnableStopStartInRequestWithTimeAlertPreCreated() {
        DistroXAutoscaleClusterRequest autoscaleRequest = new DistroXAutoscaleClusterRequest();
        TimeAlertRequest timeAlertRequest = new TimeAlertRequest();
        timeAlertRequest.setTimeZone("Asia/Calcutta");
        timeAlertRequest.setCron("1 0 0 1 1 1");
        timeAlertRequest.setAlertName("timeAlert");
        autoscaleRequest.setTimeAlertRequests(List.of(timeAlertRequest));

        autoscaleRequest.setEnableAutoscaling(Boolean.TRUE);
        autoscaleRequest.setUseStopStartMechanism(Boolean.TRUE);

        TimeAlert timeAlert = new TimeAlert();
        timeAlert.setTimeZone("Asia/Calcutta");
        timeAlert.setCluster(aCluster);
        timeAlert.setCron("1 0 1 1 1 1");
        aCluster.setTimeAlerts(Set.of(timeAlert));

        when(messagesService.getMessage(VALIDATION_TIME_STOP_START_UNSUPPORTED))
                .thenReturn("Schedule-Based Autoscaling does not support the stop / start scaling mechanism.");

        assertThrows(BadRequestException.class, () -> underTest.validateScheduleWithStopStart(autoscaleRequest),
                "Schedule-Based Autoscaling does not support the stop / start scaling mechanism.");
    }

    @Test
    void testCreateTimeAlertSuccessWithStopStartDisabled() {
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

        assertDoesNotThrow(() -> underTest.validateScheduleWithStopStart(autoscaleClusterRequest));
    }

    @Test
    void testEnableStopStartSuccessWithNoTimeAlertPreCreated() {
        AutoscaleClusterState autoscaleState = new AutoscaleClusterState();
        aCluster.setTimeAlerts(Collections.emptySet());
        autoscaleState.setUseStopStartMechanism(Boolean.TRUE);

        assertDoesNotThrow(() -> underTest.validateScheduleWithStopStart(aCluster, autoscaleState));
    }

    @Test
    void testCreateTimeAlertSuccessIfStopStartDisabledInRequestButEnabledInClusterEntity() {
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

        assertDoesNotThrow(() -> underTest.validateScheduleWithStopStart(autoscaleClusterRequest));
    }

    @Test
    void testCreateLoadAlertSuccessWithStopStartIfTimeAlertPreCreatedButEmptyInRequest() {
        DistroXAutoscaleClusterRequest asClusterRequest = new DistroXAutoscaleClusterRequest();
        asClusterRequest.setEnableAutoscaling(Boolean.TRUE);
        ScalingPolicyRequest sp1 = new ScalingPolicyRequest();
        sp1.setHostGroup("compute");
        LoadAlertRequest loadAlertRequest = new LoadAlertRequest();
        loadAlertRequest.setScalingPolicy(sp1);
        LoadAlertConfigurationRequest loadAlertConfig = new LoadAlertConfigurationRequest();
        loadAlertConfig.setMinResourceValue(1);
        loadAlertConfig.setMaxResourceValue(100);
        loadAlertConfig.setCoolDownMinutes(5);
        loadAlertRequest.setLoadAlertConfiguration(loadAlertConfig);
        asClusterRequest.setLoadAlertRequests(List.of(loadAlertRequest));

        asClusterRequest.setTimeAlertRequests(emptyList());

        TimeAlert timeAlert = new TimeAlert();
        ScalingPolicy scalingPolicy = new ScalingPolicy();
        scalingPolicy.setAdjustmentType(AdjustmentType.EXACT);
        scalingPolicy.setHostGroup("compute");
        scalingPolicy.setAlert(timeAlert);
        scalingPolicy.setScalingAdjustment(4);
        timeAlert.setScalingPolicy(scalingPolicy);
        timeAlert.setTimeZone("Asia/Calcutta");
        timeAlert.setCron("1 0 1 1 1 1");
        aCluster.setTimeAlerts(Set.of(timeAlert));

        assertDoesNotThrow(() -> underTest.validateScheduleWithStopStart(asClusterRequest));
    }

    @Test
    void testLoadAlertCreateWhenHostGroupSupported() {
        Set<String> requestHostGroups = Set.of("compute", "compute1");
        Set<String> supportedHostGroups = Set.of("compute", "compute1", "compute3");

        when(recommendationService.getAutoscaleRecommendations(anyString()))
                .thenReturn(new AutoscaleRecommendationV4Response(supportedHostGroups, supportedHostGroups));

        underTest.validateSupportedHostGroup(aCluster, requestHostGroups, AlertType.LOAD);
    }

    @Test
    void testTimeAlertCreateWhenHostGroupSupported() {
        Set<String> requestHostGroups = Set.of("compute", "compute1");
        Set<String> supportedHostGroups = Set.of("compute", "compute1");

        when(recommendationService.getAutoscaleRecommendations(anyString()))
                .thenReturn(new AutoscaleRecommendationV4Response(supportedHostGroups, supportedHostGroups));

        underTest.validateSupportedHostGroup(aCluster, requestHostGroups, AlertType.TIME);
    }

    @Test
    void testValidateDistroXAutoscaleClusterRequestWhenValidLoadAlertRequest() {
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
    void testValidateDistroXAutoscaleClusterRequestWhenHostGroupNotSupported() {
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

        assertThrows(BadRequestException.class, () -> underTest.validateDistroXAutoscaleClusterRequest(aCluster, request), "unsupported.hostgroup");
    }

    @Test
    void testValidateDistroXAutoscaleClusterRequestWhenClusterSizeExceeded() {
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

        when(limitsConfigurationService.getMaxNodeCountLimit(anyString())).thenReturn(400);
        when(messagesService.getMessage(AUTOSCALING_CLUSTER_LIMIT_EXCEEDED, List.of(400)))
                .thenReturn("cluster limit exceeded");

        assertThrows(BadRequestException.class, () -> underTest.validateDistroXAutoscaleClusterRequest(aCluster, request), "cluster limit exceeded");
    }

    @Test
    void testValidateDistroXAutoscaleClusterRequestWhenValidTimeAlertRequest() {
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
    void testValidateDistroXAutoscaleClusterRequestWhenInvalidTimeZone() {
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

        assertThrows(BadRequestException.class, () -> underTest.validateDistroXAutoscaleClusterRequest(aCluster, request),
                "Invalid ID for offset-based ZoneId: GMT-4343");
    }

    @Test
    void testValidateDistroXAutoscaleClusterRequestWhenInvalidHostGroup() {
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

        assertThrows(BadRequestException.class, () -> underTest.validateDistroXAutoscaleClusterRequest(aCluster, request), "unsupported.hostgroup");
    }

    private Cluster getACluster() {
        Cluster cluster = new Cluster();
        cluster.setStackCrn(TEST_STACK_CRN);
        cluster.setStackName("teststack");
        cluster.setCloudPlatform("AWS");
        cluster.setTunnel(Tunnel.CLUSTER_PROXY);
        cluster.setAutoscalingEnabled(true);
        return cluster;
    }
}

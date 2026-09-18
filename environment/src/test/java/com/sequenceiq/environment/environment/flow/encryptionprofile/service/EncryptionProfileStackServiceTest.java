package com.sequenceiq.environment.environment.flow.encryptionprofile.service;

import static com.sequenceiq.environment.environment.flow.encryptionprofile.service.EncryptionProfileOperation.DISABLE;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.service.EncryptionProfileOperation.ENABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.environment.environment.flow.DatalakeMultipleFlowsResultEvaluator;
import com.sequenceiq.environment.environment.flow.MultipleFlowsResultEvaluator;
import com.sequenceiq.environment.environment.flow.encryptionprofile.validator.EncryptionProfileClusterResolver;
import com.sequenceiq.environment.environment.poller.DatahubPollerProvider;
import com.sequenceiq.environment.environment.poller.SdxPollerProvider;
import com.sequenceiq.environment.environment.service.datahub.DatahubService;
import com.sequenceiq.environment.environment.service.sdx.SdxService;
import com.sequenceiq.environment.environment.service.stack.StackService;
import com.sequenceiq.environment.exception.SdxOperationFailedException;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
class EncryptionProfileStackServiceTest {

    private static final Long ENV_ID = 1L;

    private static final String DATALAKE_CRN_1 = "crn:cdp:datalake:us-west-1:1234:datalake:dl1";

    private static final String DATALAKE_CRN_2 = "crn:cdp:datalake:us-west-1:1234:datalake:dl2";

    private static final String DATAHUB_CRN = "crn:cdp:datahub:us-west-1:1234:cluster:dh1";

    private static final FlowIdentifier DATALAKE_FLOW_1 = new FlowIdentifier(FlowType.FLOW, "dl-flow-1");

    private static final FlowIdentifier DATALAKE_FLOW_2 = new FlowIdentifier(FlowType.FLOW, "dl-flow-2");

    private static final FlowIdentifier DATAHUB_FLOW = new FlowIdentifier(FlowType.FLOW, "dh-flow");

    @Mock
    private StackService stackService;

    @Mock
    private SdxService sdxService;

    @Mock
    private DatahubService datahubService;

    @Mock
    private EncryptionProfileClusterResolver encryptionProfileClusterResolver;

    @Mock
    private SdxPollerProvider sdxPollerProvider;

    @Mock
    private DatalakeMultipleFlowsResultEvaluator datalakeMultipleFlowsResultEvaluator;

    @Mock
    private DatahubPollerProvider datahubPollerProvider;

    @Mock
    private MultipleFlowsResultEvaluator datahubMultipleFlowsResultEvaluator;

    @InjectMocks
    private EncryptionProfileStackService underTest;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "maxTime", 1);
        ReflectionTestUtils.setField(underTest, "sleepTime", 1);
    }

    @Test
    void testApplyOnDatalakesWithDisableTriggersDisableOnEveryDatalake() {
        StackViewV4Response datalake1 = stack("dl1", DATALAKE_CRN_1);
        StackViewV4Response datalake2 = stack("dl2", DATALAKE_CRN_2);
        when(sdxService.disableEncryptionProfile(DATALAKE_CRN_1)).thenReturn(DATALAKE_FLOW_1);
        when(sdxService.disableEncryptionProfile(DATALAKE_CRN_2)).thenReturn(DATALAKE_FLOW_2);
        when(sdxPollerProvider.flowListPoller(ENV_ID, List.of(DATALAKE_FLOW_1))).thenReturn(AttemptResults::justFinish);
        when(sdxPollerProvider.flowListPoller(ENV_ID, List.of(DATALAKE_FLOW_2))).thenReturn(AttemptResults::justFinish);
        when(datalakeMultipleFlowsResultEvaluator.anyFailed(any())).thenReturn(false);

        List<String> failures = underTest.applyOnDatalakes(ENV_ID, targets(List.of(datalake1, datalake2), List.of()), DISABLE);

        assertThat(failures).isEmpty();
        verify(sdxService).disableEncryptionProfile(DATALAKE_CRN_1);
        verify(sdxService).disableEncryptionProfile(DATALAKE_CRN_2);
        verify(sdxService, never()).enableEncryptionProfile(any(), any());
    }

    @Test
    void testApplyOnDatalakesWithDisableReportsTriggerFailureAndContinues() {
        StackViewV4Response datalake1 = stack("dl1", DATALAKE_CRN_1);
        StackViewV4Response datalake2 = stack("dl2", DATALAKE_CRN_2);
        when(sdxService.disableEncryptionProfile(DATALAKE_CRN_1)).thenThrow(new SdxOperationFailedException("dl is busy"));
        when(sdxService.disableEncryptionProfile(DATALAKE_CRN_2)).thenReturn(DATALAKE_FLOW_2);
        when(sdxPollerProvider.flowListPoller(ENV_ID, List.of(DATALAKE_FLOW_2))).thenReturn(AttemptResults::justFinish);
        when(datalakeMultipleFlowsResultEvaluator.anyFailed(List.of(DATALAKE_FLOW_2))).thenReturn(false);

        List<String> failures = underTest.applyOnDatalakes(ENV_ID, targets(List.of(datalake1, datalake2), List.of()), DISABLE);

        assertThat(failures).containsExactly("Data Lake 'dl1' (dl is busy)");
        verify(sdxService).disableEncryptionProfile(DATALAKE_CRN_2);
    }

    @Test
    void testApplyOnDatahubsWithDisableTriggersDisable() {
        StackViewV4Response datahub = stack("dh1", DATAHUB_CRN);
        when(datahubService.disableEncryptionProfile(DATAHUB_CRN)).thenReturn(DATAHUB_FLOW);
        when(datahubPollerProvider.multipleFlowsPoller(ENV_ID, List.of(DATAHUB_FLOW))).thenReturn(AttemptResults::justFinish);
        when(datahubMultipleFlowsResultEvaluator.collectFailed(List.of(DATAHUB_FLOW))).thenReturn(List.of());

        List<String> failures = underTest.applyOnDatahubs(ENV_ID, targets(List.of(), List.of(datahub)), DISABLE);

        assertThat(failures).isEmpty();
        verify(datahubService).disableEncryptionProfile(DATAHUB_CRN);
        verify(datahubService, never()).enableEncryptionProfile(any(), any());
    }

    @Test
    void testApplyOnDatahubsWithEnableTriggersEnableWithoutClusterLevelCrn() {
        StackViewV4Response datahub = stack("dh1", DATAHUB_CRN);
        when(datahubService.enableEncryptionProfile(DATAHUB_CRN, null)).thenReturn(DATAHUB_FLOW);
        when(datahubPollerProvider.multipleFlowsPoller(ENV_ID, List.of(DATAHUB_FLOW))).thenReturn(AttemptResults::justFinish);
        when(datahubMultipleFlowsResultEvaluator.collectFailed(List.of(DATAHUB_FLOW))).thenReturn(List.of());

        List<String> failures = underTest.applyOnDatahubs(ENV_ID, targets(List.of(), List.of(datahub)), ENABLE);

        assertThat(failures).isEmpty();
        verify(datahubService, never()).disableEncryptionProfile(any());
    }

    @Test
    void testApplyKeepsDiscoveryFailures() {
        TargetClusters targetClusters = new TargetClusters(List.of(), List.of(), List.of("Data Lake 'dl1' (not found)"),
                List.of("Data Hub 'dh1' (not found)"));

        assertThat(underTest.applyOnDatalakes(ENV_ID, targetClusters, DISABLE)).containsExactly("Data Lake 'dl1' (not found)");
        assertThat(underTest.applyOnDatahubs(ENV_ID, targetClusters, DISABLE)).containsExactly("Data Hub 'dh1' (not found)");
        verify(datahubPollerProvider, never()).multipleFlowsPoller(any(), any());
    }

    private TargetClusters targets(List<StackViewV4Response> datalakes, List<StackViewV4Response> datahubs) {
        return new TargetClusters(datalakes, datahubs, List.of(), List.of());
    }

    private StackViewV4Response stack(String name, String crn) {
        StackViewV4Response stack = new StackViewV4Response();
        stack.setName(name);
        stack.setCrn(crn);
        return stack;
    }
}

package com.sequenceiq.environment.environment.flow.encryptionprofile.handler;

import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileStateSelectors.DISABLE_ENCRYPTION_PROFILE_ON_STACKS_HANDLER_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileStateSelectors.FAILED_DISABLE_ENCRYPTION_PROFILE_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileStateSelectors.FINISH_DISABLE_ENCRYPTION_PROFILE_EVENT;
import static com.sequenceiq.environment.environment.flow.encryptionprofile.service.EncryptionProfileOperation.DISABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.views.ClusterViewV4Response;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileEvent;
import com.sequenceiq.environment.environment.flow.encryptionprofile.event.DisableEncryptionProfileFailedEvent;
import com.sequenceiq.environment.environment.flow.encryptionprofile.service.EncryptionProfileStackService;
import com.sequenceiq.environment.environment.flow.encryptionprofile.service.TargetClusters;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class DisableEncryptionProfileOnStacksHandlerTest {

    private static final Long ENV_ID = 1L;

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:1234:environment:e1";

    private static final String ENV_NAME = "envName";

    @Mock
    private EncryptionProfileStackService encryptionProfileStackService;

    @InjectMocks
    private DisableEncryptionProfileOnStacksHandler underTest;

    private DisableEncryptionProfileEvent event;

    @BeforeEach
    void setUp() {
        event = new DisableEncryptionProfileEvent(DISABLE_ENCRYPTION_PROFILE_ON_STACKS_HANDLER_EVENT.name(), ENV_ID, ENV_NAME, ENV_CRN);
    }

    @Test
    void testSelector() {
        assertThat(underTest.selector()).isEqualTo(DISABLE_ENCRYPTION_PROFILE_ON_STACKS_HANDLER_EVENT.name());
    }

    @Test
    void testDefaultFailureEvent() {
        Selectable response = underTest.defaultFailureEvent(ENV_ID, new Exception("failed"), new Event<>(event));
        assertThat(response.getSelector()).isEqualTo(FAILED_DISABLE_ENCRYPTION_PROFILE_EVENT.selector());
        assertThat(response.getResourceId()).isEqualTo(ENV_ID);
        assertThat(response).isInstanceOf(DisableEncryptionProfileFailedEvent.class);
    }

    @Test
    void testDoAcceptDisablesOnAllEligibleClusters() {
        StackViewV4Response datalake1 = stack("dl1", Status.AVAILABLE);
        StackViewV4Response datalake2 = stack("dl2", Status.UPDATE_FAILED, Status.UPDATE_FAILED);
        StackViewV4Response datahub = stack("dh1", Status.AVAILABLE);
        TargetClusters targetClusters = new TargetClusters(List.of(datalake1, datalake2), List.of(datahub), List.of(), List.of());
        when(encryptionProfileStackService.getClusters(ENV_CRN)).thenReturn(List.of(datalake1, datalake2, datahub));
        when(encryptionProfileStackService.collectClustersInheritingEnvironmentProfile(List.of(datalake1, datalake2, datahub)))
                .thenReturn(targetClusters);
        when(encryptionProfileStackService.applyOnDatalakes(ENV_ID, targetClusters, DISABLE)).thenReturn(List.of());
        when(encryptionProfileStackService.applyOnDatahubs(ENV_ID, targetClusters, DISABLE)).thenReturn(List.of());

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        assertThat(response.getSelector()).isEqualTo(FINISH_DISABLE_ENCRYPTION_PROFILE_EVENT.selector());
        assertThat(((DisableEncryptionProfileEvent) response).getSkippedClusters()).isEmpty();
    }

    @Test
    void testDoAcceptReportsNotEligibleClustersAsSkippedWithoutInspectingThem() {
        StackViewV4Response stoppedDatalake = stack("dl", Status.STOPPED);
        StackViewV4Response availableDatahub = stack("dh1", Status.AVAILABLE);
        StackViewV4Response stoppedDatahub = stack("dh2", Status.STOPPED);
        StackViewV4Response datahubWithoutCluster = new StackViewV4Response();
        datahubWithoutCluster.setName("dh3");
        datahubWithoutCluster.setStatus(Status.AVAILABLE);
        StackViewV4Response stoppedStackWithAvailableCluster = stack("dh4", Status.STOPPED, Status.AVAILABLE);
        TargetClusters targetClusters = new TargetClusters(List.of(), List.of(availableDatahub), List.of(), List.of());
        when(encryptionProfileStackService.getClusters(ENV_CRN))
                .thenReturn(List.of(stoppedDatalake, availableDatahub, stoppedDatahub, datahubWithoutCluster, stoppedStackWithAvailableCluster));
        when(encryptionProfileStackService.collectClustersInheritingEnvironmentProfile(List.of(availableDatahub))).thenReturn(targetClusters);
        when(encryptionProfileStackService.applyOnDatalakes(ENV_ID, targetClusters, DISABLE)).thenReturn(List.of());
        when(encryptionProfileStackService.applyOnDatahubs(ENV_ID, targetClusters, DISABLE)).thenReturn(List.of());

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        assertThat(response.getSelector()).isEqualTo(FINISH_DISABLE_ENCRYPTION_PROFILE_EVENT.selector());
        assertThat(((DisableEncryptionProfileEvent) response).getSkippedClusters()).containsExactly("dl", "dh2", "dh3", "dh4");
    }

    @Test
    void testDoAcceptProcessesDatahubsEvenIfDatalakeFails() {
        StackViewV4Response datalake = stack("dl", Status.AVAILABLE);
        StackViewV4Response datahub = stack("dh1", Status.AVAILABLE);
        stubTargets(new TargetClusters(List.of(datalake), List.of(datahub), List.of(), List.of()));
        when(encryptionProfileStackService.applyOnDatalakes(eq(ENV_ID), any(), eq(DISABLE))).thenReturn(List.of("Data Lake 'dl' (CM restart failed)"));
        when(encryptionProfileStackService.applyOnDatahubs(eq(ENV_ID), any(), eq(DISABLE))).thenReturn(List.of("Data Hub 'dh1' (salt failed)"));

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        assertFailedWith(response, "Data Lake 'dl' (CM restart failed), Data Hub 'dh1' (salt failed)");
        verify(encryptionProfileStackService, never()).skipDatahubs(any(), any());
    }

    @Test
    void testDoAcceptReportsSkippedClustersWhenAnEligibleClusterFails() {
        StackViewV4Response stoppedDatahub = stack("dh-stopped", Status.STOPPED);
        StackViewV4Response runningDatahub = stack("dh-running", Status.AVAILABLE);
        TargetClusters targetClusters = new TargetClusters(List.of(), List.of(runningDatahub), List.of(), List.of());
        when(encryptionProfileStackService.getClusters(ENV_CRN)).thenReturn(List.of(stoppedDatahub, runningDatahub));
        when(encryptionProfileStackService.collectClustersInheritingEnvironmentProfile(List.of(runningDatahub))).thenReturn(targetClusters);
        when(encryptionProfileStackService.applyOnDatalakes(ENV_ID, targetClusters, DISABLE)).thenReturn(List.of());
        when(encryptionProfileStackService.applyOnDatahubs(ENV_ID, targetClusters, DISABLE)).thenReturn(List.of("Data Hub 'dh-running' (salt failed)"));

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        assertThat(response).isInstanceOf(DisableEncryptionProfileFailedEvent.class);
        assertThat(response.getException().getMessage()).isEqualTo("Failed to disable encryption profile on the following cluster(s): "
                + "Data Hub 'dh-running' (salt failed). The following cluster(s) were skipped because they were not in AVAILABLE or UPDATE_FAILED "
                + "state: dh-stopped. The encryption profile was removed from the environment; retry the operation.");
    }

    @Test
    void testDoAcceptWithoutClustersFinishes() {
        stubTargets(new TargetClusters(List.of(), List.of(), List.of(), List.of()));
        when(encryptionProfileStackService.applyOnDatalakes(eq(ENV_ID), any(), eq(DISABLE))).thenReturn(List.of());
        when(encryptionProfileStackService.applyOnDatahubs(eq(ENV_ID), any(), eq(DISABLE))).thenReturn(List.of());

        Selectable response = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        assertThat(response.getSelector()).isEqualTo(FINISH_DISABLE_ENCRYPTION_PROFILE_EVENT.selector());
        assertThat(response.getResourceId()).isEqualTo(ENV_ID);
    }

    private void stubTargets(TargetClusters targetClusters) {
        when(encryptionProfileStackService.getClusters(ENV_CRN)).thenReturn(List.of());
        when(encryptionProfileStackService.collectClustersInheritingEnvironmentProfile(List.of())).thenReturn(targetClusters);
    }

    private void assertFailedWith(Selectable response, String failedClusters) {
        assertThat(response).isInstanceOf(DisableEncryptionProfileFailedEvent.class);
        DisableEncryptionProfileFailedEvent failedEvent = (DisableEncryptionProfileFailedEvent) response;
        assertThat(failedEvent.getSelector()).isEqualTo(FAILED_DISABLE_ENCRYPTION_PROFILE_EVENT.selector());
        assertThat(failedEvent.getResourceId()).isEqualTo(ENV_ID);
        assertThat(failedEvent.getResourceCrn()).isEqualTo(ENV_CRN);
        assertThat(failedEvent.getException()).isInstanceOf(CloudbreakServiceException.class);
        assertThat(failedEvent.getException().getMessage()).isEqualTo("Failed to disable encryption profile on the following cluster(s): "
                + failedClusters + ". The encryption profile was removed from the environment; retry the operation.");
    }

    private StackViewV4Response stack(String name, Status clusterStatus) {
        return stack(name, Status.AVAILABLE, clusterStatus);
    }

    private StackViewV4Response stack(String name, Status stackStatus, Status clusterStatus) {
        StackViewV4Response stack = new StackViewV4Response();
        stack.setName(name);
        stack.setStatus(stackStatus);
        ClusterViewV4Response cluster = new ClusterViewV4Response();
        cluster.setStatus(clusterStatus);
        stack.setCluster(cluster);
        return stack;
    }
}

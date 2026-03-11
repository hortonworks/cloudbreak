package com.sequenceiq.freeipa.service.rotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.quartz.model.JobResource;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.service.SecretTypeListService;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rotate.FreeIpaSecretRotationRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.rotate.FreeipaSecretTypeResponse;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.entity.projection.FreeIpaListView;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class FreeIpaPeriodicSecretRotationServiceTest {

    private static final String RESOURCE_CRN = "crn:cdp:freeipa:us-west-1:acc-12345:freeipa:myfreeipa";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:acc-12345:environment:env";

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private StackService stackService;

    @Mock
    private FlowLogService flowLogService;

    @Mock
    private SecretTypeListService secretTypeListService;

    @Mock
    private List<SecretType> enabledSecretTypes;

    @Mock
    private FreeIpaSecretRotationService freeIpaSecretRotationService;

    @InjectMocks
    private FreeIpaPeriodicSecretRotationService underTest;

    private FreeIpaListView view;

    @BeforeEach
    void init() {
        StackStatus stackStatus = new StackStatus();
        stackStatus.setId(42L);
        stackStatus.setStatus(Status.AVAILABLE);
        view = new FreeIpaListView("domain", "name", RESOURCE_CRN, ENV_CRN, stackStatus);
    }

    @Test
    void listJobResourcesDelegatesToService() {
        JobResource jobResource = mock(JobResource.class);
        when(jobResource.getRemoteResourceId()).thenReturn(RESOURCE_CRN);
        when(stackService.findAllForAutoSync()).thenReturn(List.of(jobResource));
        List<JobResource> resources = underTest.listJobResources();
        assertThat(resources).hasSize(1);
        assertThat(resources.get(0).getRemoteResourceId()).isEqualTo(RESOURCE_CRN);
    }

    @Test
    void getByCrnDelegatesToService() {
        when(freeIpaService.getViewByResourceCrn(RESOURCE_CRN)).thenReturn(Optional.of(view));
        assertThat(underTest.getResourceByCrn(RESOURCE_CRN)).contains(view);
    }

    @Test
    void isSchedulableTrueWhenAvailableAndNoFlowRunning() {
        when(freeIpaService.getViewByResourceCrn(RESOURCE_CRN)).thenReturn(Optional.of(view));
        when(flowLogService.isOtherFlowRunning(42L)).thenReturn(false);
        assertThat(underTest.isSchedulable(RESOURCE_CRN)).isTrue();
    }

    @Test
    void isSchedulableFalseWhenFlowRunning() {
        when(freeIpaService.getViewByResourceCrn(RESOURCE_CRN)).thenReturn(Optional.of(view));
        when(flowLogService.isOtherFlowRunning(42L)).thenReturn(true);
        assertThat(underTest.isSchedulable(RESOURCE_CRN)).isFalse();
    }

    @Test
    void isSchedulableFalseWhenResourceNotFound() {
        when(freeIpaService.getViewByResourceCrn(RESOURCE_CRN)).thenReturn(Optional.empty());
        assertThat(underTest.isSchedulable(RESOURCE_CRN)).isFalse();
    }

    @Test
    void isSchedulableFalseWhenNotAvailable() {
        StackStatus s = new StackStatus();
        s.setId(1L);
        s.setStatus(Status.STOPPED);
        FreeIpaListView notAvailable = new FreeIpaListView("d", "n", RESOURCE_CRN, ENV_CRN, s);
        when(freeIpaService.getViewByResourceCrn(RESOURCE_CRN)).thenReturn(Optional.of(notAvailable));
        when(flowLogService.isOtherFlowRunning(1L)).thenReturn(false);
        assertThat(underTest.isSchedulable(RESOURCE_CRN)).isFalse();
    }

    @Test
    void listRotatableSecretNamesMapsSecretTypeStrings() {
        when(freeIpaService.getViewByResourceCrn(RESOURCE_CRN)).thenReturn(Optional.of(view));
        FreeipaSecretTypeResponse r1 = new FreeipaSecretTypeResponse("FREEIPA_ADMIN_PASSWORD", "n", "d", 0L);
        FreeipaSecretTypeResponse r2 = new FreeipaSecretTypeResponse("SOME_OTHER", "n2", "d2", 0L);
        when(secretTypeListService.listRotatableSecretType(eq(ENV_CRN), any())).thenReturn(List.of(r1, r2));
        assertThat(underTest.listRotatableSecretNames(RESOURCE_CRN)).containsExactlyInAnyOrder("FREEIPA_ADMIN_PASSWORD", "SOME_OTHER");
    }

    @Test
    void listRotatableSecretNamesEmptyWhenResourceNotFound() {
        when(freeIpaService.getViewByResourceCrn(RESOURCE_CRN)).thenReturn(Optional.empty());
        assertThat(underTest.listRotatableSecretNames(RESOURCE_CRN)).isEmpty();
    }

    @Test
    void enabledSecretTypesReturnsInjectedList() {
        assertThat(underTest.enabledSecretTypes()).isSameAs(enabledSecretTypes);
    }

    @Test
    void triggerRotationBuildsRequestAndDelegates() {
        when(freeIpaService.getViewByResourceCrn(RESOURCE_CRN)).thenReturn(Optional.of(view));
        List<String> due = List.of("FREEIPA_ADMIN_PASSWORD", "SALT_BOOT_SECRETS");

        underTest.triggerRotation(RESOURCE_CRN, due);

        verify(freeIpaSecretRotationService).rotateSecretsByCrn(eq("acc-12345"), eq(ENV_CRN), any(FreeIpaSecretRotationRequest.class));
    }

    @Test
    void triggerRotationDoesNothingWhenResourceNotFound() {
        when(freeIpaService.getViewByResourceCrn(RESOURCE_CRN)).thenReturn(Optional.empty());

        underTest.triggerRotation(RESOURCE_CRN, List.of("FREEIPA_ADMIN_PASSWORD"));

        verify(freeIpaSecretRotationService, never()).rotateSecretsByCrn(any(), any(), any());
    }

    @Test
    void getResourceCreationDateReturnsInstantFromStackService() {
        Instant creationDate = Instant.now().minus(Duration.ofDays(30));
        when(stackService.getCreatedByResourceCrn(eq(RESOURCE_CRN))).thenReturn(creationDate);

        Instant result = underTest.getResourceCreationDate(RESOURCE_CRN);

        assertThat(result).isEqualTo(creationDate);
        verify(stackService).getCreatedByResourceCrn(eq(RESOURCE_CRN));
    }

    @Test
    void getResourceCreationDateThrowsWhenResourceNotFound() {
        when(stackService.getCreatedByResourceCrn(eq(RESOURCE_CRN)))
                .thenThrow(new NotFoundException("FreeIPA stack not found or terminated"));

        assertThatThrownBy(() -> underTest.getResourceCreationDate(RESOURCE_CRN))
                .isInstanceOf(NotFoundException.class);
    }
}


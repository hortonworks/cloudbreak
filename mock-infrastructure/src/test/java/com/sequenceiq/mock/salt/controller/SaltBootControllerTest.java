package com.sequenceiq.mock.salt.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponse;
import com.sequenceiq.cloudbreak.orchestrator.model.GenericResponses;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltActionType;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.Minion;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltAction;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.SaltMaster;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;
import com.sequenceiq.mock.salt.SaltStoreService;

@ExtendWith(MockitoExtension.class)
class SaltBootControllerTest {

    private static final String MOCK_UUID = "uuid";

    @Mock
    private SaltStoreService saltStoreService;

    @InjectMocks
    private SaltBootController underTest;

    @Test
    void masterOnlyStopAcknowledgesEveryRequestedMaster() {
        SaltAction action = new SaltAction(SaltActionType.STOP);
        action.setMasters(List.of(master("10.0.0.1"), master("10.0.0.2:9443")));

        GenericResponses responses = underTest.saltActionDistribute(MOCK_UUID, action);

        assertThat(responses.getResponses()).extracting(GenericResponse::getAddress).containsExactlyInAnyOrder("10.0.0.1", "10.0.0.2:9443");
        assertThat(responses.getResponses()).allSatisfy(response -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value()));
        verify(saltStoreService).setSaltAction(MOCK_UUID, action);
    }

    @Test
    void masterStopResponseSatisfiesTheProductionAcknowledgementCheck() throws CloudbreakOrchestratorFailedException {
        SaltConnector connector = mock(SaltConnector.class);
        when(connector.action(any())).thenAnswer(invocation -> underTest.saltActionDistribute(MOCK_UUID, invocation.getArgument(0)));
        SaltStateService saltStateService = new SaltStateService();

        saltStateService.stopMastersToRestart(connector, Set.of("10.0.0.1", "10.0.0.2")).failIfAnyMasterWasNotStopped();
    }

    @Test
    void minionOnlyStopAcknowledgesEveryRequestedMinion() {
        SaltAction action = new SaltAction(SaltActionType.STOP);
        action.setMinions(List.of(minion("10.0.0.1"), minion("10.0.0.2")));

        GenericResponses responses = underTest.saltActionDistribute(MOCK_UUID, action);

        assertThat(responses.getResponses()).extracting(GenericResponse::getAddress).containsExactlyInAnyOrder("10.0.0.1", "10.0.0.2");
        assertThat(responses.getResponses()).allSatisfy(response -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value()));
    }

    @Test
    void runAcknowledgesEachAddressOnceWhenMasterIsAlsoAMinion() {
        SaltAction action = new SaltAction(SaltActionType.RUN);
        action.setMasters(List.of(master("10.0.0.1")));
        action.setMinions(List.of(minion("10.0.0.1"), minion("10.0.0.2")));

        GenericResponses responses = underTest.saltActionDistribute(MOCK_UUID, action);

        assertThat(responses.getResponses()).extracting(GenericResponse::getAddress).containsExactlyInAnyOrder("10.0.0.1", "10.0.0.2");
        assertThat(responses.getResponses()).allSatisfy(response -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value()));
    }

    @Test
    void actionWithoutTargetsHasNoAcknowledgements() {
        GenericResponses responses = underTest.saltActionDistribute(MOCK_UUID, new SaltAction(SaltActionType.STOP));

        assertThat(responses.getResponses()).isEmpty();
    }

    private SaltMaster master(String address) {
        SaltMaster master = new SaltMaster();
        master.setAddress(address);
        return master;
    }

    private Minion minion(String address) {
        Minion minion = new Minion();
        minion.setAddress(address);
        return minion;
    }
}

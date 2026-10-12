package com.sequenceiq.cloudbreak.core.flow2.cluster.provision;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.BOOTSTRAP_FREEIPA_ENDPOINT_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.CONFIGURE_KEYTABS_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.WAIT_FOR_EXTERNAL_DATABASE_AVAILABLE_FAILED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent.WAIT_FOR_EXTERNAL_DATABASE_AVAILABLE_FINISHED_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.BOOTSTRAPPING_FREEIPA_ENDPOINT_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.CLUSTER_CREATION_FAILED_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.CONFIGURE_KEYTABS_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.STARTING_CLUSTER_MANAGER_SERVICES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.UPLOAD_RECIPES_STATE;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationState.WAIT_FOR_EXTERNAL_DATABASE_AVAILABLE_STATE;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.flow.core.config.AbstractFlowConfiguration.Transition;

class ClusterCreationFlowConfigTest {

    private ClusterCreationFlowConfig underTest;

    private List<Transition<ClusterCreationState, ClusterCreationEvent>> transitions;

    @BeforeEach
    void setUp() {
        underTest = new ClusterCreationFlowConfig();
        transitions = underTest.getTransitions().stream().toList();
    }

    @Test
    void freeIpaEndpointBootstrapLeadsToExternalDatabaseBarrier() {
        Transition<ClusterCreationState, ClusterCreationEvent> transition = transitionFrom(BOOTSTRAPPING_FREEIPA_ENDPOINT_STATE);

        assertThat(transition.getTarget()).isEqualTo(WAIT_FOR_EXTERNAL_DATABASE_AVAILABLE_STATE);
        assertThat(transition.getEvent()).isEqualTo(BOOTSTRAP_FREEIPA_ENDPOINT_FINISHED_EVENT);
    }

    @Test
    void externalDatabaseBarrierLeadsToRecipeUpload() {
        Transition<ClusterCreationState, ClusterCreationEvent> transition = transitionFrom(WAIT_FOR_EXTERNAL_DATABASE_AVAILABLE_STATE);

        assertThat(transition.getTarget()).isEqualTo(UPLOAD_RECIPES_STATE);
        assertThat(transition.getEvent()).isEqualTo(WAIT_FOR_EXTERNAL_DATABASE_AVAILABLE_FINISHED_EVENT);
        assertThat(transition.getFailureEvent()).isEqualTo(WAIT_FOR_EXTERNAL_DATABASE_AVAILABLE_FAILED_EVENT);
    }

    @Test
    void externalDatabaseBarrierFailureRoutesToDefaultFailureState() {
        assertThat(transitionFrom(WAIT_FOR_EXTERNAL_DATABASE_AVAILABLE_STATE).getFailureState()).isNull();
        assertThat(underTest.getEdgeConfig().getDefaultFailureState()).isEqualTo(CLUSTER_CREATION_FAILED_STATE);
    }

    @Test
    void externalDatabaseIsAvailableBeforeRecipesAreUploadedBecauseTemplatedRecipesReadRedbeams() {
        assertThat(sourcesOf(UPLOAD_RECIPES_STATE)).containsExactly(WAIT_FOR_EXTERNAL_DATABASE_AVAILABLE_STATE);
    }

    @Test
    void recipeUploadAndKeytabsRunAfterTheBarrierAndStartServicesFollowsKeytabs() {
        assertThat(transitionFrom(UPLOAD_RECIPES_STATE).getTarget()).isEqualTo(CONFIGURE_KEYTABS_STATE);
        Transition<ClusterCreationState, ClusterCreationEvent> keytabs = transitionFrom(CONFIGURE_KEYTABS_STATE);
        assertThat(keytabs.getTarget()).isEqualTo(STARTING_CLUSTER_MANAGER_SERVICES_STATE);
        assertThat(keytabs.getEvent()).isEqualTo(CONFIGURE_KEYTABS_FINISHED_EVENT);
        assertThat(sourcesOf(STARTING_CLUSTER_MANAGER_SERVICES_STATE)).containsExactly(CONFIGURE_KEYTABS_STATE);
    }

    @Test
    void getEventsMatchesEnum() {
        assertThat(underTest.getEvents()).hasSize(ClusterCreationEvent.values().length);
    }

    private List<ClusterCreationState> sourcesOf(ClusterCreationState target) {
        return transitions.stream()
                .filter(t -> t.getTarget() == target)
                .map(Transition::getSource)
                .toList();
    }

    private Transition<ClusterCreationState, ClusterCreationEvent> transitionFrom(ClusterCreationState source) {
        List<Transition<ClusterCreationState, ClusterCreationEvent>> matching = transitions.stream()
                .filter(t -> t.getSource() == source)
                .toList();
        assertThat(matching).hasSize(1);
        return matching.getFirst();
    }
}

package com.sequenceiq.cloudbreak.orchestrator.rotation;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;

public class SaltStateApplyRotationContext extends RotationContext {

    private final GatewayConfig gatewayConfig;

    private final Set<String> targets;

    private final List<String> states;

    // if there is a different state for rollback, we should use it, otherwise the original state should handle the rollback
    private final Optional<List<String>> rollBackStates;

    private final Optional<List<String>> cleanupStates;

    private final ExitCriteriaModel exitCriteriaModel;

    public SaltStateApplyRotationContext(String resourceCrn, GatewayConfig gatewayConfig, Set<String> targets, List<String> states,
            Optional<List<String>> rollBackStates, Optional<List<String>> cleanupStates, ExitCriteriaModel exitCriteriaModel) {
        super(resourceCrn);
        this.gatewayConfig = gatewayConfig;
        this.targets = targets;
        this.states = states;
        this.rollBackStates = rollBackStates;
        this.cleanupStates = cleanupStates;
        this.exitCriteriaModel = exitCriteriaModel;
    }

    public GatewayConfig getGatewayConfig() {
        return gatewayConfig;
    }

    public Set<String> getTargets() {
        return targets;
    }

    public List<String> getStates() {
        return states;
    }

    public ExitCriteriaModel getExitCriteriaModel() {
        return exitCriteriaModel;
    }

    public Optional<List<String>> getRollBackStates() {
        return rollBackStates;
    }

    public Optional<List<String>> getCleanupStates() {
        return cleanupStates;
    }

    public static SaltStateApplyRotationContextBuilder builder() {
        return new SaltStateApplyRotationContextBuilder();
    }

    public static class SaltStateApplyRotationContextBuilder {

        private String resourceCrn;

        private GatewayConfig gatewayConfig;

        private Set<String> targets;

        private List<String> states;

        private Optional<List<String>> rollBackStates;

        private Optional<List<String>> cleanupStates;

        private ExitCriteriaModel exitCriteriaModel;

        public SaltStateApplyRotationContextBuilder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public SaltStateApplyRotationContextBuilder withGatewayConfig(GatewayConfig gatewayConfig) {
            this.gatewayConfig = gatewayConfig;
            return this;
        }

        public SaltStateApplyRotationContextBuilder withTargets(Set<String> targets) {
            this.targets = targets;
            return this;
        }

        public SaltStateApplyRotationContextBuilder withStates(List<String> states) {
            this.states = states;
            return this;
        }

        public SaltStateApplyRotationContextBuilder withRollbackStates(Optional<List<String>> rollBackStates) {
            this.rollBackStates = rollBackStates;
            return this;
        }

        public SaltStateApplyRotationContextBuilder withCleanupStates(Optional<List<String>> cleanupStates) {
            this.cleanupStates = cleanupStates;
            return this;
        }

        public SaltStateApplyRotationContextBuilder withExitCriteriaModel(ExitCriteriaModel exitCriteriaModel) {
            this.exitCriteriaModel = exitCriteriaModel;
            return this;
        }

        public SaltStateApplyRotationContext build() {
            return new SaltStateApplyRotationContext(resourceCrn, gatewayConfig, targets, states, rollBackStates, cleanupStates, exitCriteriaModel);
        }

    }
}

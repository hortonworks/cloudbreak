package com.sequenceiq.cloudbreak.rotation.context;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateRetryParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

public class SaltRunOrchestratorStateRotationContext extends SaltStateApplyRotationContext {

    private static final int DEFAULT_RETRY = 1;

    private final Map<String, Object> rotateParams;

    private final Optional<Map<String, Object>> rollbackParams;

    private final Optional<Map<String, Object>> cleanupParams;

    private final Boolean stateRunNeeded;

    SaltRunOrchestratorStateRotationContext(String resourceCrn, GatewayConfig gatewayConfig, Set<String> targets,
            List<String> states, Map<String, Object> rotateParams, Optional<List<String>> rollBackStates, Optional<Map<String, Object>> rollbackParams,
            Optional<List<String>> cleanupStates, Optional<Map<String, Object>> cleanupParams,
            ExitCriteriaModel exitCriteriaModel, Optional<Integer> maxRetry, Optional<Integer> maxRetryOnError,
            Boolean stateRunNeeded) {
        super(resourceCrn, gatewayConfig, targets, states,
                rollBackStates, cleanupStates, exitCriteriaModel,
                maxRetry, maxRetryOnError);
        this.rotateParams = rotateParams;
        this.rollbackParams = rollbackParams;
        this.cleanupParams = cleanupParams;
        this.stateRunNeeded = stateRunNeeded;
    }

    public OrchestratorStateParams getRotateOrchestratorStateParams() {
        OrchestratorStateParams stateParams = getBaseParams();
        stateParams.setState(getStates().get(0));
        stateParams.setStateParams(rotateParams);
        return stateParams;
    }

    public OrchestratorStateParams getRollbackOrchestratorStateParams() {
        OrchestratorStateParams stateParams = getBaseParams();
        stateParams.setState(getStateFromOptionalStates(getRollBackStates()));
        stateParams.setStateParams(rollbackParams.orElse(null));
        return stateParams;
    }

    public OrchestratorStateParams getCleanupOrchestratorStateParams() {
        OrchestratorStateParams stateParams = getBaseParams();
        stateParams.setState(getStateFromOptionalStates(getCleanupStates()));
        stateParams.setStateParams(cleanupParams.orElse(null));
        return stateParams;
    }

    private String getStateFromOptionalStates(Optional<List<String>> states) {
        return states.map(l -> l.get(0)).orElse(null);
    }

    private OrchestratorStateParams getBaseParams() {
        OrchestratorStateParams stateParams = new OrchestratorStateParams();
        stateParams.setPrimaryGatewayConfig(getGatewayConfig());
        stateParams.setTargetHostNames(getTargets());
        stateParams.setExitCriteriaModel(getExitCriteriaModel());
        OrchestratorStateRetryParams retryParams = new OrchestratorStateRetryParams();
        retryParams.setMaxRetry(getMaxRetry().orElse(DEFAULT_RETRY));
        retryParams.setMaxRetryOnError(getMaxRetryOnError().orElse(DEFAULT_RETRY));
        stateParams.setStateRetryParams(retryParams);
        return stateParams;
    }

    public Map<String, Object> getRotateParams() {
        return rotateParams;
    }

    public Optional<Map<String, Object>> getRollbackParams() {
        return rollbackParams;
    }

    public Boolean stateRunNeeded() {
        return stateRunNeeded;
    }

    public boolean rollbackStateExists() {
        return getStateFromOptionalStates(getRollBackStates()) != null && rollbackParams.isPresent();
    }

    public boolean cleanupStateExists() {
        return getStateFromOptionalStates(getCleanupStates()) != null && cleanupParams.isPresent();
    }

    public static class SaltRunOrchestratorStateRotationContextBuilder {

        private Map<String, Object> rotateParams;

        private Optional<Map<String, Object>> rollbackParams = Optional.empty();

        private Optional<Map<String, Object>> cleanupParams = Optional.empty();

        private String resourceCrn;

        private GatewayConfig gatewayConfig;

        private Set<String> targets;

        private List<String> states;

        private Optional<List<String>> rollBackStates = Optional.empty();

        private Optional<List<String>> cleanupStates = Optional.empty();

        private ExitCriteriaModel exitCriteriaModel;

        private Optional<Integer> maxRetry = Optional.empty();

        private Optional<Integer> maxRetryOnError = Optional.empty();

        private Boolean stateRunNeeded = Boolean.TRUE;

        public SaltRunOrchestratorStateRotationContextBuilder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public SaltRunOrchestratorStateRotationContextBuilder withGatewayConfig(GatewayConfig gatewayConfig) {
            this.gatewayConfig = gatewayConfig;
            return this;
        }

        public SaltRunOrchestratorStateRotationContextBuilder withTargets(Set<String> targets) {
            this.targets = targets;
            return this;
        }

        public SaltRunOrchestratorStateRotationContextBuilder withStates(List<String> states) {
            this.states = states;
            return this;
        }

        public SaltRunOrchestratorStateRotationContextBuilder withRollbackStates(Optional<List<String>> rollBackStates) {
            this.rollBackStates = rollBackStates;
            return this;
        }

        public SaltRunOrchestratorStateRotationContextBuilder withCleanupStates(Optional<List<String>> cleanupStates) {
            this.cleanupStates = cleanupStates;
            return this;
        }

        public SaltRunOrchestratorStateRotationContextBuilder withExitCriteriaModel(ExitCriteriaModel exitCriteriaModel) {
            this.exitCriteriaModel = exitCriteriaModel;
            return this;
        }

        public SaltRunOrchestratorStateRotationContextBuilder withRotateParams(Map<String, Object> rotateParams) {
            this.rotateParams = rotateParams;
            return this;
        }

        public SaltRunOrchestratorStateRotationContextBuilder withRollbackParams(Optional<Map<String, Object>> rollBackParams) {
            this.rollbackParams = rollBackParams;
            return this;
        }

        public SaltRunOrchestratorStateRotationContextBuilder withCleanupParams(Optional<Map<String, Object>> cleanupParams) {
            this.cleanupParams = cleanupParams;
            return this;
        }

        public SaltRunOrchestratorStateRotationContextBuilder withMaxRetry(Integer maxRetry) {
            this.maxRetry = Optional.of(maxRetry);
            return this;
        }

        public SaltRunOrchestratorStateRotationContextBuilder withMaxRetryOnError(Integer maxRetryOnError) {
            this.maxRetryOnError = Optional.of(maxRetryOnError);
            return this;
        }

        public SaltRunOrchestratorStateRotationContextBuilder noStateRunNeeded() {
            stateRunNeeded = Boolean.FALSE;
            return this;
        }

        public SaltRunOrchestratorStateRotationContext build() {
            return new SaltRunOrchestratorStateRotationContext(resourceCrn, gatewayConfig, targets,
                    states, rotateParams, rollBackStates, rollbackParams, cleanupStates, cleanupParams,
                    exitCriteriaModel, maxRetry, maxRetryOnError, stateRunNeeded);
        }
    }
}

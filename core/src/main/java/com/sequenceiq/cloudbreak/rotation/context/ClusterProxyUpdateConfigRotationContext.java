package com.sequenceiq.cloudbreak.rotation.context;

import java.util.function.Supplier;

import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.view.GatewayView;

public class ClusterProxyUpdateConfigRotationContext extends RotationContext {

    private final Supplier<String> knoxSecretPath;

    private final GatewayView newGatewaySecrets;

    private final Long currentGatewayId;

    protected ClusterProxyUpdateConfigRotationContext(String resourceCrn, Long currentGatewayId,
        Supplier<String> knoxSecretPath, GatewayView newGatewaySecrets) {
        super(resourceCrn);
        this.knoxSecretPath = knoxSecretPath;
        this.newGatewaySecrets = newGatewaySecrets;
        this.currentGatewayId = currentGatewayId;
    }

    public Supplier<String> getKnoxSecretPath() {
        return knoxSecretPath;
    }

    public GatewayView getNewGatewaySecrets() {
        return newGatewaySecrets;
    }

    public Long getCurrentGatewayId() {
        return currentGatewayId;
    }

    public static ClusterProxyUpdateConfigRotationContextBuilder builder() {
        return new ClusterProxyUpdateConfigRotationContextBuilder();
    }

    public static class ClusterProxyUpdateConfigRotationContextBuilder {

        private String resourceCrn;

        private Supplier<String> knoxSecretPath;

        private GatewayView newGatewaySecrets;

        private Long currentGatewayId;

        public ClusterProxyUpdateConfigRotationContextBuilder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public ClusterProxyUpdateConfigRotationContextBuilder withKnoxSecretPath(Supplier<String> knoxSecretPath) {
            this.knoxSecretPath = knoxSecretPath;
            return this;
        }

        public ClusterProxyUpdateConfigRotationContextBuilder withNewGatewaySecrets(GatewayView newGatewaySecrets) {
            this.newGatewaySecrets = newGatewaySecrets;
            return this;
        }

        public ClusterProxyUpdateConfigRotationContextBuilder withCurrentGatewayId(Long currentGatewayId) {
            this.currentGatewayId = currentGatewayId;
            return this;
        }

        public ClusterProxyUpdateConfigRotationContext build() {
            return new ClusterProxyUpdateConfigRotationContext(resourceCrn, currentGatewayId, knoxSecretPath, newGatewaySecrets);
        }
    }
}

package com.sequenceiq.cloudbreak.rotation.context;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.sequenceiq.cloudbreak.rotation.common.RotationContext;

public class CMUserRotationContext extends RotationContext {

    private final String clientUserSecret;

    private final String clientPasswordSecret;

    private final Set<Pair<String, String>> rotatableSecrets;

    public CMUserRotationContext(String resourceCrn, String clientUserSecret, String clientPasswordSecret, Set<Pair<String, String>> rotatableSecrets) {
        super(resourceCrn);
        this.clientUserSecret = clientUserSecret;
        this.clientPasswordSecret = clientPasswordSecret;
        this.rotatableSecrets = rotatableSecrets;
    }

    public String getClientUserSecret() {
        return clientUserSecret;
    }

    public String getClientPasswordSecret() {
        return clientPasswordSecret;
    }

    public Set<Pair<String, String>> getRotatableSecrets() {
        return rotatableSecrets;
    }

    public static CMUserRotationContextBuilder builder() {
        return new CMUserRotationContextBuilder();
    }

    public static class CMUserRotationContextBuilder {

        private String clientUserSecret;

        private String clientPasswordSecret;

        private Set<Pair<String, String>> rotatableSecrets = new HashSet<>();

        private String resourceCrn;

        public CMUserRotationContextBuilder withClientUserSecret(String clientUserSecret) {
            this.clientUserSecret = clientUserSecret;
            return this;
        }

        public CMUserRotationContextBuilder withClientPasswordSecret(String clientPasswordSecret) {
            this.clientPasswordSecret = clientPasswordSecret;
            return this;
        }

        public CMUserRotationContextBuilder withRotatableSecrets(Set<Pair<String, String>> rotatableSecrets) {
            this.rotatableSecrets = rotatableSecrets;
            return this;
        }

        public CMUserRotationContextBuilder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public CMUserRotationContext build() {
            return new CMUserRotationContext(resourceCrn, clientUserSecret, clientPasswordSecret, rotatableSecrets);
        }

    }
}

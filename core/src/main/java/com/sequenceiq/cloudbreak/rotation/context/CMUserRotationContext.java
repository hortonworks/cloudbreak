package com.sequenceiq.cloudbreak.rotation.context;

import com.sequenceiq.cloudbreak.rotation.common.RotationContext;

public class CMUserRotationContext extends RotationContext {

    private final String clientUserSecret;

    private final String clientPasswordSecret;

    private final String userSecret;

    private final String passwordSecret;

    public CMUserRotationContext(String resourceCrn, String clientUserSecret, String clientPasswordSecret, String userSecret, String passwordSecret) {
        super(resourceCrn);
        this.clientUserSecret = clientUserSecret;
        this.clientPasswordSecret = clientPasswordSecret;
        this.userSecret = userSecret;
        this.passwordSecret = passwordSecret;
    }

    public String getClientUserSecret() {
        return clientUserSecret;
    }

    public String getClientPasswordSecret() {
        return clientPasswordSecret;
    }

    public String getUserSecret() {
        return userSecret;
    }

    public String getPasswordSecret() {
        return passwordSecret;
    }

    public static CMUserRotationContextBuilder builder() {
        return new CMUserRotationContextBuilder();
    }

    public static class CMUserRotationContextBuilder {

        private String clientUserSecret;

        private String clientPasswordSecret;

        private String userSecret;

        private String passwordSecret;

        private String resourceCrn;

        public CMUserRotationContextBuilder withClientUserSecret(String clientUserSecret) {
            this.clientUserSecret = clientUserSecret;
            return this;
        }

        public CMUserRotationContextBuilder withClientPasswordSecret(String clientPasswordSecret) {
            this.clientPasswordSecret = clientPasswordSecret;
            return this;
        }

        public CMUserRotationContextBuilder withUserSecret(String userSecret) {
            this.userSecret = userSecret;
            return this;
        }

        public CMUserRotationContextBuilder withPasswordSecret(String passwordSecret) {
            this.passwordSecret = passwordSecret;
            return this;
        }

        public CMUserRotationContextBuilder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public CMUserRotationContext build() {
            return new CMUserRotationContext(resourceCrn, clientUserSecret, clientPasswordSecret, userSecret, passwordSecret);
        }

    }
}

package com.sequenceiq.freeipa.service.rotation.context;

import com.sequenceiq.cloudbreak.rotation.common.RotationContext;

public class FreeIpaUserPasswordRotationContext extends RotationContext {

    private final String username;

    private final String passwordSecret;

    private FreeIpaUserPasswordRotationContext(String resourceCrn, String username, String passwordSecret) {
        super(resourceCrn);
        this.username = username;
        this.passwordSecret = passwordSecret;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordSecret() {
        return passwordSecret;
    }

    public static FreeipaAdminPasswordRotationContextBuilder builder() {
        return new FreeipaAdminPasswordRotationContextBuilder();
    }

    @Override
    public String toString() {
        return "FreeIpaAdminPasswordRotationContext{" +
                "username='" + username + '\'' +
                "passwordSecret='" + passwordSecret + '\'' +
                "} " + super.toString();
    }

    public static class FreeipaAdminPasswordRotationContextBuilder {

        private String resourceCrn;

        private String username;

        private String passwordSecret;

        public FreeipaAdminPasswordRotationContextBuilder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public FreeipaAdminPasswordRotationContextBuilder withUserName(String username) {
            this.username = username;
            return this;
        }

        public FreeipaAdminPasswordRotationContextBuilder withPasswordSecret(String passwordSecret) {
            this.passwordSecret = passwordSecret;
            return this;
        }

        public FreeIpaUserPasswordRotationContext build() {
            return new FreeIpaUserPasswordRotationContext(resourceCrn, username, passwordSecret);
        }
    }
}

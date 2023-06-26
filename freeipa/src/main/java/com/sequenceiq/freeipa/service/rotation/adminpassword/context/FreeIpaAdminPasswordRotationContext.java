package com.sequenceiq.freeipa.service.rotation.adminpassword.context;

import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;

public class FreeIpaAdminPasswordRotationContext extends RotationContext {

    private final String adminPasswordSecret;

    private FreeIpaAdminPasswordRotationContext(String resourceCrn, String adminPasswordSecret) {
        super(resourceCrn);
        this.adminPasswordSecret = adminPasswordSecret;
    }

    public String getAdminPasswordSecret() {
        return adminPasswordSecret;
    }

    public static FreeipaAdminPasswordRotationContextBuilder builder() {
        return new FreeipaAdminPasswordRotationContextBuilder();
    }

    public static class FreeipaAdminPasswordRotationContextBuilder {

        private String resourceCrn;

        private String adminPasswordSecret;

        public FreeipaAdminPasswordRotationContextBuilder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public FreeipaAdminPasswordRotationContextBuilder withAdminPasswordSecret(String adminPasswordSecret) {
            this.adminPasswordSecret = adminPasswordSecret;
            return this;
        }

        public FreeIpaAdminPasswordRotationContext build() {
            return new FreeIpaAdminPasswordRotationContext(resourceCrn, adminPasswordSecret);
        }
    }
}

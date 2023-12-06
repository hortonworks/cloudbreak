package com.sequenceiq.freeipa.service.rotation.ldapbindpassword.context;

import com.sequenceiq.cloudbreak.rotation.common.RotationContext;

public class FreeIpaLdapBindPasswordRotationContext extends RotationContext {

    private final String bindPasswordSecret;

    private final String clusterName;

    private final boolean rotateUserSyncUser;

    private FreeIpaLdapBindPasswordRotationContext(String resourceCrn, String bindPasswordSecret, String clusterName, boolean rotateUserSyncUser) {
        super(resourceCrn);
        this.bindPasswordSecret = bindPasswordSecret;
        this.clusterName = clusterName;
        this.rotateUserSyncUser = rotateUserSyncUser;
    }

    public String getBindPasswordSecret() {
        return bindPasswordSecret;
    }

    public String getClusterName() {
        return clusterName;
    }

    public boolean rotateUserSyncUser() {
        return rotateUserSyncUser;
    }

    public static FreeipaLdapBindPasswordRotationContextBuilder builder() {
        return new FreeipaLdapBindPasswordRotationContextBuilder();
    }

    @Override
    public String toString() {
        return "FreeIpaLdapBindPasswordRotationContext{" +
                "bindPasswordSecret='" + bindPasswordSecret + '\'' +
                "clusterName='" + clusterName + '\'' +
                "rotateUserSyncUser'" + rotateUserSyncUser + '\'' +
                "} " + super.toString();
    }

    public static class FreeipaLdapBindPasswordRotationContextBuilder {

        private String resourceCrn;

        private String bindPasswordSecret;

        private String clusterName;

        private boolean rotateUserSyncUser;

        public FreeipaLdapBindPasswordRotationContextBuilder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public FreeipaLdapBindPasswordRotationContextBuilder withBindPasswordSecret(String bindPasswordSecret) {
            this.bindPasswordSecret = bindPasswordSecret;
            return this;
        }

        public FreeipaLdapBindPasswordRotationContextBuilder withClusterName(String clusterName) {
            this.clusterName = clusterName;
            return this;
        }

        public FreeipaLdapBindPasswordRotationContextBuilder withRotateUserSyncUser(boolean rotateUserSyncUser) {
            this.rotateUserSyncUser = rotateUserSyncUser;
            return this;
        }

        public FreeIpaLdapBindPasswordRotationContext build() {
            return new FreeIpaLdapBindPasswordRotationContext(resourceCrn, bindPasswordSecret, clusterName, rotateUserSyncUser);
        }
    }
}

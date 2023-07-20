package com.sequenceiq.cloudbreak.rotation.context;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;

public class CMServiceRoleRestartRotationContext extends RotationContext {

    private final String serviceType;

    private final String roleType;

    CMServiceRoleRestartRotationContext(String resourceCrn, String serviceType, String roleType) {
        super(resourceCrn);
        this.serviceType = serviceType;
        this.roleType = roleType;
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getRoleType() {
        return roleType;
    }

    public static CMServiceRoleRestartRotationContextBuilder builder() {
        return new CMServiceRoleRestartRotationContextBuilder();
    }

    public static class CMServiceRoleRestartRotationContextBuilder {

        private String serviceType;

        private String roleType;

        private String resourceCrn;

        public CMServiceRoleRestartRotationContextBuilder withResourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public CMServiceRoleRestartRotationContextBuilder withServiceType(String serviceType) {
            this.serviceType = serviceType;
            return this;
        }

        public CMServiceRoleRestartRotationContextBuilder withRoleType(String roleType) {
            this.roleType = roleType;
            return this;
        }

        public CMServiceRoleRestartRotationContext build() {
            if (resourceCrn == null || serviceType == null || roleType == null) {
                throw new CloudbreakServiceException(String.format("Failed to build cm service role restart rotation context. " +
                        "[resourceCrn='%s', serviceType='%s', roleType='%s'", resourceCrn, serviceType, roleType));
            }
            return new CMServiceRoleRestartRotationContext(resourceCrn, serviceType, roleType);
        }
    }

}

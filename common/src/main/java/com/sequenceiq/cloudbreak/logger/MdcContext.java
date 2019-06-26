package com.sequenceiq.cloudbreak.logger;

public class MdcContext {

    private final String resourceCrn;

    private final String resourceName;

    private final String resourceType;

    private final String flowId;

    private final String requestId;

    private final String userCrn;

    private final String tenant;

    private final String environmentCrn;

    private MdcContext(Builder builder) {
        resourceCrn = builder.resourceCrn;
        resourceName = builder.resourceName;
        resourceType = builder.resourceType;
        flowId = builder.flowId;
        requestId = builder.requestId;
        userCrn = builder.userCrn;
        tenant = builder.tenant;
        environmentCrn = builder.environmentCrn;
    }

    public String getResourceCrn() {
        return resourceCrn;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getFlowId() {
        return flowId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getUserCrn() {
        return userCrn;
    }

    public String getTenant() {
        return tenant;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String resourceCrn;

        private String resourceName;

        private String resourceType;

        private String flowId;

        private String requestId;

        private String userCrn;

        private String tenant;

        private String environmentCrn;

        public Builder resourceCrn(String resourceCrn) {
            this.resourceCrn = resourceCrn;
            return this;
        }

        public Builder resourceName(String resourceName) {
            this.resourceName = resourceName;
            return this;
        }

        public Builder resourceType(String resourceType) {
            this.resourceType = resourceType;
            return this;
        }

        public Builder flowId(String flowId) {
            this.flowId = flowId;
            return this;
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder userCrn(String userCrn) {
            this.userCrn = userCrn;
            return this;
        }

        public Builder tenant(String tenant) {
            this.tenant = tenant;
            return this;
        }

        public Builder environmentCrn(String environmentCrn) {
            this.environmentCrn = environmentCrn;
            return this;
        }

        public MdcContext build() {
            return new MdcContext(this);
        }

        public MdcContext buildMdc() {
            MdcContext mdcContext = new MdcContext(this);
            MDCBuilder.buildMdc(mdcContext);
            return mdcContext;
        }
    }
}

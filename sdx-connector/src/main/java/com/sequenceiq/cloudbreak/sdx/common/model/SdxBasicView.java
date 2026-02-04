package com.sequenceiq.cloudbreak.sdx.common.model;

import com.sequenceiq.cloudbreak.sdx.TargetPlatform;

public record SdxBasicView(
        String name,
        String crn,
        String runtime,
        boolean razEnabled,
        Long created,
        String dbServerCrn,
        TargetPlatform platform) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String name;

        private String crn;

        private String runtime;

        private boolean razEnabled;

        private Long created;

        private String dbServerCrn;

        private TargetPlatform platform;

        private Builder() {
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withCrn(String crn) {
            this.crn = crn;
            return this;
        }

        public Builder withRuntime(String runtime) {
            this.runtime = runtime;
            return this;
        }

        public Builder withRazEnabled() {
            this.razEnabled = true;
            return this;
        }

        public Builder withRazEnabled(boolean razEnabled) {
            this.razEnabled = razEnabled;
            return this;
        }

        public Builder withCreated(Long created) {
            this.created = created;
            return this;
        }

        public Builder withDbServerCrn(String dbServerCrn) {
            this.dbServerCrn = dbServerCrn;
            return this;
        }

        public Builder withPlatform(TargetPlatform platform) {
            this.platform = platform;
            return this;
        }

        public SdxBasicView build() {
            return new SdxBasicView(this.name, this.crn, this.runtime, this.razEnabled, this.created,
                    this.dbServerCrn, this.platform);
        }
    }
}

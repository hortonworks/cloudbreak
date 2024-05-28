package com.sequenceiq.cloudbreak.cloud.model.secret.request;

import java.util.Collections;
import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

public record UpdateCloudSecretResourceAccessRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudResource cloudResource,
        List<String> principals, List<String> authorizedClients) {

    public UpdateCloudSecretResourceAccessRequest(Builder builder) {
        this(
                builder.cloudContext,
                builder.cloudCredential,
                builder.cloudResource,
                builder.principals,
                builder.authorizedClients
        );
    }

    public Builder toBuilder() {
        return builder()
                .withCloudContext(cloudContext)
                .withCloudCredential(cloudCredential)
                .withCloudResource(cloudResource)
                .withPrincipals(principals)
                .withAuthorizedClients(authorizedClients);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private CloudContext cloudContext;

        private CloudCredential cloudCredential;

        private CloudResource cloudResource;

        private List<String> principals = Collections.emptyList();

        private List<String> authorizedClients = Collections.emptyList();

        private Builder() {
        }

        public Builder withCloudContext(CloudContext cloudContext) {
            this.cloudContext = cloudContext;
            return this;
        }

        public Builder withCloudCredential(CloudCredential cloudCredential) {
            this.cloudCredential = cloudCredential;
            return this;
        }

        public Builder withCloudResource(CloudResource cloudResource) {
            this.cloudResource = cloudResource;
            return this;
        }

        public Builder withPrincipals(List<String> principals) {
            this.principals = principals;
            return this;
        }

        public Builder withAuthorizedClients(List<String> authorizedClients) {
            this.authorizedClients = authorizedClients;
            return this;
        }

        public UpdateCloudSecretResourceAccessRequest build() {
            return new UpdateCloudSecretResourceAccessRequest(this);
        }
    }
}

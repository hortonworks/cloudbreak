package com.sequenceiq.cloudbreak.cloud.model.secret.request;

import java.util.Collections;
import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

public record UpdateCloudSecretResourceAccessRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudResource cloudResource,
        List<String> cryptographicPrincipals, List<String> cryptographicAuthorizedClients) {

    public UpdateCloudSecretResourceAccessRequest(Builder builder) {
        this(
                builder.cloudContext,
                builder.cloudCredential,
                builder.cloudResource,
                builder.cryptographicPrincipals,
                builder.cryptographicAuthorizedClients
        );
    }

    public Builder toBuilder() {
        return builder()
                .withCloudContext(cloudContext)
                .withCloudCredential(cloudCredential)
                .withCloudResource(cloudResource)
                .withCryptographicPrincipals(cryptographicPrincipals)
                .withCryptographicAuthorizedClients(cryptographicAuthorizedClients);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private CloudContext cloudContext;

        private CloudCredential cloudCredential;

        private CloudResource cloudResource;

        private List<String> cryptographicPrincipals = Collections.emptyList();

        private List<String> cryptographicAuthorizedClients = Collections.emptyList();

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

        public Builder withCryptographicPrincipals(List<String> cryptographicPrincipals) {
            this.cryptographicPrincipals = cryptographicPrincipals;
            return this;
        }

        public Builder withCryptographicAuthorizedClients(List<String> cryptographicAuthorizedClients) {
            this.cryptographicAuthorizedClients = cryptographicAuthorizedClients;
            return this;
        }

        public UpdateCloudSecretResourceAccessRequest build() {
            return new UpdateCloudSecretResourceAccessRequest(this);
        }

    }

}

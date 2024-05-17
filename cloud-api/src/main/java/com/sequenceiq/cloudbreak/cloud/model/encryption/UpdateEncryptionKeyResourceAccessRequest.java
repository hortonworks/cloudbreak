package com.sequenceiq.cloudbreak.cloud.model.encryption;

import java.util.ArrayList;
import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

public record UpdateEncryptionKeyResourceAccessRequest(CloudContext cloudContext, CloudCredential cloudCredential, CloudResource cloudResource,
        List<String> administratorPrincipalsToAdd, List<String> administratorPrincipalsToRemove, List<String> cryptographicPrincipalsToAdd,
        List<String> cryptographicPrincipalsToRemove, List<String> cryptographicAuthorizedClientsToAdd, List<String> cryptographicAuthorizedClientsToRemove) {

    public UpdateEncryptionKeyResourceAccessRequest(Builder builder) {
        this(
                builder.cloudContext,
                builder.cloudCredential,
                builder.cloudResource,
                builder.administratorPrincipalsToAdd,
                builder.administratorPrincipalsToRemove,
                builder.cryptographicPrincipalsToAdd,
                builder.cryptographicPrincipalsToRemove,
                builder.cryptographicAuthorizedClientsToAdd,
                builder.cryptographicAuthorizedClientsToRemove
        );
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private CloudContext cloudContext;

        private CloudCredential cloudCredential;

        private CloudResource cloudResource;

        private List<String> administratorPrincipalsToAdd = new ArrayList<>();

        private List<String> administratorPrincipalsToRemove = new ArrayList<>();

        private List<String> cryptographicPrincipalsToAdd = new ArrayList<>();

        private List<String> cryptographicPrincipalsToRemove = new ArrayList<>();

        private List<String> cryptographicAuthorizedClientsToAdd = new ArrayList<>();

        private List<String> cryptographicAuthorizedClientsToRemove = new ArrayList<>();

        private Builder() {
            // Prohibit instantiation outside the enclosing class
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

        public Builder withAdministratorPrincipalsToAdd(List<String> administratorPrincipalsToAdd) {
            this.administratorPrincipalsToAdd = administratorPrincipalsToAdd;
            return this;
        }

        public Builder withAdministratorPrincipalsToRemove(List<String> administratorPrincipalsToRemove) {
            this.administratorPrincipalsToRemove = administratorPrincipalsToRemove;
            return this;
        }

        public Builder withCryptographicPrincipalsToAdd(List<String> cryptographicPrincipalsToAdd) {
            this.cryptographicPrincipalsToAdd = cryptographicPrincipalsToAdd;
            return this;
        }

        public Builder withCryptographicPrincipalsToRemove(List<String> cryptographicPrincipalsToRemove) {
            this.cryptographicPrincipalsToRemove = cryptographicPrincipalsToRemove;
            return this;
        }

        public Builder withCryptographicAuthorizedClientsToAdd(List<String> cryptographicAuthorizedClientsToAdd) {
            this.cryptographicAuthorizedClientsToAdd = cryptographicAuthorizedClientsToAdd;
            return this;
        }

        public Builder withCryptographicAuthorizedClientsToRemove(List<String> cryptographicAuthorizedClientsToRemove) {
            this.cryptographicAuthorizedClientsToRemove = cryptographicAuthorizedClientsToRemove;
            return this;
        }

        public UpdateEncryptionKeyResourceAccessRequest build() {
            return new UpdateEncryptionKeyResourceAccessRequest(this);
        }

    }

}

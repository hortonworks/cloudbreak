package com.sequenceiq.cloudbreak.dto.credential;

import java.io.Serializable;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.dto.credential.aws.AwsCredentialAttributes;
import com.sequenceiq.cloudbreak.dto.credential.azure.AzureCredentialAttributes;

public class Credential implements Serializable {

    private final String cloudPlatform;

    private final String crn;

    private final String creator;

    private final String account;

    private final String name;

    private final String description;

    private final AwsCredentialAttributes aws;

    private final AzureCredentialAttributes azure;

    private final Json attributes;

    private Credential(Builder builder) {
        crn = builder.crn;
        name = builder.name;
        description = builder.description;
        aws = builder.aws;
        azure = builder.azure;
        attributes = builder.attributes;
        cloudPlatform = builder.cloudPlatform;
        creator = builder.creator;
        account = builder.account;
    }

    public String getCrn() {
        return crn;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public AwsCredentialAttributes getAws() {
        return aws;
    }

    public AzureCredentialAttributes getAzure() {
        return azure;
    }

    public Json getAttributes() {
        return attributes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getCreator() {
        return creator;
    }

    public String getAccount() {
        return account;
    }

    public String cloudPlatform() {
        if (aws != null) {
            return "AWS";
        } else if (azure != null) {
            return "AZURE";
        }
        return cloudPlatform;
    }

    public static final class Builder {
        private String crn;

        private String name;

        private String description;

        private AwsCredentialAttributes aws;

        private AzureCredentialAttributes azure;

        private Json attributes;

        private String cloudPlatform;

        private String creator;

        private String account;

        public Builder crn(String crn) {
            this.crn = crn;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder creator(String creator) {
            this.creator = creator;
            return this;
        }

        public Builder account(String account) {
            this.account = account;
            return this;
        }

        public Builder aws(AwsCredentialAttributes aws) {
            this.aws = aws;
            return this;
        }

        public Builder azure(AzureCredentialAttributes azure) {
            this.azure = azure;
            return this;
        }

        public Builder attributes(Json attributes) {
            this.attributes = attributes;
            return this;
        }

        public Builder cloudPlatform(String cloudPlatform) {
            this.cloudPlatform = cloudPlatform;
            return this;
        }

        public Credential build() {
            return new Credential(this);
        }
    }
}

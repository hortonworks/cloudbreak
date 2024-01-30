package com.sequenceiq.environment.api.v1.credential.model.response;

import static com.sequenceiq.environment.api.doc.ModelDescriptions.CRN;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.common.model.CredentialType;
import com.sequenceiq.environment.api.doc.ModelDescriptions;
import com.sequenceiq.environment.api.doc.credential.CredentialDescriptor;
import com.sequenceiq.environment.api.doc.credential.CredentialModelDescription;
import com.sequenceiq.environment.api.v1.credential.model.CredentialBase;
import com.sequenceiq.environment.api.v1.credential.model.parameters.aws.AwsCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.azure.AzureCredentialResponseParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.gcp.GcpCredentialParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.mock.MockParameters;
import com.sequenceiq.environment.api.v1.credential.model.parameters.yarn.YarnParameters;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = CredentialDescriptor.CREDENTIAL, allOf = CredentialBase.class, name = "CredentialV1Response")
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CredentialResponse extends CredentialBase {

    @Schema(description = ModelDescriptions.NAME, required = true)
    private String name;

    @Schema(description = CredentialModelDescription.ATTRIBUTES)
    private SecretResponse attributes;

    @Schema(description = CredentialModelDescription.AZURE_PARAMETERS)
    private AzureCredentialResponseParameters azure;

    @Schema(description = CRN)
    private String crn;

    /**
     * @deprecated data owner of any user is UMS, creator should not be stored and used anywhere, since user of creator can leave the given company
     * and can become invalid, usage of it can be error prone
     */
    @Deprecated
    @Schema(description = ModelDescriptions.CREATOR)
    private String creator;

    @Schema(description = CredentialModelDescription.ACCOUNT_IDENTIFIER)
    private String accountId;

    @Schema(description = CredentialModelDescription.CREATED)
    private Long created;

    @Schema(description = CredentialModelDescription.CREDENTIAL_TYPE, required = true)
    private CredentialType type;

    private Boolean govCloud;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public AzureCredentialResponseParameters getAzure() {
        return azure;
    }

    public void setAzure(AzureCredentialResponseParameters azure) {
        this.azure = azure;
    }

    public SecretResponse getAttributes() {
        return attributes;
    }

    public void setAttributes(SecretResponse attributes) {
        this.attributes = attributes;
    }

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    /**
     * @deprecated data owner of any user is UMS, creator should not be stored and used anywhere, since user of creator can leave the given company
     * and can become invalid, usage of it can be error prone
     */
    @Deprecated
    public String getCreator() {
        return creator;
    }

    /**
     * @deprecated data owner of any user is UMS, creator should not be stored and used anywhere, since user of creator can leave the given company
     * and can become invalid, usage of it can be error prone
     */
    @Deprecated
    public void setCreator(String creator) {
        this.creator = creator;
    }

    public CredentialType getType() {
        return type;
    }

    public void setType(CredentialType type) {
        this.type = type;
    }

    public Boolean getGovCloud() {
        return govCloud;
    }

    public void setGovCloud(Boolean govCloud) {
        this.govCloud = govCloud;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return super.toString() + ", " + "CredentialResponse{" +
                "name='" + name + '\'' +
                ", azure=" + azure +
                ", crn='" + crn + '\'' +
                ", creator='" + creator + '\'' +
                ", accountId='" + accountId + '\'' +
                ", created=" + created +
                ", govCloud=" + govCloud +
                ", type=" + type +
                '}';
    }

    public static final class Builder {

        private String cloudPlatform;

        private AwsCredentialParameters aws;

        private GcpCredentialParameters gcp;

        private YarnParameters yarn;

        private MockParameters mock;

        private String description;

        private String verificationStatusText;

        private boolean verifyPermissions;

        private boolean skipOrgPolicyDecisions;

        private String name;

        private SecretResponse attributes;

        private AzureCredentialResponseParameters azure;

        private String crn;

        private String creator;

        private String accountId;

        private Long created;

        private CredentialType type;

        private Boolean govCloud;

        private Builder() {
        }

        public Builder withCloudPlatform(String cloudPlatform) {
            this.cloudPlatform = cloudPlatform;
            return this;
        }

        public Builder withAws(AwsCredentialParameters aws) {
            this.aws = aws;
            return this;
        }

        public Builder withGcp(GcpCredentialParameters gcp) {
            this.gcp = gcp;
            return this;
        }

        public Builder withYarn(YarnParameters yarn) {
            this.yarn = yarn;
            return this;
        }

        public Builder withMock(MockParameters mock) {
            this.mock = mock;
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withVerificationStatusText(String verificationStatusText) {
            this.verificationStatusText = verificationStatusText;
            return this;
        }

        public Builder withVerifyPermissions(boolean verifyPermissions) {
            this.verifyPermissions = verifyPermissions;
            return this;
        }

        public Builder withSkipOrgPolicyDecisions(boolean skipOrgPolicyDecisions) {
            this.skipOrgPolicyDecisions = skipOrgPolicyDecisions;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withAttributes(SecretResponse attributes) {
            this.attributes = attributes;
            return this;
        }

        public Builder withAzure(AzureCredentialResponseParameters azure) {
            this.azure = azure;
            return this;
        }

        public Builder withCrn(String crn) {
            this.crn = crn;
            return this;
        }

        public Builder withCreator(String creator) {
            this.creator = creator;
            return this;
        }

        public Builder withAccountId(String accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder withCreated(Long created) {
            this.created = created;
            return this;
        }

        public Builder withType(CredentialType type) {
            this.type = type;
            return this;
        }

        public Builder withGovCloud(Boolean govCloud) {
            this.govCloud = govCloud;
            return this;
        }

        public CredentialResponse build() {
            CredentialResponse credentialResponse = new CredentialResponse();
            credentialResponse.setCloudPlatform(cloudPlatform);
            credentialResponse.setAws(aws);
            credentialResponse.setGcp(gcp);
            credentialResponse.setYarn(yarn);
            credentialResponse.setMock(mock);
            credentialResponse.setDescription(description);
            credentialResponse.setVerificationStatusText(verificationStatusText);
            credentialResponse.setVerifyPermissions(verifyPermissions);
            credentialResponse.setSkipOrgPolicyDecisions(skipOrgPolicyDecisions);
            credentialResponse.setName(name);
            credentialResponse.setAttributes(attributes);
            credentialResponse.setAzure(azure);
            credentialResponse.setCrn(crn);
            credentialResponse.setCreator(creator);
            credentialResponse.setAccountId(accountId);
            credentialResponse.setCreated(created);
            credentialResponse.setType(type);
            credentialResponse.setGovCloud(govCloud);
            return credentialResponse;
        }
    }
}

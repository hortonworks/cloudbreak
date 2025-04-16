package com.sequenceiq.cloudbreak.cloud.response;

import static com.sequenceiq.cloudbreak.cloud.doc.CredentialPrerequisiteModelDescription.GCP_CREDENTIAL_PREREQUISITES_CREATION_COMMAND;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
public class GcpCredentialPrerequisites extends CredentialBasePrerequisites implements Serializable {

    @Schema(description = GCP_CREDENTIAL_PREREQUISITES_CREATION_COMMAND, requiredMode = Schema.RequiredMode.REQUIRED)
    private String creationCommand;

    public GcpCredentialPrerequisites() {
    }

    public GcpCredentialPrerequisites(String creationCommand) {
        this.creationCommand = creationCommand;
    }

    public GcpCredentialPrerequisites(String creationCommand, Map<String, String> policies, Set<GranularPolicyResponse> granularPolicies) {
        this.creationCommand = creationCommand;
        setGranularPolicies(granularPolicies);
        setPolicies(policies);
    }

    public String getCreationCommand() {
        return creationCommand;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GcpCredentialPrerequisites that = (GcpCredentialPrerequisites) o;
        return Objects.equals(creationCommand, that.creationCommand);
    }

    @Override
    public int hashCode() {
        return Objects.hash(creationCommand);
    }

    @Override
    public String toString() {
        return "GcpCredentialPrerequisites{" +
                "creationCommand='" + creationCommand + '\'' +
                '}';
    }

    public void setCreationCommand(String creationCommand) {
        this.creationCommand = creationCommand;
    }

}

package com.sequenceiq.cloudbreak.cloud.response;

import static com.sequenceiq.cloudbreak.cloud.doc.CredentialPrerequisiteModelDescription.GCP_CREDENTIAL_PREREQUISITES_CREATION_COMMAND;

import java.io.Serializable;
import java.util.Objects;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel
public class GcpCredentialPrerequisites implements Serializable {

    @ApiModelProperty(value = GCP_CREDENTIAL_PREREQUISITES_CREATION_COMMAND, required = true)
    private String creationCommand;

    public GcpCredentialPrerequisites(String creationCommand) {
        this.creationCommand = creationCommand;
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
}

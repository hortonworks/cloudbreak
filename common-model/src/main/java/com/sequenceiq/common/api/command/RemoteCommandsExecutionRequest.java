package com.sequenceiq.common.api.command;

import static com.sequenceiq.common.api.command.doc.RemoteCommandsExecutionDescription.COMMAND;
import static com.sequenceiq.common.api.command.doc.RemoteCommandsExecutionDescription.HOSTS;
import static com.sequenceiq.common.api.command.doc.RemoteCommandsExecutionDescription.HOST_GROUPS;

import java.io.Serializable;
import java.util.Set;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value = "RemoteCommandsExecutionRequest")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RemoteCommandsExecutionRequest implements Serializable {

    @ApiModelProperty(COMMAND)
    @JsonProperty("command")
    @NotBlank
    private String command;

    @ApiModelProperty(HOSTS)
    @JsonProperty("hosts")
    private Set<String> hosts;

    @ApiModelProperty(HOST_GROUPS)
    @JsonProperty("hostGroups")
    private Set<String> hostGroups;

    public void setCommand(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    public Set<String> getHosts() {
        return hosts;
    }

    public void setHosts(Set<String> hosts) {
        this.hosts = hosts;
    }

    public Set<String> getHostGroups() {
        return hostGroups;
    }

    public void setHostGroups(Set<String> hostGroups) {
        this.hostGroups = hostGroups;
    }
}

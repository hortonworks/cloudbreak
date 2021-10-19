package com.sequenceiq.cloudbreak.cm.exception;

import java.math.BigDecimal;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.cloudera.api.swagger.model.ApiCommand;
import com.cloudera.api.swagger.model.ApiHostRef;
import com.cloudera.api.swagger.model.ApiRoleRef;
import com.cloudera.api.swagger.model.ApiServiceRef;

public class CommandDetails {

    private final BigDecimal id;

    private final String name;

    private final CommandStatus commandStatus;

    private final Optional<String> reason;

    private final Optional<String> serviceName;

    private final Optional<String> roleName;

    private final Optional<String> hostName;

    public CommandDetails(
            BigDecimal id,
            String name,
            CommandStatus commandStatus, Optional<String> reason,
            Optional<String> serviceName,
            Optional<String> roleName,
            Optional<String> hostName) {
        this.id = id;
        this.name = name;
        this.commandStatus = commandStatus;
        this.reason = reason;
        this.serviceName = serviceName;
        this.roleName = roleName;
        this.hostName = hostName;
    }

    public static CommandDetails fromApiCommand(ApiCommand apiCommand) {
        CommandStatus commandStatus;
        if (Boolean.TRUE.equals(apiCommand.getActive())) {
            commandStatus = CommandStatus.ACTIVE;
        } else if (Boolean.TRUE.equals(apiCommand.getSuccess())) {
            commandStatus = CommandStatus.SUCCESS;
        } else {
            commandStatus = CommandStatus.FAILED;
        }
        return new CommandDetails(
                apiCommand.getId(),
                apiCommand.getName(),
                commandStatus,
                Optional.ofNullable(apiCommand.getResultMessage())
                        .flatMap(r -> StringUtils.isNotEmpty(r) ? Optional.of(r) : Optional.empty()),
                Optional.ofNullable(apiCommand.getServiceRef()).map(ApiServiceRef::getServiceName),
                Optional.ofNullable(apiCommand.getRoleRef()).map(ApiRoleRef::getRoleName),
                Optional.ofNullable(apiCommand.getHostRef()).map(ApiHostRef::getHostname)
        );
    }

    public BigDecimal getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public CommandStatus getCommandStatus() {
        return commandStatus;
    }

    public String getReason() {
        return reason.orElse("");
    }

    public Optional<String> getServiceName() {
        return serviceName;
    }

    public Optional<String> getRoleName() {
        return roleName;
    }

    public Optional<String> getHostName() {
        return hostName;
    }

    @Override
    public String toString() {
        return "CommandDetails{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", commandStatus=" + commandStatus +
                reason.map(r -> ", reason=" + r).orElse("") +
                serviceName.map(r -> ", serviceName=" + r).orElse("") +
                roleName.map(r -> ", roleName=" + r).orElse("") +
                hostName.map(r -> ", hostName=" + r).orElse("") +
                '}';
    }

    public enum CommandStatus {
        ACTIVE,
        SUCCESS,
        FAILED
    }
}

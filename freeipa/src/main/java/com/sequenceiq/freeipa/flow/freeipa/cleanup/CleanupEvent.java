package com.sequenceiq.freeipa.flow.freeipa.cleanup;

import java.util.Set;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class CleanupEvent extends StackEvent {

    private final Set<String> users;

    private final Set<String> hosts;

    private final Set<String> roles;

    private final Set<String> ips;

    private final String accountId;

    private final String operationId;

    private final String clusterName;

    private final String environmentCrn;

    @SuppressWarnings("ExecutableStatementCount")
    public CleanupEvent(Long stackId, Set<String> users, Set<String> hosts, Set<String> roles, Set<String> ips, String accountId, String operationId,
            String clusterName, String environmentCrn) {
        super(stackId);
        this.users = users;
        this.hosts = hosts;
        this.roles = roles;
        this.ips = ips;
        this.accountId = accountId;
        this.operationId = operationId;
        this.clusterName = clusterName;
        this.environmentCrn = environmentCrn;
    }

    @SuppressWarnings("ExecutableStatementCount")
    public CleanupEvent(String selector, Long stackId, Set<String> users, Set<String> hosts, Set<String> roles,
            Set<String> ips, String accountId, String operationId, String clusterName, String environmentCrn) {
        super(selector, stackId);
        this.users = users;
        this.hosts = hosts;
        this.roles = roles;
        this.ips = ips;
        this.accountId = accountId;
        this.operationId = operationId;
        this.clusterName = clusterName;
        this.environmentCrn = environmentCrn;
    }

    public Set<String> getUsers() {
        return users;
    }

    public Set<String> getHosts() {
        return hosts;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getOperationId() {
        return operationId;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public Set<String> getIps() {
        return ips;
    }

    @Override
    public String toString() {
        return "CleanupEvent{" +
                "users=" + users +
                ", hosts=" + hosts +
                ", roles=" + roles +
                ", ips=" + ips +
                ", accountId='" + accountId + '\'' +
                ", operationId='" + operationId + '\'' +
                ", clusterName='" + clusterName + '\'' +
                ", environmentCrn='" + environmentCrn + '\'' +
                '}';
    }
}

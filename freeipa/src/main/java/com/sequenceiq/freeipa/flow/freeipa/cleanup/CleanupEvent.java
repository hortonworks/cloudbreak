package com.sequenceiq.freeipa.flow.freeipa.cleanup;

import java.util.Set;

import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class CleanupEvent extends StackEvent {

    private final Set<String> users;

    private final Set<String> hosts;

    private final Set<String> roles;

    private final String accountId;

    private final String operationId;

    private final String clusterName;

    @SuppressWarnings("ExecutableStatementCount")
    public CleanupEvent(Long stackId, Set<String> users, Set<String> hosts, Set<String> roles, String accountId, String operationId, String clusterName) {
        super(stackId);
        this.users = users;
        this.hosts = hosts;
        this.roles = roles;
        this.accountId = accountId;
        this.operationId = operationId;
        this.clusterName = clusterName;
    }

    @SuppressWarnings("ExecutableStatementCount")
    public CleanupEvent(String selector, Long stackId, Set<String> users, Set<String> hosts, Set<String> roles,
            String accountId, String operationId, String clusterName) {
        super(selector, stackId);
        this.users = users;
        this.hosts = hosts;
        this.roles = roles;
        this.accountId = accountId;
        this.operationId = operationId;
        this.clusterName = clusterName;
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

    @Override
    public String toString() {
        return "CleanupEvent{" +
                "users=" + users +
                ", hosts=" + hosts +
                ", roles=" + roles +
                ", accountId='" + accountId + '\'' +
                ", operationId='" + operationId + '\'' +
                ", clusterName='" + clusterName + '\'' +
                '}';
    }
}

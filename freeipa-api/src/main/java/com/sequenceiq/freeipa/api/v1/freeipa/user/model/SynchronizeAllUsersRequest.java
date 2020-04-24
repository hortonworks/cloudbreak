package com.sequenceiq.freeipa.api.v1.freeipa.user.model;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sequenceiq.freeipa.api.v1.freeipa.user.doc.UserModelDescriptions;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel("SynchronizeAllUsersV1Request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SynchronizeAllUsersRequest extends SynchronizeOperationRequestBase {
    @ApiModelProperty(value = UserModelDescriptions.USERSYNC_USER_CRNS)
    private Set<String> machineUsers = new HashSet<>();

    @ApiModelProperty(value = UserModelDescriptions.USERSYNC_USER_CRNS)
    private Set<String> users = new HashSet<>();

    @ApiModelProperty(value = UserModelDescriptions.USERSYNC_ACCOUNT_ID)
    private String accountId;

    public SynchronizeAllUsersRequest() {
    }

    public SynchronizeAllUsersRequest(Set<String> environments, Set<String> users) {
        super(environments);
        this.users = users;
    }

    public Set<String> getMachineUsers() {
        return machineUsers;
    }

    public void setMachineUsers(Set<String> machineUsers) {
        this.machineUsers = machineUsers;
    }

    public Set<String> getUsers() {
        return users;
    }

    public void setUsers(Set<String> users) {
        this.users = users;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    @Override
    public String toString() {
        return "SynchronizeAllUsersRequest{"
                + "machineUsers=" + machineUsers
                + ", users=" + users
                + ", accountId=" + accountId
                + ", " + super.fieldsToString()
                + '}';
    }
}

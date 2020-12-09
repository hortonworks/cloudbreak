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
    /**
     * @deprecated
     * defining users for sync is not supported anymore
     */
    @Deprecated
    @ApiModelProperty(value = UserModelDescriptions.USERSYNC_MACHINEUSER_CRNS)
    private Set<String> machineUsers = new HashSet<>();

    /**
     * @deprecated
     * defining users for sync is not supported anymore
     */
    @Deprecated
    @ApiModelProperty(value = UserModelDescriptions.USERSYNC_USER_CRNS)
    private Set<String> users = new HashSet<>();

    /**
     * @deprecated
     * defining users for sync is not supported anymore
     */
    @Deprecated
    @ApiModelProperty(value = UserModelDescriptions.DELETED_WORKLOAD_USERS)
    private Set<String> deletedWorkloadUsers =  new HashSet<>();

    @ApiModelProperty(value = UserModelDescriptions.USERSYNC_ACCOUNT_ID)
    private String accountId;

    @ApiModelProperty(value = UserModelDescriptions.WORKLOAD_CREDENTIALS_UPDATE_TYPE)
    private WorkloadCredentialsUpdateType workloadCredentialsUpdateType = WorkloadCredentialsUpdateType.UPDATE_IF_CHANGED;

    public SynchronizeAllUsersRequest() {
    }

    public SynchronizeAllUsersRequest(Set<String> environments, Set<String> users) {
        super(environments);
        this.users = users;
    }

    /**
     * @deprecated
     * defining users for sync is not supported anymore
     */
    @Deprecated
    public Set<String> getMachineUsers() {
        return machineUsers;
    }

    /**
     * @deprecated
     * defining users for sync is not supported anymore
     */
    @Deprecated
    public void setMachineUsers(Set<String> machineUsers) {
        this.machineUsers = machineUsers;
    }

    /**
     * @deprecated
     * defining users for sync is not supported anymore
     */
    @Deprecated
    public Set<String> getUsers() {
        return users;
    }

    /**
     * @deprecated
     * defining users for sync is not supported anymore
     */
    @Deprecated
    public void setUsers(Set<String> users) {
        this.users = users;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public WorkloadCredentialsUpdateType getWorkloadCredentialsUpdateType() {
        return workloadCredentialsUpdateType;
    }

    public void setWorkloadCredentialsUpdateType(WorkloadCredentialsUpdateType workloadCredentialsUpdateType) {
        this.workloadCredentialsUpdateType = workloadCredentialsUpdateType;
    }

    /**
     * @deprecated
     * defining users for sync is not supported anymore
     */
    @Deprecated
    public Set<String> getDeletedWorkloadUsers() {
        return deletedWorkloadUsers;
    }

    /**
     * @deprecated
     * defining users for sync is not supported anymore
     */
    @Deprecated
    public void setDeletedWorkloadUsers(Set<String> deletedWorkloadUsers) {
        this.deletedWorkloadUsers = deletedWorkloadUsers;
    }

    @Override
    public String toString() {
        return "SynchronizeAllUsersRequest{"
                + "machineUsers=" + machineUsers
                + ", users=" + users
                + ", deletedWorkloadUsers=" + deletedWorkloadUsers
                + ", accountId=" + accountId
                + ", workloadCredentialsUpdateType=" + workloadCredentialsUpdateType
                + ", " + super.fieldsToString()
                + '}';
    }
}

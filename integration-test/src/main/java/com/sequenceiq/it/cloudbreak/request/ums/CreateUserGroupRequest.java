package com.sequenceiq.it.cloudbreak.request.ums;

public class CreateUserGroupRequest {

    private String groupName;

    private String accountId;

    private String memberCrn;

    public CreateUserGroupRequest() {
    }

    public CreateUserGroupRequest(String groupName, String accountId) {
        this.groupName = groupName;
        this.accountId = accountId;
    }

    public CreateUserGroupRequest(String groupName, String accountId, String memberCrn) {
        this.groupName = groupName;
        this.accountId = accountId;
        this.memberCrn = memberCrn;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getMember() {
        return memberCrn;
    }

    public void setMember(String memberCrn) {
        this.memberCrn = memberCrn;
    }

    @Override
    public String toString() {
        return "CreateUserGroupRequest{" +
                "groupName='" + groupName + '\'' +
                ", accountId='" + accountId + '\'' +
                ", member='" + memberCrn + '\'' +
                '}';
    }
}

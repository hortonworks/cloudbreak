package com.sequenceiq.cloudbreak.auth.altus.model;

public class MachineUserRequest {

    private String name;

    private String actorCrn;

    private String accountId;

    private CdpAccessKeyType cdpAccessKeyType;

    public String getName() {
        return name;
    }

    public MachineUserRequest setName(String name) {
        this.name = name;
        return this;
    }

    public String getActorCrn() {
        return actorCrn;
    }

    public MachineUserRequest setActorCrn(String actorCrn) {
        this.actorCrn = actorCrn;
        return this;
    }

    public String getAccountId() {
        return accountId;
    }

    public MachineUserRequest setAccountId(String accountId) {
        this.accountId = accountId;
        return this;
    }

    public CdpAccessKeyType getCdpAccessKeyType() {
        return cdpAccessKeyType;
    }

    public MachineUserRequest setCdpAccessKeyType(CdpAccessKeyType cdpAccessKeyType) {
        this.cdpAccessKeyType = cdpAccessKeyType;
        return this;
    }
}

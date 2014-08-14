package com.sequenceiq.cloudbreak.controller.json;

import java.util.ArrayList;
import java.util.List;

public class AccountJson implements JsonEntity {
    private String accountName;
    private List<UserJson> users = new ArrayList<>();

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public List<UserJson> getUsers() {
        return users;
    }

    public void setUsers(List<UserJson> users) {
        this.users = users;
    }

}

package com.sequenceiq.cloudbreak.controller.json;

import java.util.ArrayList;
import java.util.List;

public class CompanyJson implements JsonEntity {
    private String companyName;
    private List<UserJson> users = new ArrayList<>();

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public List<UserJson> getUsers() {
        return users;
    }

    public void setUsers(List<UserJson> users) {
        this.users = users;
    }
}

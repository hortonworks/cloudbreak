package com.sequenceiq.cloudbreak.cloud.aws.common.util;

import java.util.List;

class TestCollectable {
    private String token;

    private List<String> listOfSomething;

    TestCollectable(String token, List<String> listOfSomething) {
        this.token = token;
        this.listOfSomething = listOfSomething;
    }

    public String getToken() {
        return token;
    }

    public List<String> getListOfSomething() {
        return listOfSomething;
    }

}

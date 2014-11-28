package com.sequenceiq.cloudbreak.it;

public enum CloudProvider {
    AWS("aws"),
    GCC("gcc"),
    AZURE("azure");

    private String nickName;

    CloudProvider(String nickName) {
        this.nickName = nickName;
    }

    public String nickName() {
        return this.nickName;
    }
}

package com.sequenceiq.cloudbreak.common.service;

public enum LockNumber {

    // 'Q' + 'u' + 'a' + 'r' + 't' + 'z' = 647
    QUARTZ(647);

    private final int lockNumber;

    LockNumber(int lockNumber) {
        this.lockNumber = lockNumber;
    }

    public int getLockNumber() {
        return lockNumber;
    }
}
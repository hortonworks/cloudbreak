package com.sequenceiq.cloudbreak.common.service;

public enum LockNumber {

    // 'Q' + 'u' + 'a' + 'r' + 't' + 'z' = 647
    QUARTZ(647),
    // 'B' + 'l' + 'u' + 'e' + 'p' + 'r' + 'i' + 'n' + 't' = 949
    BLUEPRINT(949);

    private final int lockNumber;

    LockNumber(int lockNumber) {
        this.lockNumber = lockNumber;
    }

    public int getLockNumber() {
        return lockNumber;
    }
}
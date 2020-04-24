package com.sequenceiq.it.cloudbreak.util.wait.service;

public enum WaitResult {
    TIMEOUT, EXIT, SUCCESS, FAILURE;

    public static boolean isSuccess(WaitResult waitResult) {
        return SUCCESS.equals(waitResult);
    }

    public static boolean isExited(WaitResult waitResult) {
        return EXIT.equals(waitResult);
    }

    public static boolean isTimeout(WaitResult waitResult) {
        return TIMEOUT.equals(waitResult);
    }

    public static boolean isFailure(WaitResult waitResult) {
        return FAILURE.equals(waitResult);
    }

}
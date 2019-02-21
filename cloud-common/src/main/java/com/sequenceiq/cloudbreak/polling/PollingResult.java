package com.sequenceiq.cloudbreak.polling;

public enum PollingResult {
    TIMEOUT, EXIT, SUCCESS, FAILURE;

    public static boolean isSuccess(PollingResult pollingResult) {
        return SUCCESS.equals(pollingResult);
    }

    public static boolean isExited(PollingResult pollingResult) {
        return EXIT.equals(pollingResult);
    }

    public static boolean isTimeout(PollingResult pollingResult) {
        return TIMEOUT.equals(pollingResult);
    }

    public static boolean isFailure(PollingResult pollingResult) {
        return FAILURE.equals(pollingResult);
    }

}
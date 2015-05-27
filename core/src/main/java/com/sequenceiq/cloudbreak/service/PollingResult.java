package com.sequenceiq.cloudbreak.service;

public enum PollingResult {
    TIMEOUT, EXIT, SUCCESS;

    public static boolean isSuccess(PollingResult pollingResult) {
        return PollingResult.SUCCESS.equals(pollingResult);
    }

    public static boolean isExited(PollingResult pollingResult) {
        return PollingResult.EXIT.equals(pollingResult);
    }

    public static boolean isTimeout(PollingResult pollingResult) {
        return PollingResult.TIMEOUT.equals(pollingResult);
    }

}
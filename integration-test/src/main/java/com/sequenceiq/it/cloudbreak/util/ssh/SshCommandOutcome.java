package com.sequenceiq.it.cloudbreak.util.ssh;

import java.util.Optional;

public record SshCommandOutcome(
        String instanceIp,
        int returnCode,
        String commandOutput,
        Result result,
        Throwable throwable
) {
    public enum Result {
        EXECUTED,
        UNREACHABLE,
        AUTH_FAILURE
    }

    public static SshCommandOutcome executed(String instanceIp, int returnCode, String commandResult) {
        return new SshCommandOutcome(instanceIp, returnCode, commandResult, Result.EXECUTED, null);
    }

    public static SshCommandOutcome unreachable(String instanceIp, Throwable throwable) {
        return new SshCommandOutcome(instanceIp, -1, null, Result.UNREACHABLE, throwable);
    }

    public static SshCommandOutcome authFailure(String instanceIp, Throwable throwable) {
        return new SshCommandOutcome(instanceIp, -1, null, Result.AUTH_FAILURE, throwable);
    }

    public boolean returnCodeSuccess() {
        return returnCode == 0;
    }

    public boolean executed() {
        return Result.EXECUTED.equals(result);
    }

    public Optional<String> exceptionMessage() {
        return Optional.ofNullable(throwable).map(Throwable::getMessage);
    }
}

package com.sequenceiq.it.cloudbreak.util.ssh;

import static java.lang.String.format;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;

public record SshCommandOutcome(
        String instanceIp,
        Result result,
        int returnCode,
        String commandOutput,
        Throwable throwable
) {
    public static final int MISSING_RETURN_CODE = -1;

    private static final Logger LOGGER = LoggerFactory.getLogger(SshCommandOutcome.class);

    private static final String DEFAULT_OPERATION = "SSH command";

    public enum Result {
        EXECUTED,
        UNREACHABLE,
        AUTH_FAILURE,
        TIMED_OUT
    }

    public SshCommandOutcome {
        if (StringUtils.isBlank(instanceIp)) {
            throw new IllegalArgumentException("instanceIp cannot be blank");
        }
        if (result == null) {
            throw new IllegalArgumentException("result cannot be null");
        }
        if (result == Result.EXECUTED) {
            if (returnCode < 0 && returnCode != MISSING_RETURN_CODE) {
                throw new IllegalArgumentException("returnCode must be non-negative or MISSING_RETURN_CODE for a command with result 'EXECUTED'");
            }
            if (commandOutput == null) {
                throw new IllegalArgumentException("commandOutput must not be null for a command with result 'EXECUTED'");
            }
            if (throwable != null) {
                throw new IllegalArgumentException("throwable must be null for a command with result 'EXECUTED'", throwable);
            }
        } else {
            if (returnCode != MISSING_RETURN_CODE) {
                throw new IllegalArgumentException("returnCode must be MISSING_RETURN_CODE for a command with result that is not 'EXECUTED'");
            }
            if (commandOutput != null) {
                throw new IllegalArgumentException("commandOutput must be null for a command with result that is not 'EXECUTED'");
            }
            if (throwable == null) {
                throw new IllegalArgumentException("throwable must not be null for a command with result that is not 'EXECUTED'");
            }
        }
    }

    public static SshCommandOutcome executed(String instanceIp, int returnCode, String commandResult) {
        return new SshCommandOutcome(instanceIp, Result.EXECUTED, returnCode, commandResult, null);
    }

    public static SshCommandOutcome unreachable(String instanceIp, Throwable throwable) {
        return new SshCommandOutcome(instanceIp, Result.UNREACHABLE, MISSING_RETURN_CODE, null, throwable);
    }

    public static SshCommandOutcome authFailure(String instanceIp, Throwable throwable) {
        return new SshCommandOutcome(instanceIp, Result.AUTH_FAILURE, MISSING_RETURN_CODE, null, throwable);
    }

    public static SshCommandOutcome timedOut(String instanceIp, Throwable throwable) {
        return new SshCommandOutcome(instanceIp, Result.TIMED_OUT, MISSING_RETURN_CODE, null, throwable);
    }

    /**
     * @return whether the command was executed and returned a zero exit code
     */
    public boolean returnCodeSuccess() {
        return executed() && returnCode == 0;
    }

    public boolean executed() {
        return result == Result.EXECUTED;
    }

    public Optional<String> exceptionMessage() {
        return Optional.ofNullable(throwable).map(Throwable::getMessage);
    }

    /**
     * Fails the test if the command could not be executed on the instance because of an SSH connection or authentication failure, or because it
     * timed out. The exit code of the command is not checked.
     */
    public SshCommandOutcome requireExecuted() {
        return requireExecuted(DEFAULT_OPERATION);
    }

    /**
     * Fails the test if the command could not be executed on the instance because of an SSH connection or authentication failure, or because it
     * timed out. The exit code of the command is not checked.
     *
     * @param operation the description of what the command was meant to do, used as the subject of the failure message
     */
    public SshCommandOutcome requireExecuted(String operation) {
        if (!executed()) {
            String message = format("%s could not be executed on '%s' instance, SSH %s: %s", operation, instanceIp, result, exceptionMessage().orElse(null));
            Log.error(LOGGER, message);
            throw new TestFailException(message, throwable);
        }
        return this;
    }

    /**
     * Fails the test if the command could not be executed on the instance, or if it was executed but returned a non-zero (or missing) exit code.
     */
    public SshCommandOutcome requireSuccess() {
        return requireSuccess(DEFAULT_OPERATION);
    }

    /**
     * Fails the test if the command could not be executed on the instance, or if it was executed but returned a non-zero (or missing) exit code.
     *
     * @param operation the description of what the command was meant to do, used as the subject of the failure message
     */
    public SshCommandOutcome requireSuccess(String operation) {
        requireExecuted(operation);
        if (!returnCodeSuccess()) {
            String message = format("%s failed on '%s' instance! Exit code: %d, output: %s", operation, instanceIp, returnCode, commandOutput);
            Log.error(LOGGER, message);
            throw new TestFailException(message);
        }
        return this;
    }
}

package com.sequenceiq.it.cloudbreak.util.wait.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsecutiveAttemptsInDesiredStatesWaitOperationChecker<T extends WaitObject> extends WaitOperationChecker<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsecutiveAttemptsInDesiredStatesWaitOperationChecker.class);

    private final Integer consecutivePollingAttemptsInDesiredState;

    private int exitConsecutiveAttemptsInDesiredStates;

    private int checkStatusConsecutiveAttemptsInDesiredStates;

    public ConsecutiveAttemptsInDesiredStatesWaitOperationChecker(Integer consecutivePollingAttemptsInDesiredState) {
        this.consecutivePollingAttemptsInDesiredState = consecutivePollingAttemptsInDesiredState;
        this.exitConsecutiveAttemptsInDesiredStates = 0;
        this.checkStatusConsecutiveAttemptsInDesiredStates = 0;
    }

    @Override
    public boolean exitWaiting(T waitObject) {
        boolean exitWaiting = super.exitWaiting(waitObject);
        if (exitWaiting) {
            LOGGER.info("During exit check the number of consecutive polling attempts in desired state: {}", exitConsecutiveAttemptsInDesiredStates);
            exitConsecutiveAttemptsInDesiredStates += 1;
        } else {
            exitConsecutiveAttemptsInDesiredStates = 0;
        }
        boolean reachedRequestedNumberOfAttemptsInDesiredStates = consecutivePollingAttemptsInDesiredState < exitConsecutiveAttemptsInDesiredStates;
        return exitWaiting && reachedRequestedNumberOfAttemptsInDesiredStates;
    }

    @Override
    public boolean checkStatus(T waitObject) {
        boolean checkStatus = super.checkStatus(waitObject);
        if (checkStatus) {
            LOGGER.info("During status check the number of consecutive polling attempts in desired state: {}", checkStatusConsecutiveAttemptsInDesiredStates);
            checkStatusConsecutiveAttemptsInDesiredStates += 1;
        } else {
            checkStatusConsecutiveAttemptsInDesiredStates = 0;
        }
        boolean reachedRequestedNumberOfAttemptsInDesiredStates = consecutivePollingAttemptsInDesiredState < checkStatusConsecutiveAttemptsInDesiredStates;
        return checkStatus && reachedRequestedNumberOfAttemptsInDesiredStates;
    }
}

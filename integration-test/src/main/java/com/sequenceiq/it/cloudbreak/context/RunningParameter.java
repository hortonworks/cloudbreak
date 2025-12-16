package com.sequenceiq.it.cloudbreak.context;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.polling.TimeoutChecker;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;

@Component
public class RunningParameter {

    private static final int MIN_CONSECUTIVE_POLLING_ATTEMPTS = 1;

    private static final int MAX_CONSECUTIVE_POLLING_ATTEMPTS = 10;

    private CloudbreakUser who;

    private boolean skipOnFail = true;

    private String key;

    private boolean logError = true;

    private String expectedMessage;

    private String expectedPayload;

    private Class urlClass;

    private Method urlMethod;

    private FlowWaitConfig waitForFlow = FlowWaitConfig.WAIT_SUCCESS;

    private Duration pollingInterval;

    private Set<Enum<?>> ignoredStatuses;

    private TimeoutChecker timeoutChecker;

    private Integer consecutivePollingAttemptsInDesiredState;

    public static RunningParameter emptyRunningParameter() {
        return new RunningParameter();
    }

    public static RunningParameter force() {
        return new RunningParameter()
                .withSkipOnFail(false);
    }

    public static RunningParameter who(CloudbreakUser cloudbreakUser) {
        return new RunningParameter()
                .withWho(cloudbreakUser);
    }

    public static RunningParameter key(String key) {
        return new RunningParameter()
                .withKey(key);
    }

    public static RunningParameter withoutLogError() {
        return new RunningParameter()
                .withLogError(false);
    }

    public static RunningParameter expectedMessage(String message) {
        return new RunningParameter()
                .withExpectedMessage(message);
    }

    public static RunningParameter expectedPayload(String payload) {
        return new RunningParameter()
                .withExpectedPayload(payload);
    }

    public static RunningParameter httpMockUrl(Class url, Method method) {
        return new RunningParameter()
                .withHttpMockUrl(url, method);
    }

    public static RunningParameter httpMockUrl(Class url, String method) {
        return new RunningParameter()
                .withHttpMockUrl(url, method);
    }

    public static RunningParameter waitForFlow() {
        return new RunningParameter().withWaitForFlow(true);
    }

    public static RunningParameter doNotWaitForFlow() {
        return new RunningParameter().withWaitForFlow(false);
    }

    public static RunningParameter waitForFlowSuccess() {
        return new RunningParameter().withWaitForFlowSuccess();
    }

    public static RunningParameter waitForFlowFail() {
        return new RunningParameter().withWaitForFlowFail();
    }

    public static RunningParameter pollingInterval(Duration pollingInterval) {
        return new RunningParameter().withPollingInterval(pollingInterval);
    }

    public static RunningParameter ignoredStatues(Set<Enum<?>> ignoredStatuses) {
        return new RunningParameter().withIgnoredStatues(ignoredStatuses);
    }

    public static RunningParameter timeoutChecker(TimeoutChecker timeoutChecker) {
        return new RunningParameter().withTimeoutChecker(timeoutChecker);
    }

    public static RunningParameter consecutivePollingAttemptsInDesiredState(Integer numberOfAttempts) {
        return new RunningParameter().withConsecutivePollingAttemptsInDesiredState(numberOfAttempts);
    }

    public CloudbreakUser getWho() {
        return who;
    }

    public RunningParameter withWho(CloudbreakUser cloudbreakUser) {
        who = cloudbreakUser;
        return this;
    }

    public boolean isSkipOnFail() {
        return skipOnFail;
    }

    public boolean isLogError() {
        return logError;
    }

    public boolean isWaitForFlow() {
        return FlowWaitConfig.WAIT_SUCCESS.equals(waitForFlow) || FlowWaitConfig.WAIT_FAILURE.equals(waitForFlow);
    }

    public boolean isWaitForFlowSuccess() {
        return FlowWaitConfig.WAIT_SUCCESS.equals(waitForFlow);
    }

    public boolean isWaitForFlowFail() {
        return FlowWaitConfig.WAIT_FAILURE.equals(waitForFlow);
    }

    public Duration getPollingInterval() {
        return pollingInterval;
    }

    public <E extends Enum<E>> Set<E> getIgnoredStatuses() {
        if (ignoredStatuses != null) {
            return new HashSet<>((Collection<? extends E>) ignoredStatuses);
        }
        return new HashSet<>();
    }

    public RunningParameter withSkipOnFail(boolean skipOnFail) {
        this.skipOnFail = skipOnFail;
        return this;
    }

    public RunningParameter withLogError(boolean logError) {
        this.logError = logError;
        return this;
    }

    public RunningParameter withExpectedMessage(String message) {
        expectedMessage = message;
        return this;
    }

    public RunningParameter withExpectedPayload(String payload) {
        expectedPayload = payload;
        return this;
    }

    public String getKey() {
        return key;
    }

    public String getExpectedMessage() {
        return expectedMessage;
    }

    public String getExpectedPayload() {
        return expectedPayload;
    }

    public Class getUrlClass() {
        return urlClass;
    }

    public Method getUrlMethod() {
        return urlMethod;
    }

    public TimeoutChecker getTimeoutChecker() {
        return timeoutChecker;
    }

    public Integer getConsecutivePollingAttemptsInDesiredState() {
        return consecutivePollingAttemptsInDesiredState;
    }

    public RunningParameter withKey(String key) {
        this.key = key;
        return this;
    }

    public RunningParameter withHttpMockUrl(Class urlClass, Method method) {
        this.urlClass = urlClass;
        urlMethod = method;
        return this;
    }

    public RunningParameter withHttpMockUrl(Class urlClass, String method) {
        this.urlClass = urlClass;

        urlMethod = Arrays.stream(urlClass.getMethods())
                .filter(m -> m.getName().equals(method))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(urlClass + "does not have method " + method));
        return this;
    }

    public RunningParameter withWaitForFlow(boolean waitForFlow) {
        this.waitForFlow = waitForFlow ? FlowWaitConfig.WAIT_SUCCESS : FlowWaitConfig.NOT_WAIT;
        return this;
    }

    public RunningParameter withoutWaitForFlow() {
        this.waitForFlow = FlowWaitConfig.NOT_WAIT;
        return this;
    }

    public RunningParameter withWaitForFlowSuccess() {
        this.waitForFlow = FlowWaitConfig.WAIT_SUCCESS;
        return this;
    }

    public RunningParameter withWaitForFlowFail() {
        this.waitForFlow = FlowWaitConfig.WAIT_FAILURE;
        return this;
    }

    public RunningParameter withPollingInterval(Duration pollingInterval) {
        this.pollingInterval = pollingInterval;
        return this;
    }

    public RunningParameter withIgnoredStatues(Set<Enum<?>> ignoredStatuses) {
        this.ignoredStatuses = ignoredStatuses;
        return this;
    }

    public RunningParameter withTimeoutChecker(TimeoutChecker timeoutChecker) {
        this.timeoutChecker = timeoutChecker;
        return this;
    }

    public RunningParameter withConsecutivePollingAttemptsInDesiredState(Integer numberOfAttempts) {
        if (MIN_CONSECUTIVE_POLLING_ATTEMPTS > numberOfAttempts || MAX_CONSECUTIVE_POLLING_ATTEMPTS < numberOfAttempts) {
            String message = String.format("The parameter value should be between %s and %s!", MIN_CONSECUTIVE_POLLING_ATTEMPTS,
                    MAX_CONSECUTIVE_POLLING_ATTEMPTS);
            throw new IllegalArgumentException(message);
        }
        this.consecutivePollingAttemptsInDesiredState = numberOfAttempts;
        return this;
    }

    public enum FlowWaitConfig {
        WAIT_SUCCESS,
        WAIT_FAILURE,
        NOT_WAIT;
    }
}

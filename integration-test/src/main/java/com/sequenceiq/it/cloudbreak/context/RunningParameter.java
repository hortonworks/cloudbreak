package com.sequenceiq.it.cloudbreak.context;

import java.lang.reflect.Method;
import java.util.Arrays;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;

@Component
public class RunningParameter {

    private CloudbreakUser who;

    private boolean skipOnFail = true;

    private boolean doAsAdmin;

    private String key;

    private boolean logError = true;

    private String expectedMessage;

    private Class urlClass;

    private Method urlMethod;

    private boolean waitForFlow = true;

    @Inject
    private TestContext testContext;

    public CloudbreakUser getWho() {
        if (doAsAdmin) {
            if (testContext.realUmsUserCacheReadyToUse()) {
                return testContext.getRealUmsAdmin();
            }
        }
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
        return waitForFlow;
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

    public String getKey() {
        return key;
    }

    public String getExpectedMessage() {
        return expectedMessage;
    }

    public Class getUrlClass() {
        return urlClass;
    }

    public Method getUrlMethod() {
        return urlMethod;
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

    public RunningParameter switchToAdmin() {
        this.doAsAdmin = true;
        return this;
    }

    public RunningParameter swithcToActor() {
        this.doAsAdmin = false;
        return this;
    }

    public RunningParameter withWaitForFlow(boolean waitForFlow) {
        this.waitForFlow = waitForFlow;
        return this;
    }

    public RunningParameter withWaitForFlow() {
        this.waitForFlow = true;
        return this;
    }

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
}
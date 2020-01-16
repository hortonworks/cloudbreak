package com.sequenceiq.it.cloudbreak.context;

import java.lang.reflect.Method;
import java.util.Arrays;

import com.sequenceiq.it.cloudbreak.actor.Actor;

public class RunningParameter {

    private Actor who;

    private boolean skipOnFail = true;

    private String key;

    private boolean logError = true;

    private String expectedMessage;

    private Class urlClass;

    private Method urlMethod;

    public Actor getWho() {
        return who;
    }

    public RunningParameter withWho(Actor who) {
        this.who = who;
        return this;
    }

    public boolean isSkipOnFail() {
        return skipOnFail;
    }

    public boolean isLogError() {
        return logError;
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

    public static RunningParameter emptyRunningParameter() {
        return new RunningParameter();
    }

    public static RunningParameter force() {
        return new RunningParameter()
                .withSkipOnFail(false);
    }

    public static RunningParameter who(Actor who) {
        return new RunningParameter()
                .withWho(who);
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
}

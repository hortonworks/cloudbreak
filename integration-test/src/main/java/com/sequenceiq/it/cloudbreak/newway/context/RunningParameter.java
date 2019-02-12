package com.sequenceiq.it.cloudbreak.newway.context;

import java.util.function.Consumer;

public class RunningParameter {

    private String who;

    private boolean skipOnFail = true;

    private String key;

    private boolean logError = true;

    private String expectedMessage;

    private Consumer<Exception> exceptionConsumer;

    public String getWho() {
        return who;
    }

    public RunningParameter withWho(String who) {
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

    public RunningParameter withExceptionConsumer(Consumer<Exception> exceptionConsumer) {
        this.exceptionConsumer = exceptionConsumer;
        return this;
    }

    public Consumer<Exception> getExceptionConsumer() {
        return exceptionConsumer;
    }

    public String getKey() {
        return key;
    }

    public String getExpectedMessage() {
        return expectedMessage;
    }

    public RunningParameter withKey(String key) {
        this.key = key;
        return this;
    }

    public static RunningParameter emptyRunningParameter() {
        return new RunningParameter();
    }

    public static RunningParameter force() {
        return new RunningParameter()
                .withSkipOnFail(false);
    }

    public static RunningParameter who(String who) {
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

    public static RunningParameter exceptionConsumer(Consumer<Exception> exceptionConsumer) {
        return new RunningParameter()
                .withExceptionConsumer(exceptionConsumer);
    }
}

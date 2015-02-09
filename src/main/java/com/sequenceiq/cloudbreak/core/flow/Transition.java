package com.sequenceiq.cloudbreak.core.flow;

public class Transition {
    private String current;
    private String next;
    private String failure;

    public Transition(String current, String next, String failure) {
        this.current = current;
        this.next = next;
        this.failure = failure;
    }

    public String getCurrent() {
        return current;
    }

    public String getNext() {
        return next;
    }

    public String getFailure() {
        return failure;
    }
}


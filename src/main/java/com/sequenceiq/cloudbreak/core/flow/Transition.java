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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Transition{");
        sb.append("current='").append(current).append('\'');
        sb.append(", next='").append(next).append('\'');
        sb.append(", failure='").append(failure).append('\'');
        sb.append('}');
        return sb.toString();
    }
}


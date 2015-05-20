package com.sequenceiq.cloudbreak.core.flow;

public final class TransitionFactory {

    private TransitionFactory() {
    }

    public static Transition createTransition(String current, String next, String failure) {
        if (current == null || next == null || failure == null) {
            throw new IllegalStateException("Invalid transition definition");
        }
        return new Transition(current, next, failure);
    }
}


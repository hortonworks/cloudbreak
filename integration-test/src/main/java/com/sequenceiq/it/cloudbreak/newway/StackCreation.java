package com.sequenceiq.it.cloudbreak.newway;

public class StackCreation extends Stack {

    private final StackEntity stack;

    public StackCreation(StackEntity s) {
        stack = s;
    }

    @Override
    public void setCreationStrategy(Strategy strategy) {
        stack.setCreationStrategy(strategy);
    }

    public StackEntity getStack() {
        return stack;
    }
}

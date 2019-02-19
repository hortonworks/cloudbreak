package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;

public class StackCreation extends Stack {

    private final StackTestDto stack;

    public StackCreation(StackTestDto s) {
        stack = s;
    }

    @Override
    public void setCreationStrategy(Strategy strategy) {
        stack.setCreationStrategy(strategy);
    }

    public StackTestDto getStack() {
        return stack;
    }
}

package com.sequenceiq.freeipa.service.rotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.orchestrator.StackBasedExitCriteriaModel;

class ExitCriteriaProviderTest {

    private static final Long STACK_ID = Long.MAX_VALUE;

    private ExitCriteriaProvider underTest = new ExitCriteriaProvider();

    @BeforeEach
    public void setUp() {
        InMemoryStateStore.deleteStack(STACK_ID);
    }

    @Test
    public void testPollableState() {
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(Status.AVAILABLE);
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setStackStatus(stackStatus);

        ExitCriteriaModel exitCriteriaModel = underTest.get(stack);

        assertInstanceOf(StackBasedExitCriteriaModel.class, exitCriteriaModel);
        assertEquals(Optional.of(STACK_ID), ((StackBasedExitCriteriaModel) exitCriteriaModel).getStackId());
        assertEquals(PollGroup.POLLABLE, InMemoryStateStore.getStack(STACK_ID));
    }

    @Test
    public void testCancelledState() {
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(Status.DELETE_COMPLETED);
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setStackStatus(stackStatus);

        ExitCriteriaModel exitCriteriaModel = underTest.get(stack);

        assertInstanceOf(StackBasedExitCriteriaModel.class, exitCriteriaModel);
        assertEquals(Optional.of(STACK_ID), ((StackBasedExitCriteriaModel) exitCriteriaModel).getStackId());
        assertEquals(PollGroup.CANCELLED, InMemoryStateStore.getStack(STACK_ID));
    }
}
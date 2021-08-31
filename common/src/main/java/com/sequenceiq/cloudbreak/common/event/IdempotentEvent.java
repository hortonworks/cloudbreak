package com.sequenceiq.cloudbreak.common.event;

/**
 * If a flow / flowchain trigger event implements this method then the
 * flow engine will check if the current running flow / flow chain was
 * started with the same event based on the {@link IdempotentEvent#equalsEvent}
 * method.
 */
public interface IdempotentEvent<T extends IdempotentEvent<T>> extends Acceptable, Selectable {

    /**
     * Checks if the two events represent the same logical flow / flow chain.
     * @param other - payload of the running flow / flow chain's trigger event
     * @return {@code true} if the caller treats the running flow / flow chain as
     * the same that it wanted to trigger, {@code false} if the running flow / flow
     * chain is not the same and should be handled as a conflict.
     */
    boolean equalsEvent(T other);
}

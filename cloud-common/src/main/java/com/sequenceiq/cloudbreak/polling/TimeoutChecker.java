package com.sequenceiq.cloudbreak.polling;

public interface TimeoutChecker {
    /**
     * Do the calculations needed for timeout checking
     * @return true if the operation is timed out
     */
    boolean checkTimeout();
}

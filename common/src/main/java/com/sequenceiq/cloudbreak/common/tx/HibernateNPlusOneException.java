package com.sequenceiq.cloudbreak.common.tx;

/**
 * If you see this error, then probably your entity model or the query what you are trying to execute
 * are not ideal.
 */
public class HibernateNPlusOneException extends RuntimeException {

    public HibernateNPlusOneException(int queryCount) {
        super(String.format("You have executed %d queries in a single transaction, " +
                "please doublecheck the entity relationship!", queryCount));
    }
}

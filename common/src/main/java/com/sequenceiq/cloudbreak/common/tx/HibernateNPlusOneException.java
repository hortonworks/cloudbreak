package com.sequenceiq.cloudbreak.common.tx;

/**
 * If you see this error, then probably your entity model or the query what you are trying to execute
 * are not ideal.
 */
public class HibernateNPlusOneException extends RuntimeException {

    public HibernateNPlusOneException(String message) {
        super(message);
    }
}

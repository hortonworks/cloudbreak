package com.sequenceiq.freeipa.client.auth;

import com.sequenceiq.freeipa.client.FreeIpaClientException;

/**
 * Can indicate a number of errors from the FreeIPA server, including:<ul>
 *
 * <li>Invalid user name
 * <li>Invalid realm
 * <li>Insufficient privileges/access rights
 *
 * </ul>
 */
public class InvalidUserOrRealmException extends FreeIpaClientException {
    public InvalidUserOrRealmException() {
        super("Invalid user or realm");
    }

    public InvalidUserOrRealmException(String message) {
        super(message);
    }

    public InvalidUserOrRealmException(Throwable cause) {
        super(cause);
    }

    public InvalidUserOrRealmException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidUserOrRealmException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

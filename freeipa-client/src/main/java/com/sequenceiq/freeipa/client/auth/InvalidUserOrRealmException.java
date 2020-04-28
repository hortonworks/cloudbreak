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

    @Override
    public boolean isClientUnusable() {
        return true;
    }
}

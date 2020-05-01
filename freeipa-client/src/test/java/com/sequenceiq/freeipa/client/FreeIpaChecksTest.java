package com.sequenceiq.freeipa.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FreeIpaChecksTest {

    @Test
    void checkUserNotProtectedPasses() throws FreeIpaClientException {
        String errMessage = "user protected";
        FreeIpaChecks.checkUserNotProtected("clark", () -> errMessage);
    }

    @Test
    void checkUserNotProtectedThrows() {
        String errMessage = "user protected";
        for (String user : FreeIpaChecks.IPA_PROTECTED_USERS) {
            Exception e = assertThrows(FreeIpaClientException.class, () ->
                FreeIpaChecks.checkUserNotProtected(user, () -> errMessage)
            );
            assertEquals(errMessage, e.getMessage());
        }
    }

    @Test
    void checkGroupNotProtectedPasses() throws FreeIpaClientException {
        String errMessage = "group protected";
        FreeIpaChecks.checkGroupNotProtected("superheroes", () -> errMessage);
    }

    @Test
    void checkGroupNotProtectedThrows() {
        String errMessage = "group protected";
        for (String group : FreeIpaChecks.IPA_PROTECTED_GROUPS) {
            Exception e = assertThrows(FreeIpaClientException.class, () ->
                FreeIpaChecks.checkGroupNotProtected(group, () -> errMessage)
            );
            assertEquals(errMessage, e.getMessage());
        }
    }

    @Test
    void checkGroupNotUnmanagedPasses() throws FreeIpaClientException {
        String errMessage = "group unmanaged";
        FreeIpaChecks.checkGroupNotUnmanaged("superheroes", () -> errMessage);
    }

    @Test
    void checkGroupNotUnmanagedThrows() {
        String errMessage =  "group unmanaged";
        for (String group : FreeIpaChecks.IPA_UNMANAGED_GROUPS) {
            Exception e = assertThrows(FreeIpaClientException.class, () ->
                FreeIpaChecks.checkGroupNotUnmanaged(group, () -> errMessage)
            );
            assertEquals(errMessage, e.getMessage());
        }
    }
}
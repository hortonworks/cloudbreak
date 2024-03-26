package com.sequenceiq.cloudbreak.rotation.common;

import jakarta.ws.rs.ProcessingException;

public class RotationPollingSvcOutageUtils {

    private RotationPollingSvcOutageUtils() {

    }

    public static void pollWithSvcOutageErrorHandling(Runnable runnable, Class<? extends Exception> originalException) {
        try {
            runnable.run();
        } catch (Exception e) {
            if (originalException.isInstance(e) && e.getCause() instanceof ProcessingException) {
                throw new RotationPollerExternalSvcOutageException("External service is not reachable, not responding correctly: ", e);
            }
            throw e;
        }
    }
}

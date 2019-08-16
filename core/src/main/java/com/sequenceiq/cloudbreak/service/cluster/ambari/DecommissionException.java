package com.sequenceiq.cloudbreak.service.cluster.ambari;

public class DecommissionException extends RuntimeException {
    public DecommissionException(Throwable cause) {
        super(cause);
    }

    public DecommissionException(String message) {
        super(message);
    }
}

package com.sequenceiq.freeipa.service.freeipa.user;

import java.util.Optional;

public class AcceptResult {
    private static final AcceptResult ACCEPTED = new AcceptResult(true, Optional.empty());

    private final boolean accepted;

    private final Optional<String> rejectionMessage;

    public AcceptResult(boolean accepted, Optional<String> rejectionMessage) {
        this.accepted = accepted;
        this.rejectionMessage = rejectionMessage;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public Optional<String> getRejectionMessage() {
        return rejectionMessage;
    }

    public static AcceptResult accept() {
        return ACCEPTED;
    }

    public static AcceptResult reject(String reason) {
        return new AcceptResult(false, Optional.ofNullable(reason));
    }
}

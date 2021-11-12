package com.sequenceiq.cloudbreak.util;

import static java.lang.String.format;

import java.util.Objects;
import java.util.Optional;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

public class NotAllowedStatusUpdate {

    private String targetStatus;

    private Optional<String> expectedStatus = Optional.empty();

    private String type;

    private final Stack stack;

    public NotAllowedStatusUpdate(Stack stack) {
        this.stack = Objects.requireNonNull(stack);
    }

    public static NotAllowedStatusUpdate builder(Stack stack) {
        return new NotAllowedStatusUpdate(stack);
    }

    public static NotAllowedStatusUpdate stack(Stack stack) {
        return new NotAllowedStatusUpdate(stack).type("stack");
    }

    public static NotAllowedStatusUpdate cluster(Stack stack) {
        return new NotAllowedStatusUpdate(stack).type("cluster");
    }

    public NotAllowedStatusUpdate to(Object targetStatus) {
        this.targetStatus = targetStatus.toString();
        return this;
    }

    public NotAllowedStatusUpdate expectedIn(Object expectedStatus) {
        this.expectedStatus = Optional.of(expectedStatus.toString());
        return this;
    }

    public NotAllowedStatusUpdate type(String type) {
        this.type = type;
        return this;
    }

    public BadRequestException badRequest() {
        String stackStatus = Optional.ofNullable(stack.getStatus())
                .map(Status::name)
                .orElse("N/A");
        String message = new StringBuilder()
                .append("Cannot update the status of the ")
                .append(type)
                .append(" to ")
                .append(targetStatus)
                .append(" when stack is in ")
                .append(stackStatus)
                .append(" state.")
                .append(expectedStatus.map(e -> format(" The %s should be in %s status.", type, e)).orElse(""))
                .toString();
        return new BadRequestException(message);
    }
}

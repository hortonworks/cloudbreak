package com.sequenceiq.cloudbreak.cm.client.tracing;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StackBasedCmApiNameExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackBasedCmApiNameExtractor.class);

    private static final int MAX_SIZE = 15;

    public Optional<String> getCmApiName(StackWalker stackWalker) {
        Optional<StackWalker.StackFrame> foundFrame = stackWalker.walk(stackFrameStream -> stackFrameStream
                .filter(f -> !f.getClassName().startsWith("com.cloudera.api.swagger.client"))
                .filter(e -> e.getClassName().startsWith("com.cloudera.api.swagger"))
                .limit(MAX_SIZE)
                .findFirst());
        return constructApiNameFromFrame(foundFrame);
    }

    private Optional<String> constructApiNameFromFrame(Optional<StackWalker.StackFrame> foundFrame) {
        return foundFrame.map(frame -> {
            String[] split = frame.getClassName().split("\\.");
            String name = split[split.length - 1];
            return name + "." + frame.getMethodName();
        }).or(() -> {
            LOGGER.error("Couldn't extract the CM API name for the tracer.");
            return Optional.empty();
        });
    }
}

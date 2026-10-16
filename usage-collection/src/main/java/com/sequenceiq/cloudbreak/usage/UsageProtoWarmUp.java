package com.sequenceiq.cloudbreak.usage;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import com.sequenceiq.cloudbreak.aspect.Measure;

/**
 * Pays the one-time reflective initialization cost of the generated usage protobuf classes during startup.
 *
 * The first {@code toString()} of a protobuf message (used when usage events are logged) initializes the message type's
 * field accessor table via reflection while holding the table's monitor. {@link UsageProto.Event} has close to 200 fields,
 * so on a cold JVM this takes seconds, and every concurrent first flow that logs a usage event waits on the same monitor
 * before its flow can be accepted. Warming up before the web server starts moves that cost out of request handling.
 */
@Component
public class UsageProtoWarmUp {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsageProtoWarmUp.class);

    @Measure(UsageProtoWarmUp.class)
    public void warmUp() {
        try {
            Set<Descriptor> warmedUpTypes = warmUp(UsageProto.Event.getDefaultInstance(), new HashSet<>());
            LOGGER.info("Warmed up {} usage protobuf message types", warmedUpTypes.size());
        } catch (RuntimeException e) {
            LOGGER.warn("Usage protobuf warm-up failed, first usage events will pay the initialization cost", e);
        }
    }

    Set<Descriptor> warmUp(Message message, Set<Descriptor> visited) {
        if (visited.add(message.getDescriptorForType())) {
            // the result is not needed, printing initializes the field accessor table of the message type
            message.toString();
            Message.Builder builder = message.newBuilderForType();
            for (FieldDescriptor field : message.getDescriptorForType().getFields()) {
                if (field.getJavaType() == FieldDescriptor.JavaType.MESSAGE && !field.isMapField()) {
                    warmUp(builder.newBuilderForField(field).getDefaultInstanceForType(), visited);
                }
            }
        }
        return visited;
    }
}

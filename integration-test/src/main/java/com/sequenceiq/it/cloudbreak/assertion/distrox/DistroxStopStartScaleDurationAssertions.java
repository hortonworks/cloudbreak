package com.sequenceiq.it.cloudbreak.assertion.distrox;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventContainer;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredNotificationEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.legacy.OperationDetails;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;

public class DistroxStopStartScaleDurationAssertions implements Assertion<DistroXTestDto, CloudbreakClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroxStopStartScaleDurationAssertions.class);

    private final long expectedDuration;

    private final boolean scalingUp;

    public DistroxStopStartScaleDurationAssertions(long expectedDuration, boolean scalingUp) {
        this.expectedDuration = expectedDuration;
        this.scalingUp = scalingUp;
    }

    private long getTimestampForEvent(List<StructuredNotificationEvent> structuredNotificationEvents, String message) {
        OperationDetails latestEvent = structuredNotificationEvents.stream()
                .filter(events -> StringUtils.containsIgnoreCase(events.getNotificationDetails().getNotification(), message))
                .map(StructuredEvent::getOperation)
                .max(Comparator.comparing(OperationDetails::getTimestamp))
                .orElseThrow(() -> new TestFailException(String.format("Cannot find Structured Event for '%s' message!", message)));
        long latestEventTimestamp = latestEvent.getTimestamp();
        LOGGER.info(String.format("[%s] event time '%s'.", message,
                LocalDateTime.ofInstant(Instant.ofEpochMilli(latestEventTimestamp), ZoneId.systemDefault())));
        return latestEventTimestamp;
    }

    @Override
    public DistroXTestDto doAssertion(TestContext testContext, DistroXTestDto testDto, CloudbreakClient client) throws Exception {
        String startMessage = scalingUp ? "Scaling up (via instance start) for host group" : "Scaling down (via instance stop) for host group";
        String stopMessage = scalingUp ? "Scaled up (via instance start) host group" : "Scaled down (via instance stop) host group";

        StructuredEventContainer structuredEventContainer = client.getDefaultClient(testContext)
                .eventV4Endpoint()
                .structured(testDto.getName(), testContext.getActingUserCrn().getAccountId());
        List<StructuredNotificationEvent> structuredNotificationEvents = structuredEventContainer.getNotification();

        long startTime = getTimestampForEvent(structuredNotificationEvents, startMessage);
        long endTime = getTimestampForEvent(structuredNotificationEvents, stopMessage);
        long actualDuration = endTime - startTime;
        LOGGER.info(String.format("[%s] event duration '%s' minutes.", startMessage, TimeUnit.MILLISECONDS.toMinutes(actualDuration)));

        String message = String.format("Distrox last scale have been took (%d) more than the expected %d minutes!",
                TimeUnit.MILLISECONDS.toMinutes(actualDuration), expectedDuration);
        if (actualDuration > TimeUnit.MINUTES.toMillis(expectedDuration)) {
            LOGGER.error(message);
            throw new TestFailException(message);
        }
        return testDto;
    }
}
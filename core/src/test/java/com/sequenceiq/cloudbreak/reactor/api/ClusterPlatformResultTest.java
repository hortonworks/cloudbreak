package com.sequenceiq.cloudbreak.reactor.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.SocketTimeoutException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.common.event.Payload;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterSyncRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.ClusterSyncResult;

class ClusterPlatformResultTest {

    private static final Long STACK_ID = 1L;

    private static final String STATUS_REASON = "Cluster sync failed";

    private static final String TIMEOUT_MESSAGE = "Read timed out";

    @Test
    void testGetExceptionReturnsErrorDetailsForFailedResult() {
        SocketTimeoutException exception = new SocketTimeoutException(TIMEOUT_MESSAGE);

        ClusterSyncResult underTest = new ClusterSyncResult(STATUS_REASON, exception, new ClusterSyncRequest(STACK_ID));

        assertThat(underTest.getException()).isSameAs(exception);
        assertThat(underTest.getException()).isSameAs(underTest.getErrorDetails());
    }

    @Test
    void testGetExceptionReturnsNullForSuccessfulResult() {
        ClusterSyncResult underTest = new ClusterSyncResult(new ClusterSyncRequest(STACK_ID));

        assertThat(underTest.getException()).isNull();
    }

    @Test
    void testGetExceptionIsNotSerializedButErrorDetailsIs() throws JsonProcessingException {
        ClusterSyncResult underTest = new ClusterSyncResult(STATUS_REASON, new SocketTimeoutException(TIMEOUT_MESSAGE),
                new ClusterSyncRequest(STACK_ID));

        String json = JsonUtil.writeValueAsStringSilent(underTest);

        JsonNode jsonNode = JsonUtil.readTree(json);
        assertThat(jsonNode.has("exception")).isFalse();
        assertThat(jsonNode.has("errorDetails")).isTrue();
        assertThat(jsonNode.get("errorDetails").get("message").asText()).isEqualTo(TIMEOUT_MESSAGE);
    }

    @Test
    void testFailedResultSurvivesPayloadJsonRoundTripWithException() {
        ClusterSyncResult original = new ClusterSyncResult(STATUS_REASON, new SocketTimeoutException(TIMEOUT_MESSAGE),
                new ClusterSyncRequest(STACK_ID));

        String json = JsonUtil.writeValueAsStringSilent(original);
        Payload deserialized = JsonUtil.readValueUnchecked(json, Payload.class);

        assertThat(deserialized).isInstanceOf(ClusterSyncResult.class);
        ClusterSyncResult result = (ClusterSyncResult) deserialized;
        assertThat(result.getException()).isNotNull();
        assertThat(result.getException().getMessage()).isEqualTo(TIMEOUT_MESSAGE);
        assertThat(result.getErrorDetails()).isSameAs(result.getException());
        assertThat(result.getStatusReason()).isEqualTo(STATUS_REASON);
        assertThat(result.getResourceId()).isEqualTo(STACK_ID);
        assertThat(result.getStatus()).isEqualTo(EventStatus.FAILED);
    }

    @Test
    void testSuccessfulResultSurvivesPayloadJsonRoundTripWithoutException() {
        ClusterSyncResult original = new ClusterSyncResult(new ClusterSyncRequest(STACK_ID));

        String json = JsonUtil.writeValueAsStringSilent(original);
        Payload deserialized = JsonUtil.readValueUnchecked(json, Payload.class);

        assertThat(deserialized).isInstanceOf(ClusterSyncResult.class);
        assertThat(deserialized.getException()).isNull();
        assertThat(deserialized.getResourceId()).isEqualTo(STACK_ID);
    }
}

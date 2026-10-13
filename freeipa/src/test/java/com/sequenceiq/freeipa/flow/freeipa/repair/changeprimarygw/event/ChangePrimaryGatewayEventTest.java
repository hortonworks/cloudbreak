package com.sequenceiq.freeipa.flow.freeipa.repair.changeprimarygw.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.eventbus.Promise;

class ChangePrimaryGatewayEventTest {

    private static final String SELECTOR = "CHANGE_PRIMARY_GATEWAY_EVENT";

    private static final Long STACK_ID = 1L;

    private static final String OPERATION_ID = "operation-id";

    @Test
    void verifyReplicationDefaultsToFalseWithoutExplicitFlag() {
        ChangePrimaryGatewayEvent event = new ChangePrimaryGatewayEvent(SELECTOR, STACK_ID, List.of("i-1"), Boolean.TRUE, OPERATION_ID);

        assertFalse(event.isVerifyReplication());
    }

    @Test
    void deserializingJsonWithoutVerifyReplicationYieldsFalse() throws IOException {
        ObjectNode legacyJson = serializedEvent(true);
        legacyJson.remove("verifyReplication");

        ChangePrimaryGatewayEvent event = JsonUtil.readValue(legacyJson.toString(), ChangePrimaryGatewayEvent.class);

        assertFalse(event.isVerifyReplication());
        assertEquals(STACK_ID, event.getResourceId());
        assertEquals(List.of("i-1"), event.getRepairInstanceIds());
        assertEquals(OPERATION_ID, event.getOperationId());
    }

    @Test
    void deserializingJsonWithNullVerifyReplicationYieldsFalse() throws IOException {
        ObjectNode json = serializedEvent(true);
        json.putNull("verifyReplication");

        ChangePrimaryGatewayEvent event = JsonUtil.readValue(json.toString(), ChangePrimaryGatewayEvent.class);

        assertFalse(event.isVerifyReplication());
    }

    @Test
    void verifyReplicationTrueSurvivesJsonRoundTrip() throws IOException {
        ChangePrimaryGatewayEvent original = new ChangePrimaryGatewayEvent(SELECTOR, STACK_ID, List.of("i-1", "i-2"), Boolean.FALSE, OPERATION_ID)
                .withReplicationVerification();

        String json = JsonUtil.writeValueAsString(original);
        ChangePrimaryGatewayEvent deserialized = JsonUtil.readValue(json, ChangePrimaryGatewayEvent.class);

        assertTrue(json.contains("\"verifyReplication\":true"));
        assertTrue(deserialized.isVerifyReplication());
        assertEquals(original.getRepairInstanceIds(), deserialized.getRepairInstanceIds());
        assertEquals(original.getFinalChain(), deserialized.getFinalChain());
        assertEquals(original.getOperationId(), deserialized.getOperationId());
    }

    @Test
    void verifyReplicationFalseSurvivesJsonRoundTrip() throws IOException {
        ChangePrimaryGatewayEvent original = new ChangePrimaryGatewayEvent(SELECTOR, STACK_ID, List.of("i-1"), Boolean.TRUE, OPERATION_ID);

        ChangePrimaryGatewayEvent deserialized = JsonUtil.readValue(JsonUtil.writeValueAsString(original), ChangePrimaryGatewayEvent.class);

        assertFalse(deserialized.isVerifyReplication());
    }

    @Test
    void withReplicationVerificationKeepsEveryFieldAndThePromise() {
        Promise<AcceptResult> accepted = new Promise<>();
        List<String> instanceIds = List.of("i-1", "i-2");
        ChangePrimaryGatewayEvent original = new ChangePrimaryGatewayEvent(SELECTOR, STACK_ID, instanceIds, Boolean.FALSE, OPERATION_ID, accepted);

        ChangePrimaryGatewayEvent copy = original.withReplicationVerification();

        assertNotSame(original, copy);
        assertTrue(copy.isVerifyReplication());
        assertEquals(SELECTOR, copy.selector());
        assertEquals(STACK_ID, copy.getResourceId());
        assertEquals(instanceIds, copy.getRepairInstanceIds());
        assertEquals(Boolean.FALSE, copy.getFinalChain());
        assertEquals(OPERATION_ID, copy.getOperationId());
        assertSame(accepted, copy.accepted());
    }

    @Test
    void withReplicationVerificationDoesNotMutateTheOriginal() {
        ChangePrimaryGatewayEvent original = new ChangePrimaryGatewayEvent(SELECTOR, STACK_ID, List.of("i-1"), Boolean.TRUE, OPERATION_ID);

        original.withReplicationVerification();

        assertFalse(original.isVerifyReplication());
    }

    @Test
    void equalsEventDistinguishesVerifyReplication() {
        ChangePrimaryGatewayEvent withVerification = new ChangePrimaryGatewayEvent(SELECTOR, STACK_ID, List.of("i-1"), Boolean.TRUE, OPERATION_ID)
                .withReplicationVerification();
        ChangePrimaryGatewayEvent withoutVerification = new ChangePrimaryGatewayEvent(SELECTOR, STACK_ID, List.of("i-1"), Boolean.TRUE, OPERATION_ID);

        assertFalse(withVerification.equalsEvent(withoutVerification));
        assertTrue(withVerification.equalsEvent(new ChangePrimaryGatewayEvent(SELECTOR, STACK_ID, List.of("i-1"), Boolean.TRUE, OPERATION_ID)
                .withReplicationVerification()));
    }

    private ObjectNode serializedEvent(boolean verifyReplication) throws IOException {
        ChangePrimaryGatewayEvent event = new ChangePrimaryGatewayEvent(SELECTOR, STACK_ID, List.of("i-1"), Boolean.TRUE, OPERATION_ID);
        if (verifyReplication) {
            event = event.withReplicationVerification();
        }
        return (ObjectNode) JsonUtil.readTree(JsonUtil.writeValueAsString(event));
    }
}

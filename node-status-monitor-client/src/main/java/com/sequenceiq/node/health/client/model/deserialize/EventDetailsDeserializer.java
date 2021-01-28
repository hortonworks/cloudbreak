package com.sequenceiq.node.health.client.model.deserialize;

import java.io.IOException;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.node.health.client.model.EventDetails;

public class EventDetailsDeserializer extends JsonDeserializer<EventDetails> {
    @Override
    public EventDetails deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        if (!node.isNull() && !node.asText().isEmpty()) {
            node.isEmpty();
        }

        EventDetails eventDetails = new EventDetails();

        Optional<JsonNode> meteredResourceCrn = Optional.ofNullable(node.get("meteredResourceCrn"));
        meteredResourceCrn.map(JsonNode::asText).ifPresent(eventDetails::setMeteredResourceCrn);

        Optional<JsonNode> meteredResourceName = Optional.ofNullable(node.get("meteredResourceName"));
        meteredResourceName.map(JsonNode::asText).ifPresent(eventDetails::setMeteredResourceName);

        Optional<JsonNode> serviceType = Optional.ofNullable(node.get("serviceType"));
        serviceType.map(JsonNode::asText).ifPresent(eventDetails::setServiceType);

        return eventDetails;
    }
}

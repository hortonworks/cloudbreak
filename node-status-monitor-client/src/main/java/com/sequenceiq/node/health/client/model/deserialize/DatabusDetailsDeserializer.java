package com.sequenceiq.node.health.client.model.deserialize;

import java.io.IOException;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.node.health.client.model.DatabusDetails;

public class DatabusDetailsDeserializer extends JsonDeserializer<DatabusDetails> {
    @Override
    public DatabusDetails deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        if (!node.isNull() && !node.asText().isEmpty()) {
            node.isEmpty();
        }

        DatabusDetails databusDetails = new DatabusDetails();

        Optional<JsonNode> endpoint = Optional.ofNullable(node.get("endpoint"));
        endpoint.map(JsonNode::asText).ifPresent(databusDetails::setEndpoint);

        Optional<JsonNode> proxyUrl = Optional.ofNullable(node.get("proxyUrl"));
        proxyUrl.map(JsonNode::asText).ifPresent(databusDetails::setProxyUrl);

        Optional<JsonNode> stream = Optional.ofNullable(node.get("stream"));
        stream.map(JsonNode::asText).ifPresent(databusDetails::setStream);

        return databusDetails;
    }
}

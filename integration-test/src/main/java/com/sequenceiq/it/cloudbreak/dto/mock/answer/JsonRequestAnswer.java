package com.sequenceiq.it.cloudbreak.dto.mock.answer;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.it.cloudbreak.dto.mock.HttpMock;
import com.sequenceiq.it.cloudbreak.dto.mock.Method;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

import spark.Request;

public class JsonRequestAnswer<S> extends AbstractRequestWithBodyHandler<S, JsonNode> {
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonRequestAnswer.class);

    public JsonRequestAnswer(Method method, String path, Class<String> requestType, HttpMock mock) {
        super(method, path, JsonNode.class, mock);
    }

    @Override
    JsonNode prepareRequestInstance(Request request) {
        try {
            return JsonUtil.readTree(request.body());
        } catch (IOException e) {
            LOGGER.error("Could not parse json from request body", e);
            throw new TestFailException("Could not parse json from request body");
        }
    }
}

package com.sequenceiq.mock.legacy.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sequenceiq.mock.legacy.response.MockResponse;

@Service
public class ResponseModifierService {

    private final Map<String, MockResponse> responses = new HashMap<>();

    public void addResponse(MockResponse mockResponse) {
        responses.put(mockResponse.getHttpMethod().toLowerCase() + "_" + mockResponse.getPath(), mockResponse);
    }

    public MockResponse getResponse(String method, String path) {
        return responses.get(method + "_" + path);
    }
}

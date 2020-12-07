package com.sequenceiq.mock.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpServerErrorException;

import com.google.gson.Gson;
import com.sequenceiq.cloudbreak.util.CheckedSupplier;
import com.sequenceiq.mock.spi.MockResponse;

@Service
public class ResponseModifierService {

    private final Map<String, List<MockResponse>> responses = new ConcurrentHashMap<>();

    private final Map<String, Integer> called = new ConcurrentHashMap<>();

    public void addResponse(MockResponse mockResponse) {
        String responseKey = mockResponse.getHttpMethod().toLowerCase() + "_" + mockResponse.getPath();
        List<MockResponse> mockResponses = responses.computeIfAbsent(responseKey, key -> new ArrayList<>());
        mockResponses.add(mockResponse);
    }

    public List<MockResponse> getResponse(String path) {
        return responses.get(path);
    }

    /**
     * Evaluate the response by URI an http method if it appears in the pre-defined response list. The order of the responses matter with the same path and
     * method. (Check the unit tests)
     * Evaluate the times:
     * 0: will be permanently in the list, the next ones with the same path and method will be never evaluate
     * 1...n: evaluate n times. If reach the n-th, the response will be removed from the list of path.
     *
     * Evaluate the response or exception
     * Only if the the status code is 200 return with the response.
     * If the status code is null, throw a NotFound status.
     *
     * @param path the http method and URI separated by `_`. e.g: get_/path
     * @param defaultResponse supplier for the default response if the response cannot be found in the pre-defined list
     * @param <T> Type of the default value
     * @return return with the default values or throw an exception based on the pre-defined status code
     * @throws Throwable if any errors occurred in the supplier.
     */
    public <T> T evaluateResponse(String path, Class<?> returnType, CheckedSupplier<T, Throwable> defaultResponse) throws Throwable {
        List<MockResponse> mockResponses = responses.get(path);
        if (CollectionUtils.isEmpty(mockResponses)) {
            return defaultResponse.get();
        }
        MockResponse mockResponse = mockResponses.get(0);
        handleTimes(path, mockResponse);

        return parseStatusCode(returnType, mockResponse);
    }

    private void handleTimes(String path, MockResponse mockResponse) {
        int times = mockResponse.getTimes();
        if (times > 0) {
            String calledKey = path + "_" + mockResponse.hashCode();
            int call = called.computeIfAbsent(calledKey, key -> 0);
            call = call + 1;
            if (call == times) {
                called.remove(calledKey);
                List<MockResponse> mockResponses = responses.get(path);
                mockResponses.remove(mockResponse);
                if (mockResponses.isEmpty()) {
                    responses.remove(path);
                }
            } else {
                called.put(calledKey, call);
            }
        }
    }

    private <T> T parseStatusCode(Class<?> returnType, MockResponse mockResponse) {
        if (mockResponse.getStatusCode() == 0) {
            throw new HttpServerErrorException(HttpStatus.NOT_FOUND, "Status code is not defined.");
        } else if (mockResponse.getStatusCode() == HttpStatus.OK.value()) {
            Object response = mockResponse.getResponse();
            return (T) new Gson().fromJson(new Gson().toJson(response), returnType);
        } else if (mockResponse.getStatusCode() == HttpStatus.NO_CONTENT.value()) {
            return null;
        }
        throw new HttpServerErrorException(HttpStatus.valueOf(mockResponse.getStatusCode()), mockResponse.getMessage());
    }
}

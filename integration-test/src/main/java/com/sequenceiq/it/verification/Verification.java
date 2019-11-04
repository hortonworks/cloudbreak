package com.sequenceiq.it.verification;

import static org.testng.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.Response;

public class Verification {

    private static final Logger LOGGER = LoggerFactory.getLogger(Verification.class);

    private final String path;

    private final boolean regex;

    private final String httpMethod;

    private Map<Call, Response> requestResponseMap;

    private Integer atLeast;

    private Integer exactTimes;

    private final Collection<Pattern> patternList = new ArrayList<>();

    private final Map<String, Integer> bodyContainsList = new HashMap<>();

    public Verification(String path, String httpMethod, Map<Call, Response> requestResponseMap, boolean regex) {
        this.path = path;
        this.regex = regex;
        this.httpMethod = httpMethod;
        this.requestResponseMap = requestResponseMap;
    }

    public Verification(String path, String httpMethod, boolean regex) {
        this.path = path;
        this.regex = regex;
        this.httpMethod = httpMethod;
    }

    public Verification atLeast(int times) {
        atLeast = times;
        return this;
    }

    public Verification exactTimes(int times) {
        exactTimes = times;
        return this;
    }

    public Verification bodyContains(String text) {
        bodyContainsList.put(text, -1);
        return this;
    }

    public Verification bodyContains(String text, int times) {
        bodyContainsList.put(text, times);
        return this;
    }

    public Verification bodyRegexp(String regexp) {
        Pattern pattern = Pattern.compile(regexp);
        patternList.add(pattern);
        return this;
    }

    public void verify() {
        logVerify();
        int times = getTimesMatched();
        checkAtLeast(times);
        checkExactTimes(times);
    }

    public void verify(Map<Call, Response> requestResponseMap) {
        this.requestResponseMap = requestResponseMap;
        verify();
    }

    private void logVerify() {
        LOGGER.info("Verification call: " + path);
        LOGGER.info("Body must contains: " + StringUtils.join(bodyContainsList, ","));
        List<String> patternStringList = patternList.stream().map(Pattern::pattern).collect(Collectors.toList());
        LOGGER.info("Body must match: " + StringUtils.join(patternStringList, ","));
    }

    private void checkExactTimes(int times) {
        if (exactTimes != null) {
            if (exactTimes != times) {
                logRequests();
                fail(path + " request should have been invoked exactly " + exactTimes + " times, but it was invoked " + times + " times");
            }
        }
    }

    private void checkAtLeast(int times) {
        if (atLeast != null) {
            if (times < atLeast) {
                logRequests();
                fail(path + " request should have been invoked at least " + atLeast + " times, but it was invoked " + times + " times");
            }
        }
    }

    private int getTimesMatched() {
        int times = 0;
        for (Call call : requestResponseMap.keySet()) {
            boolean pathMatched = isPathMatched(call);
            if (call.getMethod().equals(httpMethod) && pathMatched) {
                int bodyContainsNumber = 0;
                for (Entry<String, Integer> stringIntegerEntry : bodyContainsList.entrySet()) {
                    int count = StringUtils.countMatches(call.getPostBody(), stringIntegerEntry.getKey());
                    int required = stringIntegerEntry.getValue();
                    if ((required < 0 && count > 0) || (count == required)) {
                        bodyContainsNumber++;
                    }
                }
                int patternNumber = 0;
                for (Pattern pattern : patternList) {
                    boolean patternMatch = pattern.matcher(call.getPostBody()).matches();
                    if (patternMatch) {
                        patternNumber++;
                    }
                }
                if (bodyContainsList.size() == bodyContainsNumber && patternList.size() == patternNumber) {
                    times++;
                }
            }
        }
        return times;
    }

    private boolean isPathMatched(Call call) {
        boolean pathMatched;
        return regex ? Pattern.matches(path, call.getUri()) : call.getUri().equals(path);
    }

    private void logRequests() {
        LOGGER.info("Request received: ");
        requestResponseMap.keySet().forEach(call -> LOGGER.info("Request: " + call));
    }

}

package com.sequenceiq.it.verification;

import static org.testng.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import spark.Response;

public class Verification {

    private static final Logger LOGGER = LoggerFactory.getLogger(Verification.class);

    private String path;
    private boolean regex;
    private String httpMethod;
    private Map<Call, Response> requestResponseMap;
    private Integer atLeast;
    private Integer exactTimes;
    private List<Pattern> patternList = new ArrayList<>();
    private List<String> bodyContainsList = new ArrayList<>();

    public Verification(String path, String httpMethod, Map<Call, Response> requestResponseMap, boolean regex) {
        this.path = path;
        this.regex = regex;
        this.httpMethod = httpMethod;
        this.requestResponseMap = requestResponseMap;
    }

    public Verification atLeast(int times) {
        this.atLeast = times;
        return this;
    }

    public Verification exactTimes(int times) {
        this.exactTimes = times;
        return this;
    }

    public Verification bodyContains(String text) {
        bodyContainsList.add(text);
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
                fail(path + " request didn't invoked exactly " + exactTimes + " times, invoked " + times + " times");
            }
        }
    }

    private void checkAtLeast(int times) {
        if (atLeast != null) {
            if (times < atLeast) {
                logRequests();
                fail(path + " request didn't invoked at least " + atLeast + " times, invoked " + times + " times");
            }
        }
    }

    private int getTimesMatched() {
        int times = 0;
        for (Call call : requestResponseMap.keySet()) {
            boolean pathMatched = isPathMatched(call);
            if (call.getMethod().equals(httpMethod) && pathMatched) {
                int bodyContainsNumber = 0;
                int patternNumber = 0;
                for (String bodyContains : bodyContainsList) {
                    boolean contains = call.getPostBody().contains(bodyContains);
                    if (contains) {
                        bodyContainsNumber++;
                    }
                }
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
        if (regex) {
            pathMatched = Pattern.matches(path, call.getUri());
        } else {
            pathMatched = call.getUri().equals(path);
        }
        return pathMatched;
    }

    private void logRequests() {
        LOGGER.info("Request received: ");
        requestResponseMap.keySet().stream().forEach(call -> LOGGER.info("Request: " + call.toString()));
    }

}

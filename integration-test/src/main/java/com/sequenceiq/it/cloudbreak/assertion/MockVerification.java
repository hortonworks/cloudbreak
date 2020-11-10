package com.sequenceiq.it.cloudbreak.assertion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;

import com.sequenceiq.it.cloudbreak.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.verification.Call;

import spark.Response;

public class MockVerification<T> implements Assertion<T, MicroserviceClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MockVerification.class);

    private static final int RETRY_WAIT_SECONDS = 30;

    private static final int RETRY_TIMES = 5;

    private final String path;

    private final HttpMethod httpMethod;

    private Integer atLeast;

    private Integer exactTimes;

    private boolean useRegExp;

    private final Collection<Pattern> patternList = new ArrayList<>();

    private final Map<String, Integer> bodyContainsList = new HashMap<>();

    public MockVerification(HttpMethod httpMethod, String path, Integer exactTimes) {
        this.path = path;
        this.httpMethod = httpMethod;
        this.exactTimes = exactTimes;
    }

    public static MockVerification verify(HttpMethod httpMethod, String path) {
        return new MockVerification(httpMethod, path, 1);
    }

    public static MockVerification verifyRegEx(HttpMethod httpMethod, String path) {
        MockVerification verificator = new MockVerification(httpMethod, path, 1);
        verificator.setUseRegExp(true);
        return verificator;
    }

    @Override
    public T doAssertion(TestContext testContext, T testDto, MicroserviceClient cloudbreakClient) {
        //TODO please don't remove this. It will be enabled if the following jira will be resolved: https://jira.cloudera.com/browse/CB-9111
//        logVerify();
//        Call[] calls = testContext.getExecuteQueryToMockInfrastructure().execute("/calls/" + testContext.getTestMethodName(), r -> r.readEntity(Call[].class));
//        int matchesCount = getTimesMatched(Arrays.asList(calls));

//        SimpleRetryWrapper.create(() -> check(matchesCount))
//                .withName("MockVerification check")
//                .withRetryTimes(RETRY_TIMES)
//                .withRetryWaitSeconds(RETRY_WAIT_SECONDS)
//                .run();

        LOGGER.info("Verification is disabled because we cannot decide the test.");
        return testDto;
    }

    private void check(int matchesCount) {
        checkExactTimes(matchesCount);
        checkAtLeast(matchesCount);
    }

    public String getPath() {
        return path;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public Integer getAtLeast() {
        return atLeast;
    }

    public void setAtLeast(Integer atLeast) {
        this.atLeast = atLeast;
    }

    public Integer getExactTimes() {
        return exactTimes;
    }

    public void setExactTimes(Integer exactTimes) {
        this.exactTimes = exactTimes;
    }

    public boolean isUseRegExp() {
        return useRegExp;
    }

    public void setUseRegExp(boolean useRegExp) {
        this.useRegExp = useRegExp;
    }

    public Collection<Pattern> getPatternList() {
        return patternList;
    }

    public Map<String, Integer> getBodyContainsList() {
        return bodyContainsList;
    }

    public MockVerification atLeast(int times) {
        atLeast = times;
        exactTimes = null;
        return this;
    }

    public MockVerification exactTimes(int times) {
        exactTimes = times;
        atLeast = null;
        return this;
    }

    public MockVerification bodyContains(String text) {
        bodyContainsList.put(text, -1);
        return this;
    }

    public MockVerification bodyContains(String text, int times) {
        bodyContainsList.put(text, times);
        return this;
    }

    public MockVerification bodyRegexp(String regexp) {
        Pattern pattern = Pattern.compile(regexp);
        patternList.add(pattern);
        return this;
    }

    protected void checkExactTimes(int times) {
        if (exactTimes != null) {
            if (exactTimes != times) {
                throw new RuntimeException(path + " with body " + generateBodyTimesLog(bodyContainsList) + " "
                        + "request should have been invoked exactly " + exactTimes + " times, but it was invoked " + times + " times");
            }
        }
    }

    protected void checkAtLeast(int times) {
        if (atLeast != null) {
            if (times < atLeast) {
                throw new RuntimeException(path + "with body" + generateBodyTimesLog(bodyContainsList)
                        + " request should have been invoked at least " + atLeast + " times, but it was invoked " + times + " times");
            }
        }
    }

    protected int getTimesMatched(List<Call> calls) {
        int times = calls.stream()
                .filter(this::isPathMatched)
                .filter(call -> call.getMethod().equals(httpMethod.toString()))
                .mapToInt(this::getMatchedTimes).sum();
        return times;
    }

    private int getMatchedTimes(Call call) {
        int bodyContainsNumber = 0;
        bodyContainsNumber = getBodyContainsNumber(call, bodyContainsNumber);
        int patternNumber = 0;
        patternNumber = getPatternNumber(call, patternNumber);
        if (bodyContainsList.size() == bodyContainsNumber && patternList.size() == patternNumber) {
            return 1;
        }
        return 0;
    }

    private int getPatternNumber(Call call, int patternNumber) {
        for (Pattern pattern : patternList) {
            boolean patternMatch = pattern.matcher(call.getPostBody()).matches();
            if (patternMatch) {
                patternNumber++;
            }
        }
        return patternNumber;
    }

    private int getBodyContainsNumber(Call call, int bodyContainsNumber) {
        for (Map.Entry<String, Integer> stringIntegerEntry : bodyContainsList.entrySet()) {
            int count = StringUtils.countMatches(call.getPostBody(), stringIntegerEntry.getKey());
            int required = stringIntegerEntry.getValue();
            if ((required < 0 && count > 0) || (count == required)) {
                bodyContainsNumber++;
            }
        }
        return bodyContainsNumber;
    }

    private boolean isPathMatched(Call call) {
        return useRegExp ? Pattern.matches(path, call.getUri()) : call.getUri().contains(path);
    }

    private String generateBodyTimesLog(Map<String, Integer> bodyContainsList) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : bodyContainsList.entrySet()) {
            sb.append("Body: " + entry.getKey() + " Times: " + entry.getValue());
        }
        return sb.toString();
    }

    private void logRequests(Map<Call, Response> requestResponseMap) {
        LOGGER.info("Request received: ");
        requestResponseMap.keySet().forEach(call -> LOGGER.info("Request: " + call));
    }

    private void logVerify() {
        LOGGER.info("Verification call: " + getPath());
        LOGGER.info("Body must contains: " + StringUtils.join(getBodyContainsList(), ","));
        List<String> patternStringList = getPatternList().stream().map(Pattern::pattern).collect(Collectors.toList());
        LOGGER.info("Body must match: " + StringUtils.join(patternStringList, ","));
    }
}

package com.sequenceiq.it.verification;

import static org.testng.Assert.fail;

import java.util.Map;

import spark.Response;

public class Verify {

    private String path;
    private String httpMethod;
    private Map<Call, Response> requestResponseMap;
    private Integer atLeast = 1;
    private Integer exactTimes;

    public Verify(String path, String httpMethod, Map<Call, Response> requestResponseMap) {
        this.path = path;
        this.httpMethod = httpMethod;
        this.requestResponseMap = requestResponseMap;
    }

    public Verify atLeast(int times) {
        this.atLeast = times;
        return this;
    }

    public Verify exactTimes(int times) {
        this.exactTimes = times;
        return this;
    }

    public void verify() {
        int times = 0;
        for (Call call : requestResponseMap.keySet()) {
            if (call.getMethod().equals(httpMethod) && call.getUri().equals(path)) {
                times++;
            }
        }
        if (times < atLeast) {
            fail("request didn't invoked at least " + atLeast + " times, invoked only: " + times);
        }
        if (exactTimes != null) {
            if (exactTimes != times) {
                fail("request didn't invoked exactly " + exactTimes + " times, invoked only: " + times);
            }
        }
    }

}

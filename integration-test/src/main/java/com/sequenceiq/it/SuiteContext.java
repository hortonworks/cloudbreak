package com.sequenceiq.it;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SuiteContext {
    private Map<String, IntegrationTestContext> suiteContextMap = Collections.synchronizedMap(new HashMap<String, IntegrationTestContext>());

    public IntegrationTestContext getItContext(String suite) {
        return suiteContextMap.get(suite);
    }

    public IntegrationTestContext putItContext(String suite, IntegrationTestContext itContext) {
        return suiteContextMap.put(suite, itContext);
    }
}

package com.sequenceiq.it

import java.util.Collections
import java.util.HashMap

class SuiteContext {
    private val suiteContextMap = Collections.synchronizedMap(HashMap<String, IntegrationTestContext>())

    fun getItContext(suite: String): IntegrationTestContext {
        return suiteContextMap[suite]
    }

    fun putItContext(suite: String, itContext: IntegrationTestContext): IntegrationTestContext {
        return suiteContextMap.put(suite, itContext)
    }
}

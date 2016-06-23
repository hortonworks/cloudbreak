package com.sequenceiq.cloudbreak.shell.completion

import java.util.ArrayList
import java.util.HashMap

class SecurityRules(rulesString: String) : AbstractCompletion(rulesString) {
    private val rules = ArrayList<Map<String, String>>()

    init {
        parseRules(rulesString)
    }

    fun getRules(): List<Map<String, String>> {
        return rules
    }

    private fun parseRules(rulesString: String) {
        val ruleStringArray = rulesString.split(";".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
        for (ruleString in ruleStringArray) {
            val ruleParams = ruleString.split(":".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()
            val ruleMap = HashMap<String, String>()
            ruleMap.put("subnet", ruleParams[0])
            ruleMap.put("protocol", ruleParams[1])
            ruleMap.put("ports", ruleParams[2])
            rules.add(ruleMap)
        }
    }

}

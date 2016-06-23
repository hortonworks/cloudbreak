package com.sequenceiq.it.verification

import org.testng.Assert.fail

import java.util.ArrayList
import java.util.regex.Pattern
import java.util.stream.Collectors

import org.apache.commons.lang3.StringUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import spark.Response

class Verification(private val path: String, private val httpMethod: String, private val requestResponseMap: Map<Call, Response>, private val regex: Boolean) {
    private var atLeast: Int? = null
    private var exactTimes: Int? = null
    private val patternList = ArrayList<Pattern>()
    private val bodyContainsList = ArrayList<String>()

    fun atLeast(times: Int): Verification {
        this.atLeast = times
        return this
    }

    fun exactTimes(times: Int): Verification {
        this.exactTimes = times
        return this
    }

    fun bodyContains(text: String): Verification {
        bodyContainsList.add(text)
        return this
    }

    fun bodyRegexp(regexp: String): Verification {
        val pattern = Pattern.compile(regexp)
        patternList.add(pattern)
        return this
    }

    fun verify() {
        logVerify()
        val times = timesMatched
        checkAtLeast(times)
        checkExactTimes(times)
    }

    private fun logVerify() {
        LOGGER.info("Verification call: " + path)
        LOGGER.info("Body must contains: " + StringUtils.join(bodyContainsList, ","))
        val patternStringList = patternList.stream().map(Function<Pattern, String> { it.pattern() }).collect(Collectors.toList<String>())
        LOGGER.info("Body must match: " + StringUtils.join(patternStringList, ","))
    }

    private fun checkExactTimes(times: Int) {
        if (exactTimes != null) {
            if (exactTimes !== times) {
                logRequests()
                fail("$path request didn't invoked exactly $exactTimes times, invoked $times times")
            }
        }
    }

    private fun checkAtLeast(times: Int) {
        if (atLeast != null) {
            if (times < atLeast) {
                logRequests()
                fail("$path request didn't invoked at least $atLeast times, invoked $times times")
            }
        }
    }

    private val timesMatched: Int
        get() {
            var times = 0
            for (call in requestResponseMap.keys) {
                val pathMatched = isPathMatched(call)
                if (call.method == httpMethod && pathMatched) {
                    var bodyContainsNumber = 0
                    var patternNumber = 0
                    for (bodyContains in bodyContainsList) {
                        val contains = call.postBody.contains(bodyContains)
                        if (contains) {
                            bodyContainsNumber++
                        }
                    }
                    for (pattern in patternList) {
                        val patternMatch = pattern.matcher(call.postBody).matches()
                        if (patternMatch) {
                            patternNumber++
                        }
                    }
                    if (bodyContainsList.size == bodyContainsNumber && patternList.size == patternNumber) {
                        times++
                    }
                }
            }
            return times
        }

    private fun isPathMatched(call: Call): Boolean {
        val pathMatched: Boolean
        if (regex) {
            pathMatched = Pattern.matches(path, call.uri)
        } else {
            pathMatched = call.uri == path
        }
        return pathMatched
    }

    private fun logRequests() {
        LOGGER.info("Request received: ")
        requestResponseMap.keys.stream().forEach({ call -> LOGGER.info("Request: " + call.toString()) })
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(Verification::class.java)
    }

}

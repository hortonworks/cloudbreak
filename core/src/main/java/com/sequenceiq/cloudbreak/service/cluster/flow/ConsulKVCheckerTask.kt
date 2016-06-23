package com.sequenceiq.cloudbreak.service.cluster.flow

import java.util.Arrays
import java.util.HashSet

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.ecwid.consul.v1.ConsulClient
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask
import com.sequenceiq.cloudbreak.service.cluster.PluginFailureException
import com.sequenceiq.cloudbreak.service.stack.flow.ConsulUtils

@Component
class ConsulKVCheckerTask : StackBasedStatusCheckerTask<ConsulKVCheckerContext>() {

    override fun checkStatus(context: ConsulKVCheckerContext): Boolean {
        val keys = context.keys
        val expectedValue = context.expectedValue
        val failValue = context.failValue
        val client = context.consulClient
        LOGGER.info("Checking if keys in Consul's key-value store have the expected value '{}'", expectedValue)
        val failedKeys = HashSet<String>()
        var matchingKeys = 0
        var notFoundKeys = 0
        for (key in keys) {
            val value = ConsulUtils.getKVValue(Arrays.asList(client), key, null)
            if (value != null) {
                if (value == failValue) {
                    failedKeys.add(key)
                } else if (value == expectedValue) {
                    matchingKeys++
                }
            } else {
                notFoundKeys++
            }
        }
        LOGGER.info("Keys: [Total: {}, {}: {}, Not {}: {}, Not found: {}, {}: {}]",
                keys.size,
                expectedValue, matchingKeys,
                expectedValue, keys.size - matchingKeys - notFoundKeys - failedKeys.size,
                notFoundKeys,
                failValue, failedKeys.size)
        if (!failedKeys.isEmpty()) {
            throw PluginFailureException(String.format("Found failure signal at keys: %s", failedKeys))
        }
        return matchingKeys == keys.size
    }

    override fun handleTimeout(ctx: ConsulKVCheckerContext) {
        throw PluginFailureException(String.format("Operation timed out. Keys not found or don't have the expected value '%s'.", ctx.expectedValue))
    }

    override fun successMessage(ctx: ConsulKVCheckerContext): String {
        return String.format("All %s keys found and have the expected value '%s'.", ctx.keys.size, ctx.expectedValue)
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(ConsulKVCheckerTask::class.java)
    }

}


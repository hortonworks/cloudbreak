package com.sequenceiq.cloudbreak.service.stack.flow

import java.util.Arrays

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.ecwid.consul.v1.ConsulClient
import com.ecwid.consul.v1.catalog.model.CatalogService
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask

@Component
class ConsulServiceCheckerTask : StackBasedStatusCheckerTask<ConsulContext>() {

    override fun checkStatus(consulContext: ConsulContext): Boolean {
        val serviceName = consulContext.targets[0]
        val client = consulContext.consulClient
        LOGGER.info("Checking consul service registration of '{}'", serviceName)
        val service = ConsulUtils.getService(Arrays.asList(client), serviceName)
        if (service.isEmpty()) {
            LOGGER.info("Consul service '{}' is not registered yet", serviceName)
            return false
        } else {
            LOGGER.info("Consul service '{}' found on '{}'", serviceName, service[0].node)
            return true
        }
    }

    override fun handleTimeout(t: ConsulContext) {
        throw CloudbreakServiceException(String.format("Operation timed out. Consul service is not registered %s", t.targets))
    }

    override fun successMessage(t: ConsulContext): String {
        return String.format("Consul service successfully registered '%s'", t.targets)
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(ConsulServiceCheckerTask::class.java)
    }

}
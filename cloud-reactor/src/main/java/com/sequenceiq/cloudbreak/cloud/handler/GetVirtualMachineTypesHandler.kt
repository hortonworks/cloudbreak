package com.sequenceiq.cloudbreak.cloud.handler

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.google.common.collect.Maps
import com.sequenceiq.cloudbreak.cloud.event.platform.GetVirtualMachineTypesRequest
import com.sequenceiq.cloudbreak.cloud.event.platform.GetVirtualMachineTypesResult
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors
import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.model.PlatformVirtualMachines
import com.sequenceiq.cloudbreak.cloud.model.Variant
import com.sequenceiq.cloudbreak.cloud.model.VmType

import reactor.bus.Event

@Component
class GetVirtualMachineTypesHandler : CloudPlatformEventHandler<GetVirtualMachineTypesRequest> {

    @Inject
    private val cloudPlatformConnectors: CloudPlatformConnectors? = null

    override fun type(): Class<GetVirtualMachineTypesRequest> {
        return GetVirtualMachineTypesRequest::class.java
    }

    override fun accept(getVirtualMachineTypesRequestEvent: Event<GetVirtualMachineTypesRequest>) {
        LOGGER.info("Received event: {}", getVirtualMachineTypesRequestEvent)
        val request = getVirtualMachineTypesRequestEvent.data
        try {
            val platformVms = Maps.newHashMap<Platform, Collection<VmType>>()
            val platformDefaultVm = Maps.newHashMap<Platform, VmType>()
            for (connector in cloudPlatformConnectors!!.platformVariants.platformToVariants.entries) {
                val defaultVm = cloudPlatformConnectors.getDefault(connector.key).parameters().vmTypes().defaultType()
                val vmTypes = cloudPlatformConnectors.getDefault(connector.key).parameters().vmTypes().types()

                platformDefaultVm.put(connector.key, defaultVm)
                platformVms.put(connector.key, vmTypes)
            }
            val pv = PlatformVirtualMachines(platformVms, platformDefaultVm)
            val getVirtualMachineTypesResult = GetVirtualMachineTypesResult(request, pv)
            request.result.onNext(getVirtualMachineTypesResult)
            LOGGER.info("Query platform machine types types finished.")
        } catch (e: Exception) {
            request.result.onNext(GetVirtualMachineTypesResult(e.message, e, request))
        }

    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(GetVirtualMachineTypesHandler::class.java)
    }
}

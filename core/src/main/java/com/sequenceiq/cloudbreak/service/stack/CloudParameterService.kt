package com.sequenceiq.cloudbreak.service.stack

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus
import com.sequenceiq.cloudbreak.cloud.event.platform.GetDiskTypesRequest
import com.sequenceiq.cloudbreak.cloud.event.platform.GetDiskTypesResult
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformOrchestratorsRequest
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformOrchestratorsResult
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformRegionsRequest
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformRegionsResult
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformVariantsRequest
import com.sequenceiq.cloudbreak.cloud.event.platform.GetPlatformVariantsResult
import com.sequenceiq.cloudbreak.cloud.event.platform.GetVirtualMachineTypesRequest
import com.sequenceiq.cloudbreak.cloud.event.platform.GetVirtualMachineTypesResult
import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrators
import com.sequenceiq.cloudbreak.cloud.model.PlatformRegions
import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants
import com.sequenceiq.cloudbreak.cloud.model.PlatformVirtualMachines
import com.sequenceiq.cloudbreak.service.stack.connector.OperationException

import reactor.bus.Event
import reactor.bus.EventBus

@Service
class CloudParameterService {

    @Inject
    private val eventBus: EventBus? = null

    val platformVariants: PlatformVariants
        get() {
            LOGGER.debug("Get platform variants")
            val getPlatformVariantsRequest = GetPlatformVariantsRequest()
            eventBus!!.notify(getPlatformVariantsRequest.selector(), Event.wrap(getPlatformVariantsRequest))
            try {
                val res = getPlatformVariantsRequest.await()
                LOGGER.info("Platform variants result: {}", res)
                if (res.status == EventStatus.FAILED) {
                    LOGGER.error("Failed to get platform variants", res.errorDetails)
                    throw OperationException(res.errorDetails)
                }
                return res.platformVariants
            } catch (e: InterruptedException) {
                LOGGER.error("Error while getting the platform variants", e)
                throw OperationException(e)
            }

        }

    val diskTypes: PlatformDisks
        get() {
            LOGGER.debug("Get platform disktypes")
            val getDiskTypesRequest = GetDiskTypesRequest()
            eventBus!!.notify(getDiskTypesRequest.selector(), Event.wrap(getDiskTypesRequest))
            try {
                val res = getDiskTypesRequest.await()
                LOGGER.info("Platform disk types result: {}", res)
                if (res.status == EventStatus.FAILED) {
                    LOGGER.error("Failed to get platform disk types", res.errorDetails)
                    throw OperationException(res.errorDetails)
                }
                return res.platformDisks
            } catch (e: InterruptedException) {
                LOGGER.error("Error while getting the platform disk types", e)
                throw OperationException(e)
            }

        }

    val vmtypes: PlatformVirtualMachines
        get() {
            LOGGER.debug("Get platform vm types")
            val getVirtualMachineTypesRequest = GetVirtualMachineTypesRequest()
            eventBus!!.notify(getVirtualMachineTypesRequest.selector(), Event.wrap(getVirtualMachineTypesRequest))
            try {
                val res = getVirtualMachineTypesRequest.await()
                LOGGER.info("Platform vm types result: {}", res)
                if (res.status == EventStatus.FAILED) {
                    LOGGER.error("Failed to get platform vm types", res.errorDetails)
                    throw OperationException(res.errorDetails)
                }
                return res.platformVirtualMachines
            } catch (e: InterruptedException) {
                LOGGER.error("Error while getting the platform vm types", e)
                throw OperationException(e)
            }

        }

    val regions: PlatformRegions
        get() {
            LOGGER.debug("Get platform regions")
            val getPlatformRegionsRequest = GetPlatformRegionsRequest()
            eventBus!!.notify(getPlatformRegionsRequest.selector(), Event.wrap(getPlatformRegionsRequest))
            try {
                val res = getPlatformRegionsRequest.await()
                LOGGER.info("Platform regions result: {}", res)
                if (res.status == EventStatus.FAILED) {
                    LOGGER.error("Failed to get platform regions", res.errorDetails)
                    throw OperationException(res.errorDetails)
                }
                return res.platformRegions
            } catch (e: InterruptedException) {
                LOGGER.error("Error while getting the platform regions", e)
                throw OperationException(e)
            }

        }

    val orchestrators: PlatformOrchestrators
        get() {
            LOGGER.debug("Get platform orchestrators")
            val getPlatformOrchestratorsRequest = GetPlatformOrchestratorsRequest()
            eventBus!!.notify(getPlatformOrchestratorsRequest.selector(), Event.wrap(getPlatformOrchestratorsRequest))
            try {
                val res = getPlatformOrchestratorsRequest.await()
                LOGGER.info("Platform orchestrators result: {}", res)
                if (res.status == EventStatus.FAILED) {
                    LOGGER.error("Failed to get platform orchestrators", res.errorDetails)
                    throw OperationException(res.errorDetails)
                }
                return res.platformOrchestrators
            } catch (e: InterruptedException) {
                LOGGER.error("Error while getting the platform orchestrators", e)
                throw OperationException(e)
            }

        }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(CloudParameterService::class.java)
    }
}

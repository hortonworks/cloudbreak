package com.sequenceiq.cloudbreak.core.flow2.stack.provision.action

import java.util.concurrent.TimeUnit

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.event.Selectable
import com.sequenceiq.cloudbreak.cloud.event.setup.CheckImageResult
import com.sequenceiq.cloudbreak.core.flow2.PayloadConverter
import com.sequenceiq.cloudbreak.core.flow2.stack.StackContext
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.PrepareImageResultToStackEventConverter
import com.sequenceiq.cloudbreak.core.flow2.stack.provision.StackCreationEvent
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException

import reactor.fn.Consumer
import reactor.fn.timer.Timer

@Component("CheckImageAction")
class CheckImageAction : AbstractStackCreationAction<StackEvent>(StackEvent::class.java) {

    @Inject
    private val stackCreationService: StackCreationService? = null
    @Inject
    private val timer: Timer? = null

    override fun doExecute(context: StackContext, payload: StackEvent, variables: MutableMap<Any, Any>) {
        val checkImageResult = stackCreationService!!.checkImage(context)
        when (checkImageResult.imageStatus) {
            ImageStatus.IN_PROGRESS -> repeat(context)
            ImageStatus.CREATE_FINISHED -> sendEvent(context)
            ImageStatus.CREATE_FAILED -> {
                LOGGER.error("Error during image status check: {}", payload)
                val faultNum = getFaultNum(variables) + 1
                if (faultNum == FAULT_TOLERANCE) {
                    removeFaultNum(variables)
                    throw CloudbreakServiceException("Image copy failed.")
                } else {
                    setFaultNum(variables, faultNum)
                    repeat(context)
                }
            }
            else -> // TODO error handling
                LOGGER.error("Unknown imagestatus: {}", checkImageResult.imageStatus)
        }
    }

    override fun createRequest(context: StackContext): Selectable {
        return StackEvent(StackCreationEvent.IMAGE_COPY_FINISHED_EVENT.stringRepresentation(), context.stack.id)
    }

    override fun initPayloadConverterMap(payloadConverters: MutableList<PayloadConverter<StackEvent>>) {
        payloadConverters.add(PrepareImageResultToStackEventConverter())
    }

    private fun repeat(context: StackContext) {
        timer!!.submit({ sendEvent(context.flowId, StackEvent(StackCreationEvent.IMAGE_COPY_CHECK_EVENT.stringRepresentation(), context.stack.id)) }, REPEAT_TIME.toLong(), TimeUnit.MILLISECONDS)
    }

    private fun getFaultNum(variables: Map<Any, Any>): Int {
        val faultNum = variables[IMAGE_COPY_FAULT_NUM] as Int
        return faultNum ?: 0
    }

    private fun setFaultNum(variables: MutableMap<Any, Any>, faultNum: Int) {
        variables.put(IMAGE_COPY_FAULT_NUM, faultNum)
    }

    private fun removeFaultNum(variables: MutableMap<Any, Any>) {
        variables.remove(IMAGE_COPY_FAULT_NUM)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(CheckImageAction::class.java)
        private val REPEAT_TIME = 5000
        private val FAULT_TOLERANCE = 5
        private val IMAGE_COPY_FAULT_NUM = "IMAGE_COPY_FAULT_NUM"
    }
}

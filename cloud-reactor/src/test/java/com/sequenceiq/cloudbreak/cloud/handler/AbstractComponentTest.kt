package com.sequenceiq.cloudbreak.cloud.handler

import javax.inject.Inject

import org.junit.runner.RunWith
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest
import com.sequenceiq.cloudbreak.cloud.handler.testcontext.TestApplicationContext

import reactor.bus.Event
import reactor.bus.EventBus

@RunWith(SpringJUnit4ClassRunner::class)
@SpringApplicationConfiguration(classes = TestApplicationContext::class)
abstract class AbstractComponentTest<T> {
    @Inject
    private val eb: EventBus? = null

    @Inject
    private val g: ParameterGenerator? = null

    protected fun sendCloudRequest(): T {
        val request = request
        return sendCloudRequest(request)
    }

    protected fun sendCloudRequest(request: CloudPlatformRequest<Any>): T {
        eb!!.notify(topicName, Event.wrap(request))
        try {
            return request.await() as T
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }

    }

    protected abstract val topicName: String

    protected abstract val request: CloudPlatformRequest<Any>

    protected fun g(): ParameterGenerator {
        return g
    }

}

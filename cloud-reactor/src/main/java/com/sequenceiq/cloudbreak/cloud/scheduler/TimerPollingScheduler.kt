package com.sequenceiq.cloudbreak.cloud.scheduler

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

import reactor.bus.EventBus
import reactor.fn.Consumer
import reactor.fn.Pausable
import reactor.fn.timer.Timer

@Component
@Scope(value = "prototype")
class TimerPollingScheduler : Consumer<Long> {

    @Inject
    private val timer: Timer? = null

    @Inject
    private val eventBus: EventBus? = null

    private var latch: CountDownLatch? = null

    private var selfSchedule: Pausable? = null

    override fun accept(aLong: Long?) {
        latch!!.countDown()
        LOGGER.info("Schedule received: {}, count: {}", this, latch!!.count)
        if (latch!!.count == 0) {
            cancel()
        }


    }

    fun schedule(count: Int, period: Int) {
        this.latch = CountDownLatch(count)
        selfSchedule = timer!!.schedule(this, period.toLong(), TimeUnit.SECONDS)
    }

    fun cancel() {
        selfSchedule!!.cancel()
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(TimerPollingScheduler::class.java)
    }
}

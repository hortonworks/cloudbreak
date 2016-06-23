package com.sequenceiq.cloudbreak.cloud.event

import java.util.concurrent.TimeUnit

import com.sequenceiq.cloudbreak.cloud.context.CloudContext
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential

import reactor.rx.Promise
import reactor.rx.Promises

open class CloudPlatformRequest<T>(val cloudContext: CloudContext, val cloudCredential: CloudCredential) : Selectable {
    val result: Promise<T>

    init {
        this.result = Promises.prepare<T>()
    }

    override fun selector(): String {
        return selector(javaClass)
    }

    override val stackId: Long?
        get() = cloudContext.id

    @Throws(InterruptedException::class)
    @JvmOverloads fun await(timeout: Long = 1, unit: TimeUnit = TimeUnit.HOURS): T {
        val result = this.result.await(timeout, unit) ?: throw InterruptedException("Operation timed out, couldn't retrieve result")
        return result
    }

    override fun toString(): String {
        return "CloudPlatformRequest{"
        +"cloudContext=" + cloudContext
        +", cloudCredential=" + cloudCredential
        +'}'
    }

    companion object {

        fun selector(clazz: Class<Any>): String {
            return clazz.simpleName.toUpperCase()
        }
    }
}

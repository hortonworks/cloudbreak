package com.sequenceiq.cloudbreak.logger

import java.lang.reflect.Field

import com.sequenceiq.cloudbreak.domain.CbUser
import org.slf4j.MDC

object MDCBuilder {

    @JvmOverloads fun buildMdcContext(`object`: Any? = null) {
        if (`object` == null) {
            MDC.put(LoggerContextKey.OWNER_ID.toString(), "cloudbreak")
            MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), "cloudbreakLog")
            MDC.put(LoggerContextKey.RESOURCE_ID.toString(), "undefined")
            MDC.put(LoggerContextKey.RESOURCE_NAME.toString(), "cb")
        } else {
            MDC.put(LoggerContextKey.OWNER_ID.toString(), getFieldValue(`object`, "owner"))
            MDC.put(LoggerContextKey.RESOURCE_ID.toString(), getFieldValue(`object`, "id"))
            MDC.put(LoggerContextKey.RESOURCE_NAME.toString(), getFieldValue(`object`, "name"))
            MDC.put(LoggerContextKey.RESOURCE_TYPE.toString(), `object`.javaClass.getSimpleName().toUpperCase())
        }
    }

    fun buildMdcContext(stackId: String, stackName: String, ownerId: String) {
        MDC.put(LoggerContextKey.OWNER_ID.toString(), ownerId)
        MDC.put(LoggerContextKey.RESOURCE_ID.toString(), stackId)
        MDC.put(LoggerContextKey.RESOURCE_NAME.toString(), stackName)
    }

    fun buildUserMdcContext(user: CbUser?) {
        if (user != null) {
            MDC.put(LoggerContextKey.OWNER_ID.toString(), user.userId)
        }
    }

    private fun getFieldValue(o: Any, field: String): String {
        try {
            val privateStringField = o.javaClass.getDeclaredField(field)
            privateStringField.setAccessible(true)
            return privateStringField.get(o).toString()
        } catch (e: Exception) {
            return "undefined"
        }

    }
}

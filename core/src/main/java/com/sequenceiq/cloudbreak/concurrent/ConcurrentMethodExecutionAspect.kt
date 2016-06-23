package com.sequenceiq.cloudbreak.concurrent

import java.lang.reflect.Method
import java.util.concurrent.locks.Lock

import org.aspectj.lang.JoinPoint
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.google.common.util.concurrent.Striped
import com.sequenceiq.cloudbreak.cloud.event.Payload
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException

@Component
@Aspect
class ConcurrentMethodExecutionAspect {

    private val locks = Striped.lazyWeakLock(STRIPES)

    @Pointcut("execution(@com.sequenceiq.cloudbreak.concurrent.GuardedMethod * *(..))")
    fun guardedMethod() {
    }

    @Pointcut("execution(@com.sequenceiq.cloudbreak.concurrent.LockedMethod * *(..))")
    fun lockedMethod() {
    }

    @Pointcut("args(com.sequenceiq.cloudbreak.cloud.event.Payload)")
    fun methodWithPayloadArgument() {
    }

    @Pointcut("guardedMethod() && methodWithPayloadArgument()")
    fun guardedMethodWithPayloadArg() {
    }

    @Pointcut("lockedMethod() && methodWithPayloadArgument()")
    fun lockedMethodWithPayloadArg() {
    }

    @Around("com.sequenceiq.cloudbreak.concurrent.ConcurrentMethodExecutionAspect.lockedMethodWithPayloadArg()")
    @Throws(Throwable::class)
    fun executeLockedMethod(joinPoint: ProceedingJoinPoint): Any {
        val stackId = getStackId(joinPoint)
        val lockPrefix = getLockedMethodLockPrefix(joinPoint)
        val lockKey = createLockKey(lockPrefix, stackId)
        val lock = locks.get(lockKey)
        if (!lock.tryLock()) {
            logWaitingOperation(lockPrefix, stackId)
            lock.lock()
            logContinueOperation(lockPrefix, stackId)
        }
        try {
            return joinPoint.proceed()
        } finally {
            lock.unlock()
        }
    }

    @Around("com.sequenceiq.cloudbreak.concurrent.ConcurrentMethodExecutionAspect.guardedMethodWithPayloadArg()")
    @Throws(Throwable::class)
    fun executeGuardedMethod(joinPoint: ProceedingJoinPoint): Any {
        val stackId = getStackId(joinPoint)
        val lockPrefix = getGuardedMethodLockPrefix(joinPoint)
        val lockKey = createLockKey(lockPrefix, stackId)
        val lock = locks.get(lockKey)
        if (lock.tryLock()) {
            try {
                return joinPoint.proceed()
            } finally {
                lock.unlock()
            }
        } else {
            return skipMethodExecution(lockPrefix, stackId)
        }
    }

    private fun createLockKey(lockPrefix: String, stackId: Long?): String {
        return if (stackId == null) lockPrefix else lockPrefix + stackId.toString()
    }

    private fun getGuardedMethodLockPrefix(joinPoint: JoinPoint): String {
        try {
            return getAnnotation<GuardedMethod>(GuardedMethod::class.java, joinPoint).lockPrefix()
        } catch (ex: Exception) {
            return ""
        }

    }

    private fun getLockedMethodLockPrefix(joinPoint: JoinPoint): String {
        try {
            return getAnnotation<LockedMethod>(LockedMethod::class.java, joinPoint).lockPrefix()
        } catch (ex: Exception) {
            return ""
        }

    }

    @Throws(NoSuchMethodException::class)
    private fun <T : Annotation> getAnnotation(clazz: Class<T>, joinPoint: JoinPoint): T {
        val methodSignature = joinPoint.signature as MethodSignature
        val method = joinPoint.target.javaClass.getDeclaredMethod(joinPoint.signature.name,
                *methodSignature.method.parameterTypes)
        return method.getAnnotation(clazz)
    }

    private fun getStackId(joinPoint: JoinPoint): Long? {
        val payload = getPayload(joinPoint)
        return payload?.stackId
    }

    private fun getPayload(joinPoint: JoinPoint): Payload? {
        var payload: Payload? = null
        for (arg in joinPoint.args) {
            if (arg is Payload) {
                payload = arg
            }
        }
        return payload
    }

    private fun skipMethodExecution(lockPrefix: String, stackId: Long?): Any {
        val message: String
        if (stackId != null) {
            message = String.format("%s operation will be skipped on stack %d, because it is running on a different thread.", lockPrefix, stackId)
        } else {
            message = String.format("%s operation will be skipped, because it is running on a different thread.", lockPrefix)
        }
        LOGGER.info(message)
        throw CancellationException(message)
    }

    private fun logWaitingOperation(lockPrefix: String, stackId: Long?) {
        if (stackId != null) {
            LOGGER.info("Waiting for other {} operation on stack {} to be finished.", lockPrefix, stackId)
        } else {
            LOGGER.info("Waiting for other {} operation to be finished.", lockPrefix)
        }
    }

    private fun logContinueOperation(lockPrefix: String, stackId: Long?) {
        if (stackId != null) {
            LOGGER.info("Continue {} operation on stack {}.", lockPrefix, stackId)
        } else {
            LOGGER.info("Continue {} operation.", lockPrefix)
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ConcurrentMethodExecutionAspect::class.java)
        private val STRIPES = 10
    }
}

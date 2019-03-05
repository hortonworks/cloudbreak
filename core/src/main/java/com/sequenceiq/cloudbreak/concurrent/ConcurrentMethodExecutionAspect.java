package com.sequenceiq.cloudbreak.concurrent;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.concurrent.locks.Lock;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.util.concurrent.Striped;
import com.sequenceiq.cloudbreak.cloud.event.Payload;
import com.sequenceiq.cloudbreak.cloud.scheduler.CancellationException;

@Component
@Aspect
public class ConcurrentMethodExecutionAspect {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConcurrentMethodExecutionAspect.class);

    private static final int STRIPES = 10;

    private final Striped<Lock> locks = Striped.lazyWeakLock(STRIPES);

    @Pointcut("execution(@com.sequenceiq.cloudbreak.concurrent.GuardedMethod * *(..))")
    public void guardedMethod() {
    }

    @Pointcut("execution(@com.sequenceiq.cloudbreak.concurrent.LockedMethod * *(..))")
    public void lockedMethod() {
    }

    @Pointcut("args(com.sequenceiq.cloudbreak.cloud.event.Payload)")
    public void methodWithPayloadArgument() {
    }

    @Pointcut("guardedMethod() && methodWithPayloadArgument()")
    public void guardedMethodWithPayloadArg() {
    }

    @Pointcut("lockedMethod() && methodWithPayloadArgument()")
    public void lockedMethodWithPayloadArg() {
    }

    @Around("com.sequenceiq.cloudbreak.concurrent.ConcurrentMethodExecutionAspect.lockedMethodWithPayloadArg()")
    public Object executeLockedMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        Long stackId = getStackId(joinPoint);
        String lockPrefix = getLockedMethodLockPrefix(joinPoint);
        String lockKey = createLockKey(lockPrefix, stackId);
        Lock lock = locks.get(lockKey);
        if (!lock.tryLock()) {
            logWaitingOperation(lockPrefix, stackId);
            lock.lock();
            try {
                logContinueOperation(lockPrefix, stackId);
            } finally {
                lock.unlock();
            }
        }
        return joinPoint.proceed();
    }

    @Around("com.sequenceiq.cloudbreak.concurrent.ConcurrentMethodExecutionAspect.guardedMethodWithPayloadArg()")
    public Object executeGuardedMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        Long stackId = getStackId(joinPoint);
        String lockPrefix = getGuardedMethodLockPrefix(joinPoint);
        String lockKey = createLockKey(lockPrefix, stackId);
        Lock lock = locks.get(lockKey);
        if (lock.tryLock()) {
            try {
                return joinPoint.proceed();
            } finally {
                lock.unlock();
            }
        } else {
            return skipMethodExecution(lockPrefix, stackId);
        }
    }

    private String createLockKey(String lockPrefix, Long stackId) {
        return stackId == null ? lockPrefix : lockPrefix + stackId;
    }

    private String getGuardedMethodLockPrefix(JoinPoint joinPoint) {
        try {
            return getAnnotation(GuardedMethod.class, joinPoint).lockPrefix();
        } catch (Exception ignored) {
            return "";
        }
    }

    private String getLockedMethodLockPrefix(JoinPoint joinPoint) {
        try {
            return getAnnotation(LockedMethod.class, joinPoint).lockPrefix();
        } catch (Exception ignored) {
            return "";
        }
    }

    private <T extends Annotation> T getAnnotation(Class<T> clazz, JoinPoint joinPoint) throws NoSuchMethodException {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = joinPoint.getTarget().getClass().getDeclaredMethod(joinPoint.getSignature().getName(),
                methodSignature.getMethod().getParameterTypes());
        return method.getAnnotation(clazz);
    }

    private Long getStackId(JoinPoint joinPoint) {
        Payload payload = getPayload(joinPoint);
        return payload == null ? null : payload.getStackId();
    }

    private Payload getPayload(JoinPoint joinPoint) {
        Payload payload = null;
        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof Payload) {
                payload = (Payload) arg;
            }
        }
        return payload;
    }

    private Object skipMethodExecution(String lockPrefix, Long stackId) {
        String message;
        message = stackId != null ? String.format("%s operation will be skipped on stack %d, because it is running on a different thread.", lockPrefix, stackId)
                : String.format("%s operation will be skipped, because it is running on a different thread.", lockPrefix);
        LOGGER.info(message);
        throw new CancellationException(message);
    }

    private void logWaitingOperation(String lockPrefix, Long stackId) {
        if (stackId != null) {
            LOGGER.debug("Waiting for other {} operation on stack {} to be finished.", lockPrefix, stackId);
        } else {
            LOGGER.debug("Waiting for other {} operation to be finished.", lockPrefix);
        }
    }

    private void logContinueOperation(String lockPrefix, Long stackId) {
        if (stackId != null) {
            LOGGER.debug("Continue {} operation on stack {}.", lockPrefix, stackId);
        } else {
            LOGGER.debug("Continue {} operation.", lockPrefix);
        }
    }
}

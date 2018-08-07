package com.sequenceiq.cloudbreak.security;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.aspect.HasPermissionAspects;

@Component
@Aspect
public class HasPermissionAspectForMockitoTest extends HasPermissionAspects {

    // Mockito generated code (stubbing, as well as call to getMockitoInterceptor activates the AOP joinpoint => false positives)
    private boolean stubbing = true;

    private void turnStubbingOff() {
        stubbing = false;
    }

    private void turnStubbingOn() {
        stubbing = true;
    }

    private boolean stubbingOn() {
        return stubbing;
    }

    @Override
    @Around("allRepositories()")
    public Object hasPermission(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        if (!stubbingOn() && !proceedingJoinPoint.getSignature().getName().contains("Mockito")) {
            return super.hasPermission(proceedingJoinPoint);
        }
        return proceedingJoinPoint.proceed();
    }

    public class StubbingDeactivator implements AutoCloseable {

        public StubbingDeactivator() {
            turnStubbingOff();
        }

        @Override
        public void close() throws Exception {
            turnStubbingOn();
        }
    }
}

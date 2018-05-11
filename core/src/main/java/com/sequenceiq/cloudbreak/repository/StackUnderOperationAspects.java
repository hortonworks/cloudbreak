package com.sequenceiq.cloudbreak.repository;

import javax.inject.Inject;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.StackUnderOperationService;

@Component
@Aspect
public class StackUnderOperationAspects {

    @Inject
    private StackUnderOperationService stackUnderOperationService;

    @Inject
    private StackRepository stackRepository;

    @Pointcut("execution(public * com.sequenceiq.cloudbreak.repository.StackRepository+.*(..))")
    public void interceptStackMethod() {
    }

    @Pointcut("execution(public * com.sequenceiq.cloudbreak.repository.ClusterRepository+.*(..))")
    public void interceptClusterMethod() {
    }

    @AfterReturning(pointcut = "com.sequenceiq.cloudbreak.repository.StackUnderOperationAspects.interceptStackMethod()", returning = "result")
    public void setByStack(Object result) {
        if (result instanceof Stack) {
            stackUnderOperationService.set(((Stack) result).getId());
        }
    }

    @AfterReturning(pointcut = "com.sequenceiq.cloudbreak.repository.StackUnderOperationAspects.interceptClusterMethod()", returning = "result")
    public void setByCluster(Object result) {
        if (result instanceof Cluster) {
            Cluster cluster = (Cluster) result;
            if (cluster.getStack() != null) {
                stackUnderOperationService.set(cluster.getStack().getId());
            }
        }
    }
}

package com.sequenceiq.cloudbreak.aspect;

import javax.inject.Inject;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.service.StackUnderOperationService;

@Component
@Aspect
public class StackUnderOperationAspects {

    @Inject
    private StackUnderOperationService stackUnderOperationService;

    @Pointcut("execution(public * com.sequenceiq.cloudbreak.repository.StackRepository+.*(..))")
    public void interceptStackMethod() {
    }

    @Pointcut("execution(public * com.sequenceiq.cloudbreak.repository.cluster.ClusterRepository+.*(..))")
    public void interceptClusterMethod() {
    }

    @AfterReturning(pointcut = "com.sequenceiq.cloudbreak.aspect.StackUnderOperationAspects.interceptStackMethod()", returning = "result")
    public void setByStack(Object result) {
        if (result instanceof Stack && ((Stack) result).getType() != StackType.TEMPLATE) {
            stackUnderOperationService.set(((Stack) result).getId());
        }
    }

    @AfterReturning(pointcut = "com.sequenceiq.cloudbreak.aspect.StackUnderOperationAspects.interceptClusterMethod()", returning = "result")
    public void setByCluster(Object result) {
        if (result instanceof Cluster) {
            Cluster cluster = (Cluster) result;
            if (cluster.getStack() != null && cluster.getStack().getType() != StackType.TEMPLATE) {
                stackUnderOperationService.set(cluster.getStack().getId());
            }
        }
    }
}

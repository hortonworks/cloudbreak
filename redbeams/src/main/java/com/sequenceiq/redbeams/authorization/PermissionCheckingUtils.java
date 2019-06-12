package com.sequenceiq.redbeams.authorization;

import com.sequenceiq.redbeams.service.ThreadBasedRequestIdProvider;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;

@Component
public class PermissionCheckingUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionCheckingUtils.class);

    @Inject
    private ThreadBasedRequestIdProvider threadBasedRequestIdProvider;

    @Inject
    private GrpcUmsClient grpcUmsClient;

    public void checkPermissionsByTarget(Object target, String userCrn, ResourceAction action) {
        if (target == null) {
            return;
        }
        Iterable<?> targets = target instanceof Iterable ? (Iterable<?>) target : Collections.singleton(target);

        Set<String> deniedResourceCrns = new HashSet<>();
        for (Object targetObj : targets) {
            Object resource;
            if (targetObj instanceof Optional) {
                Optional<?> optionalTargetObj = (Optional<?>) targetObj;
                if (!optionalTargetObj.isPresent()) {
                    continue;
                }
                resource = optionalTargetObj.get();
            } else {
                resource = targetObj;
            }
            if (!(resource instanceof CrnResource)) {
                throw new IllegalStateException("Target is of type " + targetObj.getClass().getName()
                    + " which is not a CrnResource, cannot perform authorization check");
            }
            String resourceCrn = ((CrnResource) resource).getResourceCrn().toString();

            LOGGER.info("Checking permissions on " + resourceCrn + " for user " + userCrn);
            String requestId = threadBasedRequestIdProvider.getRequestId();
            LOGGER.debug("- tracking with request ID {}", requestId);
            if (!grpcUmsClient.checkRight(userCrn, action.name(), resourceCrn, requestId)) {
                deniedResourceCrns.add(resourceCrn);
            }
        }

        if (!deniedResourceCrns.isEmpty()) {
            throw new AccessDeniedException(String.format("You lack %s permission to resource(s): %s", action.name(),
                String.join(", ", deniedResourceCrns)));
        }
    }

    void validateIndex(int index, int length, String indexName) {
        if (index >= length) {
            throw new IllegalArgumentException(
                    String.format("The %s [%s] cannot be bigger than or equal to the method's argument count [%s]", indexName, index, length));
        }
    }

    // CHECKSTYLE:OFF
    public Object proceed(ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) throws Throwable {
    // CHECKSTYLE:ON
        Object result = proceedingJoinPoint.proceed();
        if (result == null) {
            LOGGER.debug("Return value is null, method signature: {}", methodSignature.toLongString());
        }
        return result;
    }

}

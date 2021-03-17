package com.sequenceiq.authorization.service;

import static java.util.stream.Collectors.toList;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;

import javax.inject.Inject;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.authorization.AuthorizationProto.RightCheck;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.model.AllMatch;
import com.sequenceiq.authorization.service.model.AuthorizationRule;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.RightUtil;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

import io.grpc.StatusRuntimeException;

@Service
public class ResourceAuthorizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceAuthorizationService.class);

    @Inject
    private UmsRightProvider umsRightProvider;

    @Inject
    private GrpcUmsClient grpcUmsClient;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private List<AuthorizationFactory<? extends Annotation>> authorizationFactories;

    @Inject
    private ResourceNameFactoryService resourceNameFactoryService;

    public void authorize(String userCrn, ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature, Optional<String> requestId) {
        boolean authzEntitled = isAuthorizationEntitlementRegistered();
        Function<AuthorizationResourceAction, String> rightMapper = umsRightProvider.getRightMapper(authzEntitled);
        getAuthorization(userCrn, proceedingJoinPoint, methodSignature).ifPresent(authorization -> {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Resource authorization rule: {}", authorization.toString(rightMapper));
            }
            List<Boolean> rightCheckResults = checkWithUms(userCrn, requestId, authzEntitled, rightMapper, authorization);
            LOGGER.debug("Ums resource right check result: {}", rightCheckResults);
            Iterator<Boolean> iterator = rightCheckResults.iterator();
            authorization.evaluateAndGetFailed(iterator).ifPresentOrElse(failedAuthorization -> {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Resource authorization failed: {}", failedAuthorization.toString(rightMapper));
                }
                if (authzEntitled) {
                    Map<String, Optional<String>> crnNameMap = resourceNameFactoryService.getNames(collectResourceCrns(failedAuthorization));
                    throw new AccessDeniedException(failedAuthorization.getAsFailureMessage(rightMapper, getNameOrDefault(crnNameMap)));
                } else {
                    throw new AccessDeniedException(convertToLegacyFailureMeessage(userCrn, failedAuthorization, rightMapper));
                }
            }, () -> LOGGER.debug("Resource authorization was successfull."));
        });
    }

    private Function<String, Optional<String>> getNameOrDefault(Map<String, Optional<String>> nameMap) {
        return name -> nameMap.getOrDefault(name, Optional.empty());
    }

    private Collection<String> collectResourceCrns(AuthorizationRule authorizationRule) {
        Set<String> crns = new HashSet<>();
        authorizationRule.convert((action, crn) -> crns.add(crn));
        return crns;
    }

    private boolean isAuthorizationEntitlementRegistered() {
        try {
            return entitlementService.isAuthorizationEntitlementRegistered(ThreadBasedUserCrnProvider.getAccountId());
        } catch (StatusRuntimeException e) {
            throw new BadRequestException(e.getMessage(), e);
        }
    }

    private List<Boolean> checkWithUms(String userCrn, Optional<String> requestId, boolean authzEntitled,
            Function<AuthorizationResourceAction, String> rightMapper, AuthorizationRule authorization) {
        if (authzEntitled) {
            List<RightCheck> rightChecks = convertToRightChecks(authorization, rightMapper);
            LOGGER.debug("Ums resource right check request: {}", rightChecks);
            return grpcUmsClient.hasRights(userCrn, userCrn, rightChecks, requestId);
        } else {
            List<Boolean> readRightResults = convertReadRightsToTrueOthersToNull(authorization, rightMapper);
            List<RightCheck> rightChecks = createRightChecksFromNonReadRights(authorization, rightMapper);
            LOGGER.debug("Legacy ums right check request: {}", rightChecks);
            List<Boolean> umsResults = grpcUmsClient.hasRights(userCrn, userCrn, rightChecks, requestId);
            return merge(readRightResults, umsResults);
        }
    }

    private Optional<AuthorizationRule> getAuthorization(String userCrn, ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        List<Optional<AuthorizationRule>> authorizations = authorizationFactories
                .stream()
                .filter(a -> methodSignature.getMethod().isAnnotationPresent(a.supportedAnnotation()))
                .map(a -> a.getAuthorization(methodSignature.getMethod().getAnnotation(a.supportedAnnotation()), userCrn, proceedingJoinPoint, methodSignature))
                .collect(toList());
        return AllMatch.from(authorizations);
    }

    private List<RightCheck> convertToRightChecks(AuthorizationRule rule, Function<AuthorizationResourceAction, String> rightMapper) {
        List<RightCheck> rightChecks = new ArrayList<>();
        rule.convert((action, crn) -> rightChecks.add(RightCheck.newBuilder()
                .setRight(rightMapper.apply(action))
                .setResource(crn)
                .build()));
        return rightChecks;
    }

    private List<Boolean> convertReadRightsToTrueOthersToNull(AuthorizationRule rule, Function<AuthorizationResourceAction, String> rightMapper) {
        List<Boolean> rightChecks = new ArrayList<>();
        rule.convert((action, crn) -> {
            if (RightUtil.isReadRight(rightMapper.apply(action))) {
                rightChecks.add(Boolean.TRUE);
            } else {
                rightChecks.add(null);
            }
        });
        return rightChecks;
    }

    private List<RightCheck> createRightChecksFromNonReadRights(AuthorizationRule rule, Function<AuthorizationResourceAction, String> rightMapper) {
        List<RightCheck> rightChecks = new ArrayList<>();
        rule.convert((action, crn) -> {
            if (!RightUtil.isReadRight(rightMapper.apply(action))) {
                rightChecks.add(RightCheck.newBuilder()
                        .setRight(rightMapper.apply(action))
                        .build());
            }
        });
        return rightChecks;
    }

    private List<Boolean> merge(List<Boolean> intermediateResults, List<Boolean> umsResults) {
        List<Boolean> result = new ArrayList<>();
        Queue<Boolean> umsResultQueue = new LinkedList<>(umsResults);
        for (Boolean value : intermediateResults) {
            if (value != null) {
                result.add(value);
            } else {
                result.add(umsResultQueue.poll());
            }
        }
        return result;
    }

    private String convertToLegacyFailureMeessage(String userCrn, AuthorizationRule failedAuthorization,
            Function<AuthorizationResourceAction, String> rightMapper) {
        Set<String> rights = new LinkedHashSet<>();
        failedAuthorization.convert((action, crn) -> rights.add(rightMapper.apply(action)));
        String format = "You have no right to perform %s in account %s.";
        if (rights.size() == 1) {
            return String.format(format, rights.iterator().next(), Crn.fromString(userCrn).getAccountId());
        } else {
            return String.format(format, String.join(", ", rights), Crn.fromString(userCrn).getAccountId());
        }
    }
}

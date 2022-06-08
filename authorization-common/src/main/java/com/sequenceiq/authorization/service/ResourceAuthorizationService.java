package com.sequenceiq.authorization.service;

import static java.util.stream.Collectors.toList;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;

@Service
public class ResourceAuthorizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceAuthorizationService.class);

    @Inject
    private UmsRightProvider umsRightProvider;

    @Inject
    private GrpcUmsClient grpcUmsClient;

    @Inject
    private List<AuthorizationFactory<? extends Annotation>> authorizationFactories;

    @Inject
    private ResourceNameFactoryService resourceNameFactoryService;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public void authorize(String userCrn, ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        Function<AuthorizationResourceAction, String> rightMapper = umsRightProvider.getRightMapper();
        getAuthorization(userCrn, proceedingJoinPoint, methodSignature).ifPresent(authorization -> {
            LOGGER.debug("Resource authorization rule: {}", authorization.toString(rightMapper));
            List<Boolean> rightCheckResults = checkWithUms(userCrn, rightMapper, authorization);
            LOGGER.debug("Ums resource right check result: {}", rightCheckResults);
            Iterator<Boolean> iterator = rightCheckResults.iterator();
            authorization.evaluateAndGetFailed(iterator).ifPresentOrElse(failedAuthorization -> {
                LOGGER.debug("Resource authorization failed: {}", failedAuthorization.toString(rightMapper));
                Map<String, Optional<String>> crnNameMap = resourceNameFactoryService.getNames(collectResourceCrns(failedAuthorization));
                throw new AccessDeniedException(failedAuthorization.getAsFailureMessage(rightMapper, getNameOrDefault(crnNameMap)));
            }, () -> LOGGER.debug("Resource authorization was successful."));
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

    private List<Boolean> checkWithUms(String userCrn, Function<AuthorizationResourceAction, String> rightMapper, AuthorizationRule authorization) {
        List<RightCheck> rightChecks = convertToRightChecks(authorization, rightMapper);
        LOGGER.debug("Ums resource right check request: {}", rightChecks);
        return grpcUmsClient.hasRights(userCrn, rightChecks, regionAwareInternalCrnGeneratorFactory);
    }

    private Optional<AuthorizationRule> getAuthorization(String userCrn, ProceedingJoinPoint proceedingJoinPoint, MethodSignature methodSignature) {
        Function<AuthorizationFactory, Annotation> getAnnotation = authFactory -> methodSignature.getMethod().getAnnotation(authFactory.supportedAnnotation());
        List<Optional<AuthorizationRule>> authorizations = authorizationFactories
                .stream()
                .filter(authFactory -> methodSignature.getMethod().isAnnotationPresent(authFactory.supportedAnnotation()))
                .map(authFactory -> authFactory.getAuthorization(getAnnotation.apply(authFactory), userCrn, proceedingJoinPoint, methodSignature))
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
}

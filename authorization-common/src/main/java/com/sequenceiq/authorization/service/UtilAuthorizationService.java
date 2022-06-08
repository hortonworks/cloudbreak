package com.sequenceiq.authorization.service;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.authorization.AuthorizationProto;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.sequenceiq.authorization.info.model.CheckResourceRightV4Response;
import com.sequenceiq.authorization.info.model.CheckResourceRightV4SingleResponse;
import com.sequenceiq.authorization.info.model.CheckResourceRightsV4Request;
import com.sequenceiq.authorization.info.model.CheckResourceRightsV4Response;
import com.sequenceiq.authorization.info.model.CheckRightOnResourcesV4Request;
import com.sequenceiq.authorization.info.model.CheckRightOnResourcesV4Response;
import com.sequenceiq.authorization.info.model.CheckRightV4Request;
import com.sequenceiq.authorization.info.model.CheckRightV4Response;
import com.sequenceiq.authorization.info.model.CheckRightV4SingleResponse;
import com.sequenceiq.authorization.info.model.RightV4;
import com.sequenceiq.authorization.service.list.Resource;
import com.sequenceiq.authorization.service.list.ResourceFilteringService;
import com.sequenceiq.authorization.service.list.ResourceListProvider;
import com.sequenceiq.authorization.service.model.AuthorizationRule;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;

@Service
public class UtilAuthorizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UtilAuthorizationService.class);

    @Inject
    private UmsRightProvider umsRightProvider;

    @Inject
    private GrpcUmsClient grpcUmsClient;

    @Inject
    private ResourceCrnAthorizationFactory resourceCrnAthorizationFactory;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private Optional<ResourceListProvider> authorizationResourceProvider;

    @Inject
    private ResourceFilteringService resourceFilteringService;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public CheckRightV4Response checkRights(CheckRightV4Request rightReq) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        List<AuthorizationProto.RightCheck> rightChecks = rightReq.getRights().stream()
                .map(rightV4 -> createRightCheckObject(umsRightProvider.getRight(rightV4.getAction()), null))
                .collect(Collectors.toList());
        List<Boolean> results = grpcUmsClient.hasRights(userCrn, rightChecks, regionAwareInternalCrnGeneratorFactory);
        return new CheckRightV4Response(rightReq.getRights().stream()
                .map(rightV4 -> new CheckRightV4SingleResponse(rightV4, results.get(rightReq.getRights().indexOf(rightV4))))
                .collect(Collectors.toList()));
    }

    public CheckResourceRightsV4Response checkRightsOnResources(CheckResourceRightsV4Request checkResourceRightsV4Request) {
        Multimap<ImmutablePair<String, RightV4>, AuthorizationProto.RightCheck> resourceRightsChecks = LinkedListMultimap.create();
        checkResourceRightsV4Request.getResourceRights()
                .forEach(resourceRightsV4 -> resourceRightsV4.getRights()
                        .forEach(rightV4 -> addRightCheck(resourceRightsV4.getResourceCrn(), rightV4, resourceRightsChecks)));
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        List<AuthorizationProto.RightCheck> rightChecks = Lists.newLinkedList(resourceRightsChecks.values());

        LOGGER.info("Check rights: {}", rightChecks);
        List<Boolean> results = grpcUmsClient.hasRights(userCrn, rightChecks, regionAwareInternalCrnGeneratorFactory);

        Map<AuthorizationProto.RightCheck, Boolean> rightCheckResultMap = new HashMap<>();
        for (AuthorizationProto.RightCheck rightCheck : rightChecks) {
            rightCheckResultMap.put(rightCheck, results.get(rightChecks.indexOf(rightCheck)));
        }

        return generateResponse(resourceRightsChecks, rightCheckResultMap);
    }

    public CheckRightOnResourcesV4Response checkRightOnResources(CheckRightOnResourcesV4Request request) {
        checkArgument(request.getResourceCrns() != null, "Resource crns must not be null.");
        request.getResourceCrns().forEach(crn -> checkArgument(Crn.isCrn(crn), crn + " is not a valid crn."));
        Crn userCrn = Crn.ofUser(ThreadBasedUserCrnProvider.getUserCrn());
        if (!entitlementService.listFilteringEnabled(userCrn.getAccountId())) {
            throw new BadRequestException("Resource based filtering is not enabled in the current account.");
        }
        if (authorizationResourceProvider.isEmpty()) {
            throw new UnsupportedOperationException("Service doesn't support resource filtering.");
        }
        return checkRightOnResources(userCrn, request, authorizationResourceProvider.get());
    }

    private void addRightCheck(String resourceCrn, RightV4 rightV4,
            Multimap<ImmutablePair<String, RightV4>, AuthorizationProto.RightCheck> resourceRightsChecks) {
        Optional<AuthorizationRule> authorizationRuleOptional =
                resourceCrnAthorizationFactory.calcAuthorization(resourceCrn, rightV4.getAction());
        if (authorizationRuleOptional.isPresent()) {
            AuthorizationRule authorizationRule = authorizationRuleOptional.get();
            authorizationRule.convert((action, resource) -> {
                AuthorizationProto.RightCheck rightCheckObject = createRightCheckObject(action.getRight(), resource);
                resourceRightsChecks.put(new ImmutablePair<>(resourceCrn, rightV4), rightCheckObject);
            });
        } else {
            AuthorizationProto.RightCheck rightCheckObject = createRightCheckObject(rightV4.getAction().getRight(), resourceCrn);
            resourceRightsChecks.put(new ImmutablePair<>(resourceCrn, rightV4), rightCheckObject);
            LOGGER.info("Can't find authorization rules for the following resource:{} ({}). " +
                            "Please make sure you are calling the right service for the resource?",
                    resourceCrn, rightV4.getAction());
        }
    }

    private CheckResourceRightsV4Response generateResponse(Multimap<ImmutablePair<String, RightV4>, AuthorizationProto.RightCheck> resourceRightsChecks,
            Map<AuthorizationProto.RightCheck, Boolean> rightCheckResultMap) {
        CheckResourceRightsV4Response response = new CheckResourceRightsV4Response(Lists.newArrayList());
        Map<ImmutablePair<String, RightV4>, Boolean> rightCheckResults = new HashMap<>();
        for (ImmutablePair<String, RightV4> resourceRight : resourceRightsChecks.keys()) {
            Collection<AuthorizationProto.RightCheck> rightChecks = resourceRightsChecks.get(resourceRight);
            Boolean hasRight = hasRight(rightCheckResultMap, rightChecks);
            rightCheckResults.put(resourceRight, hasRight);
        }

        rightCheckResults.forEach((rightCheck, result) -> {
            if (getResourceRightSingleResponse(response, rightCheck.getLeft()).isEmpty()) {
                response.getResponses().add(new CheckResourceRightV4SingleResponse(rightCheck.getLeft(), Lists.newArrayList()));
            }
            CheckRightV4SingleResponse singleResponse = new CheckRightV4SingleResponse(
                    rightCheck.getRight(),
                    result);
            getResourceRightSingleResponse(response, rightCheck.getLeft()).get().getRights().add(singleResponse);
        });
        return response;
    }

    private boolean hasRight(Map<AuthorizationProto.RightCheck, Boolean> rightCheckResultMap, Collection<AuthorizationProto.RightCheck> rightChecks) {
        for (AuthorizationProto.RightCheck rightCheck : rightChecks) {
            if (rightCheckResultMap.get(rightCheck)) {
                return true;
            }
        }
        return false;
    }

    private Optional<CheckResourceRightV4SingleResponse> getResourceRightSingleResponse(CheckResourceRightsV4Response response, String resource) {
        return response.getResponses().stream()
                .filter(singleResponseStream -> StringUtils.equals(singleResponseStream.getResourceCrn(), resource))
                .findFirst();
    }

    private AuthorizationProto.RightCheck createRightCheckObject(String right, String resource) {
        AuthorizationProto.RightCheck.Builder builder = AuthorizationProto.RightCheck.newBuilder()
                .setRight(right);
        if (resource != null) {
            builder.setResource(resource);
        }
        return builder.build();
    }

    private CheckRightOnResourcesV4Response checkRightOnResources(Crn userCrn, CheckRightOnResourcesV4Request request,
            ResourceListProvider authorizationResourceProvider) {
        List<Resource> resources = authorizationResourceProvider.findResources(userCrn.getAccountId(), request.getResourceCrns());
        List<CheckResourceRightV4Response> responses = resourceFilteringService.filter(userCrn, request.getRight().getAction(),
                resources, hasRightPredicate ->
                        request.getResourceCrns()
                                .stream()
                                .map(resourceCrn -> {
                                    CheckResourceRightV4Response checkResourceRightV4Response = new CheckResourceRightV4Response();
                                    checkResourceRightV4Response.setResourceCrn(resourceCrn);
                                    checkResourceRightV4Response.setResult(hasRightPredicate.test(resourceCrn));
                                    return checkResourceRightV4Response;
                                }).collect(Collectors.toList()));
        CheckRightOnResourcesV4Response response = new CheckRightOnResourcesV4Response();
        response.setRight(request.getRight());
        response.setResponses(responses);
        return response;
    }
}

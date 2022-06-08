package com.sequenceiq.authorization.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.google.common.collect.Lists;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.utils.AuthorizationMessageUtilsService;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;

@Service
public class UmsResourceAuthorizationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UmsResourceAuthorizationService.class);

    @Inject
    private GrpcUmsClient umsClient;

    @Inject
    private UmsRightProvider umsRightProvider;

    @Inject
    private AuthorizationMessageUtilsService authorizationMessageUtilsService;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public void checkRightOfUserOnResource(String userCrn, AuthorizationResourceAction action, String resourceCrn) {
        String right = umsRightProvider.getRight(action);
        checkRightOfUserOnResource(userCrn, right, resourceCrn, authorizationMessageUtilsService.formatTemplate(right, resourceCrn));
    }

    public Map<String, Boolean> getRightOfUserOnResources(String userCrn, AuthorizationResourceAction action, List<String> resourceCrns) {
        return umsClient.hasRights(userCrn, resourceCrns, umsRightProvider.getRight(action), regionAwareInternalCrnGeneratorFactory);
    }

    public void checkRightOfUserOnResources(String userCrn, AuthorizationResourceAction action, Collection<String> resourceCrns) {
        String right = umsRightProvider.getRight(action);
        checkRightOfUserOnResources(userCrn, right, resourceCrns, authorizationMessageUtilsService.formatTemplate(right, resourceCrns));
    }

    private void checkRightOfUserOnResource(String userCrn, String right, String resourceCrn, String unauthorizedMessage) {
        if (!umsClient.checkResourceRight(userCrn, right, resourceCrn, regionAwareInternalCrnGeneratorFactory)) {
            LOGGER.error(unauthorizedMessage);
            throw new AccessDeniedException(unauthorizedMessage);
        }
    }

    private void checkRightOfUserOnResources(String userCrn, String right, Collection<String> resourceCrns, String unauthorizedMessage) {
        if (!umsClient.hasRights(userCrn, Lists.newArrayList(resourceCrns), right, regionAwareInternalCrnGeneratorFactory)
                .values().stream().allMatch(Boolean::booleanValue)) {
            LOGGER.error(unauthorizedMessage);
            throw new AccessDeniedException(unauthorizedMessage);
        }
    }
}

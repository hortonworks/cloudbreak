package com.sequenceiq.cloudbreak.controller.v4;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.restartinstances.RestartInstancesV4Endpoint;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.cloudbreak.service.StackCommonService;

@Controller
@Transactional(Transactional.TxType.NEVER)
public class RestartInstancesV4Controller implements RestartInstancesV4Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestartInstancesV4Controller.class);

    @Inject
    private StackCommonService stackCommonService;

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.SCALE_DATAHUB)
    public void restartInstancesForClusterCrn(@TenantAwareParam @ResourceCrn String clusterCrn, List<String> instanceIds) {
        LOGGER.info("restartInstancesForClusterCrn: clusterCrn={}, instanceIds=[{}]", clusterCrn, instanceIds);
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        stackCommonService.restartMultipleInstances(NameOrCrn.ofCrn(clusterCrn), accountId, instanceIds);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.SCALE_DATAHUB)
    public void restartInstancesForClusterName(@ResourceName String clusterName, List<String> instanceIds) {
        LOGGER.info("restartInstancesForClusterName: clusterName={}, instanceIds=[{}]", clusterName, instanceIds);
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        stackCommonService.restartMultipleInstances(NameOrCrn.ofName(clusterName), accountId, instanceIds);
    }
}
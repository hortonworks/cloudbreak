package com.sequenceiq.cloudbreak.service.authorization;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.authorization.service.UmsAccountAuthorizationService;
import com.sequenceiq.authorization.service.UmsRightProvider;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.base.RightV4;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;

@Service
public class UtilAuthorizationService {

    @Inject
    private UmsRightProvider umsRightProvider;

    @Inject
    private UmsAccountAuthorizationService umsAccountAuthorizationService;

    @Inject
    private GrpcUmsClient grpcUmsClient;

    public Boolean getRightResult(RightV4 rightReq) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        if (!grpcUmsClient.isAuthorizationEntitlementRegistered(userCrn, ThreadBasedUserCrnProvider.getAccountId())) {
            return umsAccountAuthorizationService.hasRightOfUser(userCrn, umsRightProvider.getRight(rightReq.getAction()));
        }
        return Boolean.TRUE;
    }
}

package com.sequenceiq.freeipa.service.freeipa.user;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.authdistributor.AuthDistributorProto.UserState;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.authdistributor.GrpcAuthDistributorClient;
import com.sequenceiq.freeipa.converter.freeipa.user.UmsUsersStateToAuthDistributorUserStateConverter;
import com.sequenceiq.freeipa.service.freeipa.user.model.UmsUsersState;

@Service
public class AuthDistributorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthDistributorService.class);

    @Inject
    private GrpcAuthDistributorClient grpcAuthDistributorClient;

    @Inject
    private UmsUsersStateToAuthDistributorUserStateConverter umsUsersStateToAuthDistributorUserStateConverter;

    @Inject
    private EntitlementService entitlementService;

    public void updateAuthViewForEnvironment(String environmentCrn, UmsUsersState umsUsersState, String accountId, String operationId) {
        if (entitlementService.isSdxSaasIntegrationEnabled(accountId)) {
            try {
                LOGGER.debug("Update auth view in auth distributor for env: {}, operationId: {}", environmentCrn, operationId);
                UserState userState = umsUsersStateToAuthDistributorUserStateConverter.convert(umsUsersState);
                grpcAuthDistributorClient.updateAuthViewForEnvironment(environmentCrn, userState);
                LOGGER.debug("Update auth view in auth distributor finished for env: {}, operationId: {}", environmentCrn, operationId);
            } catch (Exception e) {
                LOGGER.error("Update auth view in auth distributor failed for env: {}, operationId: {}", environmentCrn, operationId, e);
            }
        }
    }

    public void removeAuthViewForEnvironment(String environmentCrn, String accountId) {
        if (entitlementService.isSdxSaasIntegrationEnabled(accountId)) {
            try {
                LOGGER.debug("Remove auth view from auth distributor for env: {}", environmentCrn);
                grpcAuthDistributorClient.removeAuthViewForEnvironment(environmentCrn);
                LOGGER.debug("Remove auth view from auth distributor finished for env: {}", environmentCrn);
            } catch (Exception e) {
                LOGGER.error("Remove auth view from auth distributor failed for env: {}", environmentCrn, e);
            }
        }
    }
}

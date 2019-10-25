package com.sequenceiq.cloudbreak.auth.altus;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;

@Service
public class EntitlementService {

    @Inject
    private GrpcUmsClient umsClient;

    public boolean ccmEnabled(String userCrn) {
        return isEntitlementRegistered(userCrn, "CDP_REVERSE_SSH_TUNNEL");
    }

    public boolean azureEnabled(String userCrn) {
        return isEntitlementRegistered(userCrn, "CDP_AZURE");
    }

    public List<String> getEntitlements(String userCrn) {
        return getAccount(userCrn).getEntitlementsList()
                .stream()
                .map(e -> e.getEntitlementName().toUpperCase())
                .collect(Collectors.toList());
    }

    private UserManagementProto.Account getAccount(String userCrn) {
        return umsClient.getAccountDetails(userCrn, userCrn, Optional.empty());
    }

    private boolean isEntitlementRegistered(String userCrn, String entitlement) {
        return getEntitlements(userCrn)
                .stream()
                .filter(e -> e.equalsIgnoreCase(entitlement))
                .findFirst()
                .isPresent();
    }

}

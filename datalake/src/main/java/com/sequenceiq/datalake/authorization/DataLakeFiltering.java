package com.sequenceiq.datalake.authorization;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.list.AbstractAuthorizationFiltering;
import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.service.sdx.SdxService;

@Component
public class DataLakeFiltering extends AbstractAuthorizationFiltering<List<SdxCluster>> {

    public static final String ENV_NAME = "ENV_NAME";

    public static final String ENV_CRN = "ENV_CRN";

    public static final String INCLUDE_DETACHED = "INCLUDE_DETACHED";

    @Inject
    private SdxService sdxService;

    public List<SdxCluster> filterDataLakesByEnvNameOrAll(AuthorizationResourceAction action, String environmentName, boolean includeDetached) {
        Map<String, Object> args = new HashMap<>();
        args.put(ENV_NAME, environmentName);
        if (includeDetached) {
            args.put(INCLUDE_DETACHED, "yes");
        }
        return filterResources(Crn.safeFromString(ThreadBasedUserCrnProvider.getUserCrn()), action, args);
    }

    public List<SdxCluster> getAllDataLakesByEnvNameOrAll(AuthorizationResourceAction action, String environmentName) {
        Map<String, Object> args = new HashMap<>();
        args.put(ENV_NAME, environmentName);
        args.put(INCLUDE_DETACHED, "yes");
        return filterResources(Crn.safeFromString(ThreadBasedUserCrnProvider.getUserCrn()), action, args);
    }

    public List<SdxCluster> filterDataLakesByEnvCrn(AuthorizationResourceAction action, String environmentCrn, boolean includeDetached) {
        Map<String, Object> args = new HashMap<>();
        args.put(ENV_CRN, environmentCrn);
        if (includeDetached) {
            args.put(INCLUDE_DETACHED, "yes");
        }
        return filterResources(Crn.safeFromString(ThreadBasedUserCrnProvider.getUserCrn()), action, args);
    }

    @Override
    public List<ResourceWithId> getAllResources(Map<String, Object> args) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        if (hasParam(ENV_NAME, args)) {
            String envName = getEnvName(args);
            if (Objects.isNull(envName)) {
                return sdxService.findAsAuthorizationResorces(accountId);
            } else {
                return sdxService.findAsAuthorizationResorcesByEnvName(accountId, envName);
            }
        } else {
            String envCrn = getEnvCrn(args);
            return sdxService.findAsAuthorizationResorcesByEnvCrn(accountId, envCrn);
        }
    }

    @Override
    public List<SdxCluster> filterByIds(List<Long> authorizedResourceIds, Map<String, Object> args) {
        return Lists.newArrayList(sdxService.findAllById(authorizedResourceIds));
    }

    @Override
    public List<SdxCluster> getAll(Map<String, Object> args) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();
        if (hasParam(ENV_NAME, args)) {
            if (hasParam(INCLUDE_DETACHED, args)) {
                return sdxService.listAllSdx(userCrn, getEnvName(args));
            } else {
                return sdxService.listSdx(userCrn, getEnvName(args));
            }
        } else {
            if (hasParam(INCLUDE_DETACHED, args)) {
                return sdxService.listAllSdxByEnvCrn(userCrn, getEnvCrn(args));
            } else {
                return sdxService.listSdxByEnvCrn(userCrn, getEnvCrn(args));
            }
        }
    }

    public String getEnvName(Map<String, Object> args) {
        return (String) args.get(ENV_NAME);
    }

    public String getEnvCrn(Map<String, Object> args) {
        return (String) args.get(ENV_CRN);
    }

    public boolean hasParam(String paramName, Map<String, Object> args) {
        return args.containsKey(paramName);
    }
}

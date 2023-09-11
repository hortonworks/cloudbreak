package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.common.model.OsType.CENTOS7;
import static com.sequenceiq.common.model.OsType.RHEL8;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.provider.ProviderPreferencesService;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.environment.api.v1.environment.model.response.SupportedOperatingSystemResponse;

@Service
public class SupportedOperatingSystemService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SupportedOperatingSystemService.class);

    private final EntitlementService entitlementService;

    private final ProviderPreferencesService providerPreferencesService;

    public SupportedOperatingSystemService(EntitlementService entitlementService, ProviderPreferencesService providerPreferencesService) {
        this.entitlementService = entitlementService;
        this.providerPreferencesService = providerPreferencesService;
    }

    public SupportedOperatingSystemResponse listSupportedOperatingSystem(String accountId) {
        SupportedOperatingSystemResponse response = new SupportedOperatingSystemResponse();

        if (providerPreferencesService.isGovCloudDeployment()) {
            response.setOsTypes(List.of(RHEL8));
            response.setDefaultOs(RHEL8.getOs());
            LOGGER.info("List of supported OS for gov cloud response: {}", response);
        } else {
            boolean rhel8Enabled = entitlementService.isRhel8ImageSupportEnabled(accountId);
            List<OsType> supportedOs = Arrays.stream(OsType.values()).filter(r -> rhel8Enabled || r != RHEL8).collect(Collectors.toList());
            response.setOsTypes(supportedOs);

            boolean rhel8Default = entitlementService.isRhel8ImagePreferred(accountId);
            if (rhel8Enabled && rhel8Default) {
                response.setDefaultOs(RHEL8.getOs());
            } else {
                response.setDefaultOs(CENTOS7.getOs());
            }
            LOGGER.info("List of supported OS. rhel8Enabled: {}, rhel8Default: {}, response: {}", rhel8Enabled, rhel8Default, response);
        }

        return response;
    }
}

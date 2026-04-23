package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.common.model.OsType.CENTOS7;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.provider.ProviderPreferencesService;
import com.sequenceiq.common.model.OsType;
import com.sequenceiq.environment.api.v1.environment.OsTypeToOsTypeResponseConverter;
import com.sequenceiq.environment.api.v1.environment.model.response.OsTypeResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.SupportedOperatingSystemResponse;

@Service
public class SupportedOperatingSystemService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SupportedOperatingSystemService.class);

    private final EntitlementService entitlementService;

    private final ProviderPreferencesService providerPreferencesService;

    private final OsTypeToOsTypeResponseConverter osTypeToOsTypeResponseConverter;

    public SupportedOperatingSystemService(EntitlementService entitlementService, ProviderPreferencesService providerPreferencesService,
            OsTypeToOsTypeResponseConverter osTypeToOsTypeResponseConverter) {
        this.entitlementService = entitlementService;
        this.providerPreferencesService = providerPreferencesService;
        this.osTypeToOsTypeResponseConverter = osTypeToOsTypeResponseConverter;
    }

    public SupportedOperatingSystemResponse listSupportedOperatingSystem(String accountId, String cloudPlatform) {
        SupportedOperatingSystemResponse response = new SupportedOperatingSystemResponse();
        OsType latestOsType = OsType.getLatestOsType();
        boolean latestOsEnabled = entitlementService.isEntitledToUseOS(accountId, latestOsType);
        List<OsTypeResponse> supportedOs;
        if (providerPreferencesService.isGovCloudDeployment()) {
            supportedOs = Arrays.stream(OsType.values())
                    .filter(os -> !CENTOS7.equals(os))
                    .filter(os -> !latestOsType.equals(os) || latestOsEnabled)
                    .map(osTypeToOsTypeResponseConverter::convert).collect(Collectors.toList());
        } else {
            supportedOs = Arrays.stream(OsType.values())
                    .filter(os -> !latestOsType.equals(os) || latestOsEnabled)
                    .map(osTypeToOsTypeResponseConverter::convert)
                    .collect(Collectors.toList());
        }
        response.setOsTypes(supportedOs);
        response.setDefaultOs(latestOsEnabled ? latestOsType.getOs() : OsType.getPreviousOsType(latestOsType).getOs());
        LOGGER.info("List of supported OS. Response: {}", response);
        return response;
    }

}

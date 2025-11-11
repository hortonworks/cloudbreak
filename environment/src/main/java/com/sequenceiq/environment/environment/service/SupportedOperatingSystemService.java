package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.common.model.OsType.RHEL8;
import static com.sequenceiq.common.model.OsType.RHEL9;

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

        if (providerPreferencesService.isGovCloudDeployment()) {
            response.setOsTypes(List.of(osTypeToOsTypeResponseConverter.convert(RHEL8)));
            response.setDefaultOs(RHEL8.getOs());
            LOGGER.info("List of supported OS for gov cloud response: {}", response);
        } else {
            boolean rhel9Enabled = entitlementService.isEntitledToUseOS(accountId, RHEL9);
            boolean preferRhel9Enabled = entitlementService.isRhel9ImagePreferred(accountId);
            List<OsTypeResponse> supportedOs = Arrays.stream(OsType.values())
                    .filter(os -> !RHEL9.equals(os) || rhel9Enabled)
                    .map(osTypeToOsTypeResponseConverter::convert).collect(Collectors.toList());
            response.setOsTypes(supportedOs);
            response.setDefaultOs(preferRhel9Enabled ? RHEL9.getOs() : RHEL8.getOs());
            LOGGER.info("List of supported OS. response: {}", response);
        }

        return response;
    }

}

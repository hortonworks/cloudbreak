package com.sequenceiq.it.cloudbreak.action.ums;

import com.sequenceiq.cloudbreak.auth.altus.service.RoleCrnGenerator;
import com.sequenceiq.cloudbreak.auth.altus.service.UmsResourceRole;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.it.cloudbreak.UmsClient;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;

public class UmsClientUtils {

    private UmsClientUtils() {

    }

    public static String getResourceRoleCrn(UmsTestDto umsTestDto, UmsClient umsClient,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        String resourceCrn = umsTestDto.getRequest().getResourceCrn();
        UmsResourceRole resourceRole = umsTestDto.getRequest().getUmsResourceRole();
        return getResourceRoleCrn(resourceRole, umsClient, resourceCrn, regionAwareInternalCrnGeneratorFactory);
    }

    public static String getResourceRoleCrn(UmsResourceRole umsResourceRole, UmsClient umsClient, String resourceCrn,
            RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory) {
        Crn resourceRoleCrn = RoleCrnGenerator.getCrnFromResourceRoles(umsResourceRole,
                umsClient.getDefaultClient().getResourceRoles(Crn.fromString(resourceCrn).getAccountId(), regionAwareInternalCrnGeneratorFactory));
        return resourceRoleCrn.toString();
    }
}

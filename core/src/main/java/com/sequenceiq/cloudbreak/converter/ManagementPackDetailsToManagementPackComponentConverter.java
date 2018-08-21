package com.sequenceiq.cloudbreak.converter;

import java.util.Arrays;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.mpack.ManagementPackDetails;
import com.sequenceiq.cloudbreak.cloud.model.component.ManagementPackComponent;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.ManagementPack;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.mpack.ManagementPackService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Component
public class ManagementPackDetailsToManagementPackComponentConverter
        extends AbstractConversionServiceAwareConverter<ManagementPackDetails, ManagementPackComponent> {
    @Inject
    private ManagementPackService managementPackService;

    @Inject
    private OrganizationService organizationService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public ManagementPackComponent convert(ManagementPackDetails source) {
        ManagementPackComponent mpack = new ManagementPackComponent();
        if (StringUtils.isNoneEmpty(source.getName())) {
            User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
            Organization organization = organizationService.get(restRequestThreadLocalService.getRequestedOrgId(), user);
            ManagementPack dmpack = managementPackService.getByNameForOrganization(source.getName(), organization);
            mpack.setName(source.getName());
            mpack.setMpackUrl(dmpack.getMpackUrl());
            mpack.setStackDefault(false);
            mpack.setPreInstalled(false);
            mpack.setForce(dmpack.isForce());
            mpack.setPurge(dmpack.isPurge());
            if (StringUtils.isNoneEmpty(dmpack.getPurgeList())) {
                mpack.setPurgeList(Arrays.asList(dmpack.getPurgeList().split(",")));
            }
        } else {
            throw new BadRequestException("Mpack name cannot be empty!");
        }
        return mpack;
    }
}

package com.sequenceiq.cloudbreak.cmtemplate.configproviders.efm;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_18;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.efm.EfmRoles.EFM;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.efm.EfmRoles.EFM_SERVER;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.UmsVirtualGroupRight;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupRequest;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class EfmConfigProvider extends AbstractRoleConfigProvider {

    protected static final String EFM_ADMIN_IDENTITIES = "efm.admin.identity";

    protected static final String EFM_ADMIN_GROUP_IDENTITIES = "efm.security.user.auth.groups.adminIdentities";

    private static final Logger LOGGER = LoggerFactory.getLogger(EfmConfigProvider.class);

    @Inject
    private GrpcUmsClient umsClient;

    @Inject
    private VirtualGroupService virtualGroupService;

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        LOGGER.info("Adding initial admin and initial admin group properties value via EfmConfigProvider");
        List<ApiClusterTemplateConfig> configs = new ArrayList<>();

        if (isVersionNewerOrEqualThanLimited(
                Optional.ofNullable(source.getBlueprintView().getProcessor().getStackVersion()).orElse(""),
                CLOUDERA_STACK_VERSION_7_2_18)) {
            VirtualGroupRequest virtualGroupRequest = source.getVirtualGroupRequest();
            String adminGroup = virtualGroupService.createOrGetVirtualGroup(virtualGroupRequest, UmsVirtualGroupRight.EFM_ADMIN);
            LOGGER.info("{} = {} added to template", EFM_ADMIN_GROUP_IDENTITIES, adminGroup);
            configs.add(config(EFM_ADMIN_GROUP_IDENTITIES, adminGroup));
        }

        Optional.ofNullable(source.getGeneralClusterConfigs().getCreatorWorkloadUserCrn())
            .flatMap(this::getUserNameFromUserCrn)
            .ifPresent(userName -> {
                LOGGER.info("{} = {} added to template", EFM_ADMIN_IDENTITIES, userName);
                configs.add(config(EFM_ADMIN_IDENTITIES, userName));
            });

        return configs;
    }

    @Override
    public String getServiceType() {
        return EFM;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(EFM_SERVER);
    }

    private Optional<String> getUserNameFromUserCrn(String userCrn) {
        Crn crn = Crn.safeFromString(userCrn);
        switch (crn.getResourceType()) {
            case USER:
                return Optional.of(umsClient.getUserDetails(userCrn).getWorkloadUsername());
            default:
                LOGGER.warn(String.format("UserCrn %s is not of resource type USER. EFM bootstrapping requires USER creator.", userCrn));
        }

        return Optional.empty();
    }
}

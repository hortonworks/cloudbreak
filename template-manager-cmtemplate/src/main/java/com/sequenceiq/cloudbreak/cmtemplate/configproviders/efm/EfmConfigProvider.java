package com.sequenceiq.cloudbreak.cmtemplate.configproviders.efm;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.efm.EfmRoles.EFM;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.efm.EfmRoles.EFM_SERVER;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

@Component
public class EfmConfigProvider extends AbstractRoleConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(EfmConfigProvider.class);

    private static final String EFM_ADMIN_IDENTITIES = "efm.admin.identity";

    @Inject
    private GrpcUmsClient umsClient;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source) {
        LOGGER.info("Adding initial admin property value via EfmConfigProvider");
        List<ApiClusterTemplateConfig> configs = new ArrayList<>();

        Optional.ofNullable(source.getGeneralClusterConfigs().getCreatorWorkloadUserCrn())
            .map(this::getUserNameFromUserCrn)
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

    private String getUserNameFromUserCrn(String userCrn) {
        Crn crn = Crn.safeFromString(userCrn);
        switch (crn.getResourceType()) {
            case USER:
                return umsClient.getUserDetails(userCrn, regionAwareInternalCrnGeneratorFactory).getWorkloadUsername();
            default:
                throw new IllegalArgumentException(String.format("UserCrn %s is not of resource type USER. EFM bootstrapping requires USER creator.", userCrn));
        }
    }
}

package com.sequenceiq.cloudbreak.cmtemplate.configproviders;

import static java.util.Optional.ofNullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.util.CdhVersionProvider;

@Component
public abstract class AbstractRoleConfigProvider implements CmTemplateComponentConfigProvider {

    @Override
    public Map<String, List<ApiClusterTemplateConfig>> getRoleConfigs(CmTemplateProcessor cmTemplate, TemplatePreparationObject source) {
        Optional<ApiClusterTemplateService> service = cmTemplate.getServiceByType(getServiceType());
        if (service.isEmpty()) {
            return Map.of();
        }

        Map<String, List<ApiClusterTemplateConfig>> configs = new HashMap<>();

        for (ApiClusterTemplateRoleConfigGroup rcg : ofNullable(service.get().getRoleConfigGroups()).orElse(new ArrayList<>())) {
            String roleType = rcg.getRoleType();
            if (roleType != null && getRoleTypes().contains(roleType)) {
                configs.put(rcg.getRefName(), getRoleConfigs(roleType, cmTemplate, source));
            }
        }

        return configs;
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }

    protected abstract List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source);

    protected Optional<Integer> getCdhPatchVersion(TemplatePreparationObject source) {
        if (source.getProductDetailsView() != null && source.getProductDetailsView().getProducts() != null) {
            Optional<ClouderaManagerProduct> cdh = getCdhProduct(source);
            if (cdh.isPresent()) {
                return cdh.flatMap(p -> CdhVersionProvider.getCdhPatchVersionFromVersionString(p.getVersion()));
            }
        }
        return Optional.empty();
    }

    protected String getCdhVersion(TemplatePreparationObject source) {
        return source.getBlueprintView().getProcessor().getStackVersion() == null ? "" : source.getBlueprintView().getProcessor().getStackVersion();
    }

    protected static Optional<ClouderaManagerProduct> getCdhProduct(TemplatePreparationObject source) {
        if (source.getProductDetailsView() != null) {
            return source.getProductDetailsView().getProducts()
                    .stream()
                    .filter(p -> "CDH".equals(p.getName()))
                    .findAny();
        }
        return Optional.empty();
    }
}

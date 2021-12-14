package com.sequenceiq.cloudbreak.cmtemplate.configproviders;

import static java.util.Optional.ofNullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;

public abstract class AbstractRoleConfigProvider implements CmTemplateComponentConfigProvider {

    public static final String LOG4J2_FORMAT_MSG_NO_LOOKUPS = "-Dlog4j2.formatMsgNoLookups=True";

    private static final String PATCH_VERSION_REGEX = "\\.p([0-9]+)\\.";

    @Override
    public Map<String, List<ApiClusterTemplateConfig>> getRoleConfigs(CmTemplateProcessor cmTemplate, TemplatePreparationObject source) {
        Optional<ApiClusterTemplateService> service = cmTemplate.getServiceByType(getServiceType());
        if (service.isEmpty()) {
            return Map.of();
        }

        Map<String, List<ApiClusterTemplateConfig>> configs = new HashMap<>();

        for (ApiClusterTemplateRoleConfigGroup rcg : ofNullable(service.get().getRoleConfigGroups()).orElseGet(List::of)) {
            if (getRoleTypes().contains(rcg.getRoleType())) {
                configs.put(rcg.getRefName(), getRoleConfigs(rcg.getRoleType(), source));
            }
        }

        return configs;
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return cmTemplateProcessor.isRoleTypePresentInService(getServiceType(), getRoleTypes());
    }

    protected abstract List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, TemplatePreparationObject source);

    protected Optional<Integer> getCdhPatchVersion(TemplatePreparationObject source) {
        if (source.getProductDetailsView() != null && source.getProductDetailsView().getProducts() != null) {
            Optional<ClouderaManagerProduct> cdh = getCdhProduct(source);
            if (cdh.isPresent()) {
                return cdh.flatMap(p -> getPatchFromVersionString(p.getVersion()));
            }
        }
        return Optional.empty();
    }

    protected Optional<ClouderaManagerProduct> getCdhProduct(TemplatePreparationObject source) {
        return source.getProductDetailsView().getProducts()
                .stream()
                .filter(p -> "CDH".equals(p.getName()))
                .findAny();
    }

    protected Optional<Integer> getPatchFromVersionString(String version) {
        Matcher matcher = Pattern.compile(PATCH_VERSION_REGEX).matcher(version);
        return matcher.find() ? Optional.of(Integer.valueOf(matcher.group(1))) : Optional.empty();
    }
}

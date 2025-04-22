package com.sequenceiq.cloudbreak.cmtemplate.configproviders.meteringv2;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.clo.CLOServiceRoles.CLO_SERVER;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.clo.CLOServiceRoles.CLO_SERVICE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.dlm.DLMServiceRoles.DLM_SERVER;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.dlm.DLMServiceRoles.DLM_SERVICE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.meteringv2.MeteringV2ServiceRoles.METERINGV2_SERVER;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.meteringv2.MeteringV2ServiceRoles.METERINGV2_SERVICE;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.meteringv2.MeteringV2ServiceRoles.METERINGV2_SERVICE_REFNAME;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.meteringv2.MeteringV2ServiceRoles.METERINGV2_SERVICE_ROLE_SERVER_REF_NAME;

import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateRoleConfigGroup;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.altus.AltusDatabusConfiguration;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.views.DatabusCredentialView;
import com.sequenceiq.cloudbreak.template.views.HostgroupView;
import com.sequenceiq.common.api.type.InstanceGroupType;

@Component
public class MeteringV2ConfigProvider extends AbstractRoleConfigProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeteringV2ConfigProvider.class);

    @Inject
    private AltusDatabusConfiguration altusDatabusConfiguration;

    @Value("${meteringv2.dbus.app.name:}")
    private String dbusAppName;

    @Value("${meteringv2.dbus.stream.name:}")
    private String dbusStreamName;

    @Value("${crn.region:}")
    private String region;

    @Inject
    private DatabusCredentialProvider databusCredentialProvider;

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        try {
            DatabusCredentialView databusCredentialView = databusCredentialProvider.getOrCreateDatabusCredential(source.getCrn());
            return List.of(
                    config(MeteringV2ServiceRoles.METERINGV2_DATABUS_ACCESS_KEY_ID, databusCredentialView.getAccessKey()),
                    config(MeteringV2ServiceRoles.METERINGV2_DATABUS_ACCESS_SECRET_KEY, databusCredentialView.getPrivateKey()),
                    config(MeteringV2ServiceRoles.METERINGV2_DATABUS_ACCESS_SECRET_KEY_ALGO, databusCredentialView.getAccessKeyType()),
                    config(MeteringV2ServiceRoles.METERINGV2_DBUS_HOST, extractHost(altusDatabusConfiguration.getAltusDatabusEndpoint())),
                    config(MeteringV2ServiceRoles.METERINGV2_DBUS_STREAM, dbusStreamName),
                    config(MeteringV2ServiceRoles.METERINGV2_DBUS_APPNAME, dbusAppName),
                    config(MeteringV2ServiceRoles.METERINGV2_DBUS_PARTITION_KEY, source.getGeneralClusterConfigs().getEnvironmentCrn()),
                    config(MeteringV2ServiceRoles.METERINGV2_DBUS_REGION, region)
            );
        } catch (Exception e) {
            LOGGER.error("Could not generate config for meteringv2: ", e);
            throw new CloudbreakServiceException(e);
        }
    }

    /**
     * The host value that is passed to the metering v2 service needs to match how Liftie operates, which does
     * not have the http/https prefix. DBUS communication is also always https.
     *
     * @param url The input URL containing the DBUS host. May or may not start with http(s)
     *
     * @return The host part of the URL
     */
    protected String extractHost(String url) {
        try {
            URL dbusURL = new URI(url).toURL();
            return dbusURL.getHost();
        } catch (Exception genex) {
            // This should never happen. But if it does, just pass the value through so that it can at least be fixed manually.
            LOGGER.error("Error parsing URL {}", url, genex);
            return url;
        }
    }

    @Override
    public Map<String, ApiClusterTemplateService> getAdditionalServices(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        if (isConfigurationNeeded(cmTemplateProcessor, source)
                && cmTemplateProcessor.getServiceByType(METERINGV2_SERVICE).isEmpty()) {
            LOGGER.info("'{}' is not part of the template so adding it as an additional service with '{}' role.", METERINGV2_SERVICE, METERINGV2_SERVER);
            ApiClusterTemplateService meteringv2Settings = stubMeteringV2Settings();
            Set<HostgroupView> hostgroupViews = source.getHostgroupViews();
            return hostgroupViews.stream()
                    .filter(hg -> InstanceGroupType.GATEWAY.equals(hg.getInstanceGroupType()))
                    .collect(Collectors.toMap(HostgroupView::getName, v -> meteringv2Settings));
        }
        return Map.of();
    }

    private ApiClusterTemplateService stubMeteringV2Settings() {
        ApiClusterTemplateService coreSettings = new ApiClusterTemplateService()
                .serviceType(METERINGV2_SERVICE)
                .refName(METERINGV2_SERVICE_REFNAME);
        ApiClusterTemplateRoleConfigGroup coreSettingsRole = new ApiClusterTemplateRoleConfigGroup()
                .roleType(METERINGV2_SERVER)
                .base(true)
                .refName(METERINGV2_SERVICE_ROLE_SERVER_REF_NAME);
        coreSettings.roleConfigGroups(List.of(coreSettingsRole));
        return coreSettings;
    }

    @Override
    public String getServiceType() {
        return METERINGV2_SERVICE;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(METERINGV2_SERVER);
    }

    @Override
    public boolean isConfigurationNeeded(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        return (isSupportedForDatalake(source) || isSupportedForDatahub(cmTemplateProcessor, source))
                && StringUtils.isNotBlank(dbusAppName)
                && StringUtils.isNotBlank(dbusStreamName)
                && StringUtils.isNotBlank(altusDatabusConfiguration.getAltusDatabusEndpoint());
    }

    private boolean isSupportedForDatalake(TemplatePreparationObject source) {
        if (StackType.DATALAKE.equals(source.getStackType())) {
            String cmVersion = source.getProductDetailsView().getCm().getVersion().split("-")[0];
            String cdhVersion = source.getBlueprintView().getProcessor().getVersion().orElse("");
            return CMRepositoryVersionUtil.isDataSharingConfigurationSupported(cmVersion, cdhVersion);
        }
        return false;
    }

    private boolean isSupportedForDatahub(CmTemplateProcessor cmTemplateProcessor, TemplatePreparationObject source) {
        if (StackType.WORKLOAD.equals(source.getStackType())) {
            return cmTemplateProcessor.isRoleTypePresentInService(DLM_SERVICE, Lists.newArrayList(DLM_SERVER))
                    || cmTemplateProcessor.isRoleTypePresentInService(CLO_SERVICE, Lists.newArrayList(CLO_SERVER));
        }
        return false;
    }
}

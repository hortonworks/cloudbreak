package com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERAMANAGER_VERSION_7_4_1;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_16;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_9;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isKnoxDatabaseSupported;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isKnoxServletAsyncSupported;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils.config;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox.KnoxRoles.GATEWAY_SERVLET_ASYNC_SUPPORTED;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox.KnoxRoles.GATEWAY_SITE_SAFETY_VALVE;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.cloudera.api.swagger.model.ApiClusterTemplateConfig;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.UmsVirtualGroupRight;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupRequest;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerRepo;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.AbstractRoleConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ConfigUtils;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.ssb.SqlStreamBuilderRoles;
import com.sequenceiq.cloudbreak.dto.KerberosConfig;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.views.GatewayView;

@Component
public class KnoxGatewayConfigProvider extends AbstractRoleConfigProvider implements BaseKnoxConfigProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(KnoxGatewayConfigProvider.class);

    private static final String KNOX_MASTER_SECRET = "gateway_master_secret";

    private static final String IDBROKER_MASTER_SECRET = "idbroker_master_secret";

    private static final String GATEWAY_PATH = "gateway_path";

    private static final String GATEWAY_SIGNING_KEYSTORE_NAME = "gateway_signing_keystore_name";

    private static final String GATEWAY_SIGNING_KEYSTORE_TYPE = "gateway_signing_keystore_type";

    private static final String GATEWAY_SIGNING_KEY_ALIAS = "gateway_signing_key_alias";

    private static final String IDBROKER_SIGNING_KEYSTORE_NAME = "idbroker_gateway_signing_keystore_name";

    private static final String IDBROKER_SIGNING_KEYSTORE_TYPE = "idbroker_gateway_signing_keystore_type";

    private static final String IDBROKER_SIGNING_KEY_ALIAS = "idbroker_gateway_signing_key_alias";

    private static final String SIGNING_JKS = "signing.jks";

    private static final String SIGNING_BCFKS = "signing.bcfks";

    private static final String JKS = "JKS";

    private static final String BCFKS = "BCFKS";

    private static final String SIGNING_IDENTITY = "signing-identity";

    private static final String GATEWAY_ADMIN_GROUPS = "gateway_knox_admin_groups";

    private static final String IDBROKER_GATEWAY_ADMIN_GROUPS = "idbroker_gateway_knox_admin_groups";

    private static final String GATEWAY_WHITELIST = "gateway_dispatch_whitelist";

    private static final String GATEWAY_DEFAULT_TOPOLOGY_NAME = "gateway_default_topology_name";

    private static final String DEFAULT_TOPOLOGY = "cdp-proxy";

    private static final String GATEWAY_CM_AUTO_DISCOVERY_ENABLED = "gateway_auto_discovery_enabled";

    private static final String GATEWAY_SERVICE_TOKENSTATE_IMPL = "gateway_service_tokenstate_impl";

    private static final String GATEWAY_TOKEN_GENERATION_KNOX_TOKEN_TTL = "gateway_token_generation_knox_token_ttl";

    private static final String GATEWAY_TOKEN_GENERATION_KNOX_TOKEN_TTL_ONE_DAY = "86400000";

    private static final String GATEWAY_TOKEN_GENERATION_ENABLE_LIFESPAN_INPUT = "gateway_token_generation_enable_lifespan_input";

    private static final String GATEWAY_TOKEN_GENERATION_ENABLE_LIFESPAN_INPUT_TRUE = "true";

    private static final String GATEWAY_SECURITY_DIR = "gateway_security_dir";

    private static final String IDBROKER_SECURITY_DIR = "idbroker_security_dir";

    @Value("${cb.knox.gateway.security.dir:}")
    private String knoxGatewaySecurityDir;

    @Value("${cb.knox.idbroker.security.dir:}")
    private String knoxIdBrokerSecurityDir;

    @Inject
    private VirtualGroupService virtualGroupService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Override
    protected List<ApiClusterTemplateConfig> getRoleConfigs(String roleType, CmTemplateProcessor templateProcessor, TemplatePreparationObject source) {
        VirtualGroupRequest virtualGroupRequest = source.getVirtualGroupRequest();
        String adminGroup = virtualGroupService.createOrGetVirtualGroup(virtualGroupRequest, UmsVirtualGroupRight.KNOX_ADMIN);

        return switch (roleType) {
            case KnoxRoles.KNOX_GATEWAY -> getKnoxGatewayConfigs(source, adminGroup);
            case KnoxRoles.IDBROKER -> getIDBrokerConfigs(source, adminGroup);
            default -> List.of();
        };
    }

    private List<ApiClusterTemplateConfig> getKnoxGatewayConfigs(TemplatePreparationObject source, String adminGroup) {
        GatewayView gateway = source.getGatewayView();
        GeneralClusterConfigs generalClusterConfigs = source.getGeneralClusterConfigs();
        String masterSecret = gateway != null ? gateway.getMasterSecret() : generalClusterConfigs.getPassword();
        String topologyName = gateway != null && gateway.getExposedServices() != null ? gateway.getTopologyName() : DEFAULT_TOPOLOGY;

        List<ApiClusterTemplateConfig> config = new ArrayList<>();
        config.add(config(KNOX_MASTER_SECRET, masterSecret));
        config.add(config(GATEWAY_DEFAULT_TOPOLOGY_NAME, topologyName));
        config.add(config(GATEWAY_ADMIN_GROUPS, adminGroup));
        config.add(config(GATEWAY_CM_AUTO_DISCOVERY_ENABLED, "false"));
        if (gateway != null) {
            config.add(config(GATEWAY_PATH, gateway.getPath()));
            config.add(config(GATEWAY_SIGNING_KEYSTORE_NAME, generalClusterConfigs.isGovCloud() ? SIGNING_BCFKS : SIGNING_JKS));
            config.add(config(GATEWAY_SIGNING_KEYSTORE_TYPE, generalClusterConfigs.isGovCloud() ? BCFKS : JKS));
            config.add(config(GATEWAY_SIGNING_KEY_ALIAS, SIGNING_IDENTITY));
            config.add(getGatewayWhitelistConfig(source));
            config.addAll(getDefaultsIfRequired(source));
        }
        addTokenStateConfig(source, config);
        addSecurityConfig(source, config);
        addKnoxServletAsyncIfRequired(source, config);
        return config;
    }

    private void addSecurityConfig(TemplatePreparationObject source, List<ApiClusterTemplateConfig> config) {
        if (isSecretEncryptionSupported(source)) {
            config.add(config(GATEWAY_SECURITY_DIR, knoxGatewaySecurityDir));
        }
    }

    private void addTokenStateConfig(TemplatePreparationObject source, List<ApiClusterTemplateConfig> config) {
        if (source.getProductDetailsView() != null
                && isKnoxDatabaseSupported(source.getProductDetailsView().getCm(), getCdhProduct(source), getCdhPatchVersion(source))) {
            config.add(config(GATEWAY_SERVICE_TOKENSTATE_IMPL, "org.apache.knox.gateway.services.token.impl.JDBCTokenStateService"));
        }
    }

    private void addKnoxServletAsyncIfRequired(TemplatePreparationObject source, List<ApiClusterTemplateConfig> config) {
        Optional<ApiClusterTemplateService> serviceByType = cmTemplateProcessorFactory.get(
                source.getBlueprintView().getBlueprintText()).getServiceByType(SqlStreamBuilderRoles.SQL_STREAM_BUILDER);
        if (source.getProductDetailsView() != null
                && isKnoxServletAsyncSupported(getCdhProduct(source))
                && serviceByType.isPresent()) {
            StringBuilder gatewayServletSiteSafetyValveValue = new StringBuilder();

            gatewayServletSiteSafetyValveValue.append(ConfigUtils.getSafetyValveProperty(GATEWAY_SERVLET_ASYNC_SUPPORTED, "true"));
            config.addAll(gatewayServletSiteSafetyValveValue.toString().isEmpty() ? List.of()
                    : List.of(config(GATEWAY_SITE_SAFETY_VALVE, gatewayServletSiteSafetyValveValue.toString())));
        }
    }

    private List<ApiClusterTemplateConfig> getIDBrokerConfigs(TemplatePreparationObject source, String adminGroup) {
        List<ApiClusterTemplateConfig> idBrokerConfigs = new ArrayList<>();
        GeneralClusterConfigs generalClusterConfigs = source.getGeneralClusterConfigs();
        idBrokerConfigs.add(config(IDBROKER_MASTER_SECRET, source.getIdBroker().getMasterSecret()));
        idBrokerConfigs.add(config(IDBROKER_GATEWAY_ADMIN_GROUPS, adminGroup));
        idBrokerConfigs.add(config(IDBROKER_SIGNING_KEYSTORE_NAME, generalClusterConfigs.isGovCloud() ? SIGNING_BCFKS : SIGNING_JKS));
        idBrokerConfigs.add(config(IDBROKER_SIGNING_KEYSTORE_TYPE, generalClusterConfigs.isGovCloud() ? BCFKS : JKS));
        idBrokerConfigs.add(config(IDBROKER_SIGNING_KEY_ALIAS, SIGNING_IDENTITY));
        if (isSecretEncryptionSupported(source)) {
            idBrokerConfigs.add(config(IDBROKER_SECURITY_DIR, knoxIdBrokerSecurityDir));
        }
        return idBrokerConfigs;
    }

    @Override
    public List<String> getRoleTypes() {
        return List.of(KnoxRoles.KNOX_GATEWAY, KnoxRoles.IDBROKER);
    }

    @VisibleForTesting
    ApiClusterTemplateConfig getGatewayWhitelistConfig(TemplatePreparationObject source) {
        String whitelist;
        Optional<KerberosConfig> kerberosConfig = source.getKerberosConfig();
        if (kerberosConfig.isPresent()) {
            String domain = kerberosConfig.get().getDomain();
            if (source.getGeneralClusterConfigs().getAutoTlsEnabled()) {
                // HTTPS only whitelist when AutoTLS enabled
                whitelist = "^/.*$;^https://([^/]+\\." + domain + "):[0-9]+/?.*$";
            } else {
                // HTTP or HTTPS whitelist when AutoTLS disabled
                whitelist = "^/.*$;^https?://([^/]+\\." + domain + "):[0-9]+/?.*$";
            }
        } else {
            // Allow all when Kerberos isn't used
            whitelist = "^*.*$";
        }
        return config(GATEWAY_WHITELIST, whitelist);
    }

    private Set<ApiClusterTemplateConfig> getDefaultsIfRequired(TemplatePreparationObject source) {
        Set<ApiClusterTemplateConfig> apiClusterTemplateConfigs = new HashSet<>();
        Optional<ClouderaManagerProduct> cdhProduct = getCdhProduct(source);
        if (cdhProduct.isPresent() && source.getProductDetailsView() != null) {
            String cdhVersion = cdhProduct.get().getVersion().split("-")[0];
            ClouderaManagerRepo clouderaManagerRepoDetails = source.getProductDetailsView().getCm();
            if (tokenServiceSupported(cdhVersion, clouderaManagerRepoDetails)) {
                Optional<String> accountId = source.getGeneralClusterConfigs().getAccountId();
                if (accountId.isPresent() && !entitlementService.isOjdbcTokenDhOneHour(accountId.get())) {
                    apiClusterTemplateConfigs.add(
                            config(GATEWAY_TOKEN_GENERATION_KNOX_TOKEN_TTL,
                                    GATEWAY_TOKEN_GENERATION_KNOX_TOKEN_TTL_ONE_DAY));
                    apiClusterTemplateConfigs.add(
                            config(GATEWAY_TOKEN_GENERATION_ENABLE_LIFESPAN_INPUT,
                                    GATEWAY_TOKEN_GENERATION_ENABLE_LIFESPAN_INPUT_TRUE));
                }
            }
        }
        return apiClusterTemplateConfigs;
    }

    private boolean tokenServiceSupported(String cdhVersion, ClouderaManagerRepo clouderaManagerRepoDetails) {
        if (isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERA_STACK_VERSION_7_2_9)) {
            if (isVersionNewerOrEqualThanLimited(clouderaManagerRepoDetails::getVersion, CLOUDERAMANAGER_VERSION_7_4_1)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSecretEncryptionSupported(TemplatePreparationObject source) {
        String cdhVersion = ConfigUtils.getCdhVersion(source);
        if (source.isEnableSecretEncryption() && isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERA_STACK_VERSION_7_2_16)) {
            LOGGER.info("Secret Encryption is supported");
            return true;
        }
        return false;
    }
}

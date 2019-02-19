package com.sequenceiq.cloudbreak.clusterdefinition.kerberos;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.template.ClusterDefinitionComponentConfigProvider;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.template.ClusterDefinitionProcessingException;
import com.sequenceiq.cloudbreak.template.TemplatePreparationObject;
import com.sequenceiq.cloudbreak.template.processor.AmbariBlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.processor.configuration.SiteConfigurations;
import com.sequenceiq.cloudbreak.type.KerberosType;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@Service
public class KerberosClusterDefinitionService implements ClusterDefinitionComponentConfigProvider {

    private static final String REALM = "NODE.DC1.CONSUL";

    private static final String DOMAIN = "node.dc1.consul";

    private static final Integer KERBEROS_DB_PROPAGATION_PORT = 6318;

    @Inject
    private KerberosDetailService kerberosDetailService;

    @Override
    public AmbariBlueprintTextProcessor customTextManipulation(TemplatePreparationObject source, AmbariBlueprintTextProcessor blueprintProcessor) {
        KerberosConfig kerberosConfig = source.getKerberosConfig().orElse(null);
        if (source.getGeneralClusterConfigs().getInstanceGroupsPresented()) {
            Integer propagationPort = source.getGeneralClusterConfigs().isGatewayInstanceMetadataPresented() ? KERBEROS_DB_PROPAGATION_PORT : null;
            String gatewayHost = source.getGeneralClusterConfigs().getPrimaryGatewayInstanceDiscoveryFQDN().orElse(null);
            String domain = gatewayHost.substring(gatewayHost.indexOf('.') + 1);
            extendBlueprintWithKerberos(blueprintProcessor, kerberosConfig, gatewayHost, domain, propagationPort);
            if (StringUtils.hasLength(kerberosConfig.getDescriptor())) {
                blueprintProcessor.replaceConfiguration("kerberos-env", kerberosConfig.getDescriptor());
            }
            if (StringUtils.hasLength(kerberosConfig.getKrb5Conf())) {
                blueprintProcessor.replaceConfiguration("krb5-conf", kerberosConfig.getKrb5Conf());
            }
        } else {
            extendBlueprintWithKerberos(blueprintProcessor, kerberosConfig, source.getGeneralClusterConfigs().getAmbariIp(), REALM, DOMAIN, null);
        }
        return blueprintProcessor;
    }

    private AmbariBlueprintTextProcessor extendBlueprintWithKerberos(AmbariBlueprintTextProcessor blueprintText, KerberosConfig kerberosConfig,
            String gatewayHost, String domain, Integer propagationPort) {
        return extendBlueprintWithKerberos(blueprintText, kerberosConfig, gatewayHost, kerberosDetailService.getRealm(domain, kerberosConfig),
                domain, propagationPort);

    }

    private AmbariBlueprintTextProcessor extendBlueprintWithKerberos(AmbariBlueprintTextProcessor blueprintText, KerberosConfig kerberosConfig,
            String gatewayHost, String realm, String domain, Integer propagationPort) {
        String kdcHosts = kerberosDetailService.resolveHostForKerberos(kerberosConfig, gatewayHost);
        String kdcType = kerberosDetailService.resolveTypeForKerberos(kerberosConfig);
        String kdcAdminHost = kerberosDetailService.resolveHostForKdcAdmin(kerberosConfig, kdcHosts);
        String ldapUrl = kerberosDetailService.resolveLdapUrlForKerberos(kerberosConfig);
        String containerDn = kerberosDetailService.resolveContainerDnForKerberos(kerberosConfig);

        ImmutableMap.Builder<String, String> builder = ImmutableMap.<String, String>builder()
                .put("realm", realm)
                .put("kdc_type", kdcType)
                .put("kdc_hosts", kdcHosts)
                .put("admin_server_host", kdcAdminHost)
                .put("encryption_types", "aes des3-cbc-sha1 rc4 des-cbc-md5")
                .put("include_all_components_in_auth_to_local_rules", "true")
                .put("ldap_url", ldapUrl == null ? "" : ldapUrl)
                .put("container_dn", containerDn == null ? "" : containerDn);
        if (kerberosConfig.getType() == KerberosType.FREEIPA) {
            builder.put("case_insensitive_username_rules", "true");
        }
        Map<String, String> kerberosEnv = builder.build();
        return extendBlueprintWithKerberos(blueprintText, kerberosEnv, kerberosDetailService.getDomains(domain),
                !kerberosConfig.isTcpAllowed(), propagationPort, kerberosConfig.getType(), false);
    }

    public AmbariBlueprintTextProcessor extendBlueprintWithKerberos(AmbariBlueprintTextProcessor blueprint, Map<String, String> kerberosEnv, String domains,
            Boolean useUdp, Integer kpropPort, KerberosType kerberosType,
            boolean forced) {
        try {
            String krb5Config = FileReaderUtils.readFileFromClasspath("kerberos/krb5-conf-template.conf");
            krb5Config = krb5Config.replaceAll("udp_preference_limit_content", useUdp ? "0" : "1");
            if (kpropPort != null) {
                krb5Config = krb5Config.replaceAll("iprop_enable_content", "true");
                krb5Config = krb5Config.replaceAll("iprop_port_content", kpropPort.toString());
            } else {
                krb5Config = krb5Config.replaceAll("iprop_enable_content", "false");
                krb5Config = krb5Config.replaceAll("iprop_port_content", "8888");
            }
            SiteConfigurations configs = SiteConfigurations.getEmptyConfiguration();
            Map<String, String> krb5Conf = new HashMap<>();
            krb5Conf.put("domains", domains);
            if (kerberosType != KerberosType.ACTIVE_DIRECTORY && kerberosType != KerberosType.FREEIPA) {
                krb5Conf.put("manage_krb5_conf", "true");
            } else {
                krb5Conf.put("manage_krb5_conf", "false");
            }
            if (!useUdp || kpropPort != null) {
                krb5Conf.put("content", krb5Config.toString());
            }
            configs.addSiteConfiguration("kerberos-env", kerberosEnv);
            configs.addSiteConfiguration("krb5-conf", krb5Conf);

            return blueprint.addComponentToHostgroups("KERBEROS_CLIENT", hg -> true)
                    .setSecurityType("KERBEROS")
                    .extendBlueprintGlobalConfiguration(configs, forced);
        } catch (IOException e) {
            throw new ClusterDefinitionProcessingException("Failed to extend cluster definition with kerberos configurations.", e);
        }
    }

    @Override
    public boolean specialCondition(TemplatePreparationObject source, String clusterDefinitionText) {
        return source.getKerberosConfig().isPresent();
    }
}

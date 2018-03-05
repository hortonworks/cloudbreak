package com.sequenceiq.cloudbreak.blueprint.kerberos;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.ImmutableMap;
import com.sequenceiq.ambari.client.services.KerberosService;
import com.sequenceiq.cloudbreak.blueprint.BlueprintComponentConfigProvider;
import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessingException;
import com.sequenceiq.cloudbreak.blueprint.BlueprintProcessor;
import com.sequenceiq.cloudbreak.blueprint.configuration.SiteConfigurations;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Service
public class KerberosBlueprintService implements BlueprintComponentConfigProvider {

    private static final String REALM = "NODE.DC1.CONSUL";

    private static final String DOMAIN = "node.dc1.consul";

    private static final Integer KERBEROS_DB_PROPAGATION_PORT = 6318;

    @Inject
    private BlueprintProcessor blueprintProcessor;

    @Inject
    private KerberosDetailService kerberosDetailService;

    @Override
    public String customTextManipulation(BlueprintPreparationObject source, String blueprintText) {
        Cluster cluster = source.getCluster();
        if (source.getStack().getInstanceGroups() != null && !source.getInstanceGroups().isEmpty()) {
            KerberosConfig kerberosConfig = source.getCluster().getKerberosConfig();
            Integer propagationPort = source.getStack().getGatewayInstanceMetadata().size() > 1 ? KERBEROS_DB_PROPAGATION_PORT : null;
            String gatewayHost = source.getPrimaryGatewayInstance().getDiscoveryFQDN();
            String domain = gatewayHost.substring(gatewayHost.indexOf('.') + 1);
            blueprintText = extendBlueprintWithKerberos(blueprintText, cluster, gatewayHost, domain, propagationPort,
                    source.getAmbariClient());
            if (StringUtils.hasLength(kerberosConfig.getDescriptor())) {
                blueprintText = replaceConfiguration(blueprintText, "kerberos-env", kerberosConfig.getDescriptor());
            }
            if (StringUtils.hasLength(kerberosConfig.getKrb5Conf())) {
                blueprintText = replaceConfiguration(blueprintText, "krb5-conf", kerberosConfig.getKrb5Conf());
            }
        } else {
            // TODO this won't work on yarn, but it doesn't work anyway
            blueprintText = extendBlueprintWithKerberos(blueprintText, cluster, cluster.getAmbariIp(), REALM, DOMAIN, null,
                    source.getAmbariClient());
        }
        return blueprintText;
    }

    private String extendBlueprintWithKerberos(String blueprintText, Cluster cluster, String gatewayHost, String domain, Integer propagationPort,
            KerberosService kerberosService) {
        KerberosConfig kerberosConfig = cluster.getKerberosConfig();
        return extendBlueprintWithKerberos(blueprintText, cluster, gatewayHost, kerberosDetailService.getRealm(domain, kerberosConfig), domain, propagationPort,
                kerberosService);

    }

    public String extendBlueprintWithKerberos(String blueprint, String kdcType, String kdcHosts, String kdcAdminHost, String realm, String domains,
            String ldapUrl, String containerDn, Boolean useUdp, Integer kpropPort, boolean forced) {
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
            Map<String, String> kerberosEnv = ImmutableMap.<String, String>builder()
                    .put("realm", realm)
                    .put("kdc_type", kdcType)
                    .put("kdc_hosts", kdcHosts)
                    .put("admin_server_host", kdcAdminHost)
                    .put("encryption_types", "aes des3-cbc-sha1 rc4 des-cbc-md5")
                    .put("ldap_url", ldapUrl == null ? "" : ldapUrl)
                    .put("container_dn", containerDn == null ? "" : containerDn)
                    .build();
            Map<String, String> krb5Conf = new HashMap<>();
            krb5Conf.put("domains", domains);
            krb5Conf.put("manage_krb5_conf", "true");
            if (!useUdp || kpropPort != null) {
                krb5Conf.put("content", krb5Config.toString());
            }
            configs.addSiteConfiguration("kerberos-env", kerberosEnv);
            configs.addSiteConfiguration("krb5-conf", krb5Conf);

            String blueprintText = blueprintProcessor.extendBlueprintGlobalConfiguration(blueprint, configs, forced);
            blueprintText = blueprintProcessor.addComponentToHostgroups(blueprintText, "KERBEROS_CLIENT", hg -> true);
            blueprintText = blueprintProcessor.setSecurityType(blueprintText, "KERBEROS");
            return blueprintText;
        } catch (IOException e) {
            throw new BlueprintProcessingException("Failed to extend blueprint with kerberos configurations.", e);
        }

    }

    private String extendBlueprintWithKerberos(String blueprintText, Cluster cluster, String gatewayHost, String realm, String domain, Integer propagationPort,
            KerberosService kerberosService) {
        KerberosConfig kerberosConfig = cluster.getKerberosConfig();
        String kdcHosts = kerberosDetailService.resolveHostForKerberos(cluster, gatewayHost);
        blueprintText = extendBlueprintWithKerberos(blueprintText,
                kerberosDetailService.resolveTypeForKerberos(kerberosConfig),
                kdcHosts,
                kerberosDetailService.resolveHostForKdcAdmin(cluster, kdcHosts),
                realm,
                kerberosDetailService.getDomains(domain),
                kerberosDetailService.resolveLdapUrlForKerberos(kerberosConfig),
                kerberosDetailService.resolveContainerDnForKerberos(kerberosConfig),
                !kerberosConfig.getTcpAllowed(), propagationPort, false);
        return blueprintText;
    }

    private String replaceConfiguration(String blueprintText, String key, String configuration) {
        try {
            JsonNode blueprint = JsonUtil.readTree(blueprintText);
            ArrayNode configurations = (ArrayNode) blueprint.get("configurations");
            Iterator<JsonNode> elements = configurations.elements();
            while (elements.hasNext()) {
                if (elements.next().get(key) != null) {
                    elements.remove();
                    break;
                }
            }
            configurations.add(JsonUtil.readTree(configuration));
            return JsonUtil.writeValueAsString(blueprint);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean additionalCriteria(BlueprintPreparationObject source, String blueprintText) {
        return source.getCluster().isSecure();
    }
}

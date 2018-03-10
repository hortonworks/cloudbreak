package com.sequenceiq.cloudbreak.blueprint.kerberos;

import java.io.IOException;
import java.util.Iterator;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sequenceiq.ambari.client.services.KerberosService;
import com.sequenceiq.cloudbreak.blueprint.BlueprintComponentConfigProvider;
import com.sequenceiq.cloudbreak.blueprint.BlueprintPreparationObject;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Service
public class KerberosBlueprintService implements BlueprintComponentConfigProvider {

    private static final String REALM = "NODE.DC1.CONSUL";

    private static final String DOMAIN = "node.dc1.consul";

    private static final Integer KERBEROS_DB_PROPAGATION_PORT = 6318;

    @Inject
    private KerberosDetailService kerberosDetailService;

    @Override
    public String configure(BlueprintPreparationObject source, String blueprintText) {
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

    private String extendBlueprintWithKerberos(String blueprintText, Cluster cluster, String gatewayHost, String realm, String domain, Integer propagationPort,
            KerberosService kerberosService) {
        KerberosConfig kerberosConfig = cluster.getKerberosConfig();
        String kdcHosts = kerberosDetailService.resolveHostForKerberos(cluster, gatewayHost);
        blueprintText = kerberosService.extendBlueprintWithKerberos(blueprintText,
                kerberosDetailService.resolveTypeForKerberos(kerberosConfig),
                kdcHosts,
                kerberosDetailService.resolveHostForKdcAdmin(cluster, kdcHosts),
                realm,
                kerberosDetailService.getDomains(domain),
                kerberosDetailService.resolveLdapUrlForKerberos(kerberosConfig),
                kerberosDetailService.resolveContainerDnForKerberos(kerberosConfig),
                !kerberosConfig.getTcpAllowed(), propagationPort);
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

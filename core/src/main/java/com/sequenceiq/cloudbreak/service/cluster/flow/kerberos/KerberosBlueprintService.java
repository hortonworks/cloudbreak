package com.sequenceiq.cloudbreak.service.cluster.flow.kerberos;

import java.io.IOException;
import java.util.Iterator;

import javax.inject.Inject;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sequenceiq.ambari.client.services.KerberosService;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.util.JsonUtil;

@Service
public class KerberosBlueprintService {

    private static final String REALM = "NODE.DC1.CONSUL";

    private static final String DOMAIN = "node.dc1.consul";

    private static final Integer KERBEROS_DB_PROPAGATION_PORT = 6318;

    @Inject
    private KerberosDetailService kerberosDetailService;

    public String extendBlueprintWithKerberos(Stack stack, String blueprintText, KerberosService kerberosService) {
        Cluster cluster = stack.getCluster();
        if (stack.getInstanceGroups() != null && !stack.getInstanceGroups().isEmpty()) {
            KerberosConfig kerberosConfig = stack.getCluster().getKerberosConfig();
            Integer propagationPort = stack.getGatewayInstanceMetadata().size() > 1 ? KERBEROS_DB_PROPAGATION_PORT : null;
            String gatewayHost = stack.getPrimaryGatewayInstance().getDiscoveryFQDN();
            String domain = gatewayHost.substring(gatewayHost.indexOf('.') + 1);
            blueprintText = extendBlueprintWithKerberos(blueprintText, cluster, gatewayHost, domain, propagationPort, kerberosService);
            if (StringUtils.hasLength(kerberosConfig.getKerberosDescriptor())) {
                blueprintText = replaceConfiguratin(blueprintText, "kerberos-env", kerberosConfig.getKerberosDescriptor());
            }
            if (StringUtils.hasLength(kerberosConfig.getKrb5Conf())) {
                blueprintText = replaceConfiguratin(blueprintText, "krb5-conf", kerberosConfig.getKrb5Conf());
            }
        } else {
            // TODO this won't work on yarn, but it doesn't work anyway
            blueprintText = extendBlueprintWithKerberos(blueprintText, cluster, cluster.getAmbariIp(), REALM, DOMAIN, null, kerberosService);
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
        blueprintText = kerberosService.extendBlueprintWithKerberos(blueprintText,
                kerberosDetailService.resolveTypeForKerberos(kerberosConfig),
                kerberosDetailService.resolveHostForKerberos(cluster, gatewayHost),
                realm,
                kerberosDetailService.getDomains(domain),
                kerberosDetailService.resolveLdapUrlForKerberos(kerberosConfig),
                kerberosDetailService.resolveContainerDnForKerberos(kerberosConfig),
                !kerberosConfig.getKerberosTcpAllowed(), propagationPort);
        return blueprintText;
    }

    private String replaceConfiguratin(String blueprintText, String key, String configuration) {
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
}

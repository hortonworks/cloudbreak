package com.sequenceiq.cloudbreak.recipe.util;

import static com.sequenceiq.cloudbreak.TestUtil.ldapConfig;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ExecutorType;
import com.sequenceiq.cloudbreak.cloud.model.AmbariDatabase;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;
import com.sequenceiq.cloudbreak.template.views.LdapView;

public class RecipeTestUtil {

    private static final String IDENTITY_USER_EMAIL = "identity.user@email.com";

    private RecipeTestUtil() {
    }

    public static GeneralClusterConfigs generalClusterConfigs() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setAmbariIp("10.1.1.1");
        generalClusterConfigs.setInstanceGroupsPresented(true);
        generalClusterConfigs.setGatewayInstanceMetadataPresented(false);
        generalClusterConfigs.setClusterName("clustername");
        generalClusterConfigs.setExecutorType(ExecutorType.DEFAULT);
        generalClusterConfigs.setStackName("clustername");
        generalClusterConfigs.setUuid("111-222-333-444");
        generalClusterConfigs.setUserName("username");
        generalClusterConfigs.setPassword("Passw0rd");
        generalClusterConfigs.setNodeCount(1);
        generalClusterConfigs.setPrimaryGatewayInstanceDiscoveryFQDN(Optional.of("fqdn.loal.com"));
        generalClusterConfigs.setIdentityUserEmail(IDENTITY_USER_EMAIL);
        return generalClusterConfigs;
    }

    public static GeneralClusterConfigs generalClusterConfigs(Cluster cluster) {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setAmbariIp(cluster.getAmbariIp());
        generalClusterConfigs.setInstanceGroupsPresented(true);
        generalClusterConfigs.setGatewayInstanceMetadataPresented(true);
        generalClusterConfigs.setClusterName(cluster.getName());
        generalClusterConfigs.setExecutorType(cluster.getExecutorType());
        generalClusterConfigs.setStackName(cluster.getName());
        generalClusterConfigs.setUuid("111-222-333-444");
        generalClusterConfigs.setUserName(cluster.getUserName());
        generalClusterConfigs.setPassword(cluster.getPassword());
        generalClusterConfigs.setNodeCount(1);
        generalClusterConfigs.setIdentityUserEmail(IDENTITY_USER_EMAIL);
        generalClusterConfigs.setPrimaryGatewayInstanceDiscoveryFQDN(Optional.of("fqdn.loal.com"));
        return generalClusterConfigs;
    }

    public static BlueprintView generalBlueprintView(String blueprintText, String version, String type) {
        return new BlueprintView(blueprintText, version, type, mock(BlueprintTextProcessor.class));
    }

    public static AmbariDatabase generalAmbariDatabase() {
        return new AmbariDatabase("cloudbreak", "fancy ambari db name", "ambariDB", "10.1.1.2", 5432,
                "Ambar!UserName", "Ambar!Passw0rd");
    }

    public static LdapView generalLdapView() {
        return new LdapView(ldapConfig(), "cn=admin,dc=example,dc=org", "admin");
    }
}

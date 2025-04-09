package com.sequenceiq.cloudbreak.recipe.util;

import static org.mockito.Mockito.mock;

import java.util.Optional;

import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.template.model.GeneralClusterConfigs;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.template.views.BlueprintView;

public class RecipeTestUtil {

    private static final String IDENTITY_USER_EMAIL = "identity.user@email.com";

    private RecipeTestUtil() {
    }

    public static GeneralClusterConfigs generalClusterConfigs() {
        GeneralClusterConfigs generalClusterConfigs = new GeneralClusterConfigs();
        generalClusterConfigs.setClusterManagerIp("10.1.1.1");
        generalClusterConfigs.setInstanceGroupsPresented(true);
        generalClusterConfigs.setGatewayInstanceMetadataPresented(false);
        generalClusterConfigs.setClusterName("clustername");
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
        generalClusterConfigs.setClusterManagerIp(cluster.getClusterManagerIp());
        generalClusterConfigs.setInstanceGroupsPresented(true);
        generalClusterConfigs.setGatewayInstanceMetadataPresented(true);
        generalClusterConfigs.setClusterName(cluster.getName());
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
        return new BlueprintView(blueprintText, version, type, null, mock(BlueprintTextProcessor.class));
    }
}

package com.sequenceiq.it.cloudbreak.newway.v4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.clusterdefinition.ClusterDefinition;
import com.sequenceiq.it.cloudbreak.newway.entity.clusterdefinition.ClusterDefinitionEntity;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.Entity;
import com.sequenceiq.it.cloudbreak.newway.RecommendationEntity;
import com.sequenceiq.it.cloudbreak.newway.Region;
import com.sequenceiq.it.cloudbreak.newway.log.Log;

public class RecommendationV4Action {

    private RecommendationV4Action() {
    }

    public static void post(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        RecommendationEntity recommendationEntity = (RecommendationEntity) entity;
        CloudbreakClient client = integrationTestContext.getContextParam(CloudbreakClient.CLOUDBREAK_CLIENT, CloudbreakClient.class);
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);

        ClusterDefinition clusterDefinition = integrationTestContext.getContextParam(ClusterDefinitionEntity.CLUSTER_DEFINITION, ClusterDefinition.class);
        if (clusterDefinition != null && clusterDefinition.getResponse() != null) {
            recommendationEntity.withAvailabilityZone(clusterDefinition.getResponse().getName());
        }

        Credential credential = Credential.getTestContextCredential().apply(integrationTestContext);
        if (credential != null) {
            recommendationEntity.withCredentialName(credential.getResponse().getName());
        }

        Region region = Region.getTestContextRegion().apply(integrationTestContext);
        if (region != null) {
            recommendationEntity.withRegion(region.getRegionV4Response().getDefaultRegion());
        }
        if (region != null) {
            Map<String, Collection<String>> regionsAvailabilityZones = region.getRegionV4Response().getAvailabilityZones();
            List<String> availabilityZones = new ArrayList<>();

            for (Entry<String, Collection<String>> regionAvailabilityZones : regionsAvailabilityZones.entrySet()) {
                availabilityZones.addAll(regionAvailabilityZones.getValue());
            }
            recommendationEntity.withAvailabilityZone(availabilityZones.get(1));
        }

        Log.log(String.join(" ", " post Recommendations to",
                recommendationEntity.getCredentialName(), "credential and to",
                recommendationEntity.getClusterDefinitionName(), "cluster definition",
                recommendationEntity.getRegion(), "region",
                recommendationEntity.getAvailabilityZone(), "availability zone. "));
        recommendationEntity.setResponse(
                client.getCloudbreakClient()
                        .clusterDefinitionV4Endpoint()
                        .createRecommendation(workspaceId, recommendationEntity.getClusterDefinitionName(), recommendationEntity.getCredentialName(),
                                recommendationEntity.getRegion(), null, recommendationEntity.getAvailabilityZone()));
        Log.logJSON(" post Recommendations response: ", recommendationEntity.getResponse());
    }

    public static void createInGiven(IntegrationTestContext integrationTestContext, Entity entity) throws IOException {
        post(integrationTestContext, entity);
    }
}

package com.sequenceiq.cloudbreak.service.cluster.flow.recipe;

import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class OrchestratorRecipeExecutorTest {

    @InjectMocks
    private OrchestratorRecipeExecutor orchestratorRecipeExecutor;

    @Mock
    private RecipeExecutionFailureCollector recipeExecutionFailureCollector;

    @Test
    public void testGetSingleRecipeExecutionFailureMessageWithInstanceMetaData() {
        final InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("fqdn");
        final InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("instance-group");
        instanceMetaData.setInstanceGroup(instanceGroup);
        when(recipeExecutionFailureCollector.getInstanceMetadataByHost(anySet(), anyString())).thenReturn(Optional.of(instanceMetaData));

        final RecipeExecutionFailureCollector.RecipeFailure recipeFailure = new RecipeExecutionFailureCollector.RecipeFailure("fqdn", "phase", "recipe");
        final String message = orchestratorRecipeExecutor.getSingleRecipeExecutionFailureMessage(Set.of(instanceMetaData), recipeFailure);

        Assert.assertEquals("[Recipe: 'recipe' - \nHostgroup: 'instance-group' - \nInstance: 'fqdn']", message);
    }

    @Test
    public void testGetSingleRecipeExecutionFailureMessageWithoutInstanceMetaData() {
        final InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setDiscoveryFQDN("other-fqdn");
        final InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("instance-group");
        instanceMetaData.setInstanceGroup(instanceGroup);
        when(recipeExecutionFailureCollector.getInstanceMetadataByHost(anySet(), anyString())).thenReturn(Optional.empty());

        final RecipeExecutionFailureCollector.RecipeFailure recipeFailure = new RecipeExecutionFailureCollector.RecipeFailure("fqdn", "phase", "recipe");
        final String message = orchestratorRecipeExecutor.getSingleRecipeExecutionFailureMessage(Set.of(instanceMetaData), recipeFailure);

        Assert.assertEquals("[Recipe: 'recipe' - \nInstance: 'fqdn' (missing metadata)]", message);
    }
}

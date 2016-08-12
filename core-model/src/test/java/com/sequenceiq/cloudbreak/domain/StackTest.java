package com.sequenceiq.cloudbreak.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.domain.json.JsonToString;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class StackTest {

    @Test
    public void infrastructureShouldNotBeStoppableForEphemeralStorage() {
        Stack stack = new Stack();
        stack.setCloudPlatform("AWS");
        Set<InstanceGroup> groups = new HashSet<>();
        groups.add(createGroup("ebs"));
        groups.add(createGroup("ephemeral"));
        stack.setInstanceGroups(groups);
        StopRestrictionReason infrastructureStoppable = stack.isInfrastructureStoppable();
        assertEquals(StopRestrictionReason.EPHEMERAL_VOLUMES, infrastructureStoppable);
    }

    @Test
    public void infrastructureShouldNotBeStoppableForSpotInstances() throws JsonProcessingException {
        Stack stack = new Stack();
        stack.setCloudPlatform("AWS");
        Set<InstanceGroup> groups = new HashSet<>();
        groups.add(createGroup("ebs"));
        InstanceGroup master = createGroup("ebs");
        master.getTemplate().setAttributes(
                new JsonToString().convertToEntityAttribute(
                        "{\"sshLocation\":\"0.0.0.0/0\",\"encrypted\":false,\"spotPrice\":0.04}"));
        groups.add(master);
        stack.setInstanceGroups(groups);
        StopRestrictionReason infrastructureStoppable = stack.isInfrastructureStoppable();
        assertEquals(StopRestrictionReason.SPOT_INSTANCES, infrastructureStoppable);
    }

    @Test
    public void infrastructureShouldBeStoppableForNonAWSClusters() {
        Stack stack = new Stack();
        stack.setCloudPlatform("GCP");
        StopRestrictionReason infrastructureStoppable = stack.isInfrastructureStoppable();
        assertEquals(StopRestrictionReason.NONE, infrastructureStoppable);
    }

    @Test
    public void infrastructureShouldBeStoppableForValidInstanceGroups() {
        Stack stack = new Stack();
        stack.setCloudPlatform("AWS");
        Set<InstanceGroup> groups = new HashSet<>();
        groups.add(createGroup("ebs"));
        InstanceGroup master = createGroup("ebs");
        master.getTemplate().setAttributes(
                new JsonToString().convertToEntityAttribute(
                        "{\"sshLocation\":\"0.0.0.0/0\",\"encrypted\":false}"));
        groups.add(master);
        stack.setInstanceGroups(groups);
        StopRestrictionReason infrastructureStoppable = stack.isInfrastructureStoppable();
        assertEquals(StopRestrictionReason.NONE, infrastructureStoppable);
    }

    private InstanceGroup createGroup(String volumeType) {
        InstanceGroup group = new InstanceGroup();
        Template ephemeralTemplate = new Template();
        ephemeralTemplate.setVolumeType(volumeType);
        group.setTemplate(ephemeralTemplate);
        return group;
    }
}

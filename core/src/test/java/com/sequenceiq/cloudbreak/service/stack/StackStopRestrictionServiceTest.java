package com.sequenceiq.cloudbreak.service.stack;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.powermock.reflect.Whitebox;

import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.model.CloudbreakDetails;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.StopRestrictionReason;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.stack.StackStopRestrictionConfiguration.ServiceRoleGroup;
import com.sequenceiq.cloudbreak.service.stack.StackStopRestrictionConfiguration.ServiceRoleGroup.ServiceRole;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.common.model.AwsDiskType;

@ExtendWith(MockitoExtension.class)
public class StackStopRestrictionServiceTest {

    private final TemporaryStorage temporaryStorage = TemporaryStorage.ATTACHED_VOLUMES;

    @Mock
    private ComponentConfigProviderService componentConfigProviderService;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private ClusterComponentConfigProvider clusterComponentProvider;

    @Mock
    private CmTemplateProcessor cmTemplateProcessor;

    @InjectMocks
    private StackStopRestrictionService underTest;

    @BeforeEach
    public void setUp() {
        StackStopRestrictionConfiguration config = new StackStopRestrictionConfiguration();
        config.setRestrictedCloudPlatform("AWS");
        config.setEphemeralCachingMinVersion("2.48.0");
        config.setEphemeralOnlyMinVersion("2.53.0");
        ServiceRoleGroup serviceRoleGroup = new ServiceRoleGroup();
        serviceRoleGroup.setName("general");
        ServiceRole sr1 = new ServiceRole();
        sr1.setService("YARN");
        sr1.setRole("NODEMANAGER");
        sr1.setRequired(false);
        Set<ServiceRole> srSet = new HashSet<>();
        srSet.add(sr1);
        serviceRoleGroup.setServiceRoles(srSet);
        ServiceRole r1 = new ServiceRole();
        r1.setRole("GATEWAY");
        r1.setRequired(false);
        Set<ServiceRole> rSet = new HashSet<>();
        rSet.add(r1);
        serviceRoleGroup.setRoles(rSet);
        List<ServiceRoleGroup> srgList = new ArrayList<>();
        srgList.add(serviceRoleGroup);
        config.setPermittedServiceRoleGroups(srgList);

        Whitebox.setInternalState(underTest, "config", config);
        Whitebox.setInternalState(underTest, "ephemeralVolumeChecker", new InstanceGroupEphemeralVolumeChecker());
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void infrastructureShouldNotBeStoppableForEphemeralOnlyBefore253CbVersionAndSaltCbVersion() {
        Set<InstanceGroup> groups = new HashSet<>();
        groups.add(createGroup(List.of("ebs"), temporaryStorage, "master"));
        groups.add(createGroup(List.of(AwsDiskType.Ephemeral.value()), temporaryStorage, "worker"));

        when(componentConfigProviderService.getCloudbreakDetails(any())).thenReturn(new CloudbreakDetails("2.48.0-XXb"));
        when(clusterComponentProvider.getSaltStateComponentCbVersion(any())).thenReturn(null);

        StopRestrictionReason actual = underTest.isInfrastructureStoppable(createStack("AWS", groups));

        Assertions.assertEquals(StopRestrictionReason.EPHEMERAL_VOLUMES, actual);
    }

    @Test
    public void stoppableWhenEphemeralOnlyAndAfter253CbVersion() {
        Set<InstanceGroup> groups = new HashSet<>();
        groups.add(createGroup(List.of("ebs"), temporaryStorage, "master"));
        groups.add(createGroup(List.of(AwsDiskType.Ephemeral.value()), temporaryStorage, "worker"));

        when(componentConfigProviderService.getCloudbreakDetails(any())).thenReturn(new CloudbreakDetails("2.53.0-XXb"));

        createServiceComponentMap(Set.of(
                ServiceComponent.of("YARN", "NODEMANAGER"),
                ServiceComponent.of("HDFS", "GATEWAY"),
                ServiceComponent.of("YARN", "GATEWAY")));

        StopRestrictionReason actual = underTest.isInfrastructureStoppable(createStack("AWS", groups));

        Assertions.assertEquals(StopRestrictionReason.NONE, actual);
    }

    @Test
    public void stoppableWhenEphemeralOnlyAndBefore253CbVersionAndAfter253SaltCbVersion() {
        Set<InstanceGroup> groups = new HashSet<>();
        groups.add(createGroup(List.of("ebs"), temporaryStorage, "master"));
        groups.add(createGroup(List.of(AwsDiskType.Ephemeral.value()), temporaryStorage, "worker"));

        when(componentConfigProviderService.getCloudbreakDetails(any())).thenReturn(new CloudbreakDetails("2.48.0-XXb"));
        when(clusterComponentProvider.getSaltStateComponentCbVersion(any())).thenReturn("2.53.0-XXb");

        createServiceComponentMap(Set.of(
                ServiceComponent.of("YARN", "NODEMANAGER"),
                ServiceComponent.of("HDFS", "GATEWAY"),
                ServiceComponent.of("YARN", "GATEWAY")));

        StopRestrictionReason actual = underTest.isInfrastructureStoppable(createStack("AWS", groups));

        Assertions.assertEquals(StopRestrictionReason.NONE, actual);
    }

    @Test
    public void stoppableWhenEphemeralOnlyAndAfter253CbVersionAndNodemanagerOnly() {
        Set<InstanceGroup> groups = new HashSet<>();
        groups.add(createGroup(List.of("ebs"), temporaryStorage, "master"));
        groups.add(createGroup(List.of(AwsDiskType.Ephemeral.value()), temporaryStorage, "worker"));

        when(componentConfigProviderService.getCloudbreakDetails(any())).thenReturn(new CloudbreakDetails("2.53.0-XXb"));

        createServiceComponentMap(Set.of(
                ServiceComponent.of("YARN", "NODEMANAGER")));

        StopRestrictionReason actual = underTest.isInfrastructureStoppable(createStack("AWS", groups));

        Assertions.assertEquals(StopRestrictionReason.NONE, actual);
    }

    @Test
    public void stoppableWhenEphemeralOnlyAndAfter253CbVersionAndGatewayOnly() {
        Set<InstanceGroup> groups = new HashSet<>();
        groups.add(createGroup(List.of("ebs"), temporaryStorage, "master"));
        groups.add(createGroup(List.of(AwsDiskType.Ephemeral.value()), temporaryStorage, "worker"));

        when(componentConfigProviderService.getCloudbreakDetails(any())).thenReturn(new CloudbreakDetails("2.53.0-XXb"));

        createServiceComponentMap(Set.of(
                ServiceComponent.of("HDFS", "GATEWAY"),
                ServiceComponent.of("YARN", "GATEWAY")));

        StopRestrictionReason actual = underTest.isInfrastructureStoppable(createStack("AWS", groups));

        Assertions.assertEquals(StopRestrictionReason.NONE, actual);
    }

    @Test
    public void notStoppableWhenEphemeralOnlyAndServiceNotPermitted() {
        Set<InstanceGroup> groups = new HashSet<>();
        groups.add(createGroup(List.of("ebs"), temporaryStorage, "master"));
        groups.add(createGroup(List.of(AwsDiskType.Ephemeral.value()), temporaryStorage, "worker"));

        when(componentConfigProviderService.getCloudbreakDetails(any())).thenReturn(new CloudbreakDetails("2.53.0-XXb"));

        createServiceComponentMap(Set.of(
                ServiceComponent.of("YARN", "NODEMANAGER"),
                ServiceComponent.of("HDFS", "DATANODE"),
                ServiceComponent.of("YARN", "GATEWAY")));

        StopRestrictionReason actual = underTest.isInfrastructureStoppable(createStack("AWS", groups));

        Assertions.assertEquals(StopRestrictionReason.EPHEMERAL_VOLUMES, actual);
    }

    @Test
    public void notStoppableWhenEphemeralOnlyAndRequiredServiceNotPresent() {
        Set<InstanceGroup> groups = new HashSet<>();
        groups.add(createGroup(List.of("ebs"), temporaryStorage, "master"));
        groups.add(createGroup(List.of(AwsDiskType.Ephemeral.value()), temporaryStorage, "worker"));

        when(componentConfigProviderService.getCloudbreakDetails(any())).thenReturn(new CloudbreakDetails("2.53.0-XXb"));

        createServiceComponentMap(Set.of(
                ServiceComponent.of("HDFS", "DATANODE"),
                ServiceComponent.of("YARN", "GATEWAY")));

        StopRestrictionReason actual = underTest.isInfrastructureStoppable(createStack("AWS", groups));

        Assertions.assertEquals(StopRestrictionReason.EPHEMERAL_VOLUMES, actual);
    }

    @Test
    public void infrastructureShouldBeStoppableForEBSVolumesOnly() {
        Set<InstanceGroup> groups = new HashSet<>();
        groups.add(createGroup(List.of("ebs"), temporaryStorage, "master"));

        when(componentConfigProviderService.getCloudbreakDetails(any())).thenReturn(new CloudbreakDetails("2.48.0-XXb"));

        StopRestrictionReason actual = underTest.isInfrastructureStoppable(createStack("AWS", groups));

        Assertions.assertEquals(StopRestrictionReason.NONE, actual);
    }

    @Test
    public void infrastructureShouldNotBeStoppableIfTemporaryStorageIsEphemeralVolumesBefore248CbVersion() {
        Set<InstanceGroup> groups = new HashSet<>();
        groups.add(createGroup(List.of("ebs"), temporaryStorage, "master"));
        groups.add(createGroup(List.of("ebs"), TemporaryStorage.EPHEMERAL_VOLUMES, "worker"));

        when(componentConfigProviderService.getCloudbreakDetails(any())).thenReturn(new CloudbreakDetails("2.47.0-bXX"));

        StopRestrictionReason actual = underTest.isInfrastructureStoppable(createStack("AWS", groups));

        Assertions.assertEquals(StopRestrictionReason.EPHEMERAL_VOLUME_CACHING, actual);
    }

    @Test
    public void infrastructureShouldBeStoppableIfTemporaryStorageIsEphemeralVolumesAfter248CbVersion() {
        Set<InstanceGroup> groups = new HashSet<>();
        groups.add(createGroup(List.of("ebs"), temporaryStorage, "master"));
        groups.add(createGroup(List.of("ebs"), TemporaryStorage.EPHEMERAL_VOLUMES, "worker"));

        when(componentConfigProviderService.getCloudbreakDetails(any())).thenReturn(new CloudbreakDetails("2.48.0-bXX"));

        StopRestrictionReason actual = underTest.isInfrastructureStoppable(createStack("AWS", groups));

        Assertions.assertEquals(StopRestrictionReason.NONE, actual);
    }

    @Test
    public void infrastructureShouldBeStoppableForMixedStorage() {
        Set<InstanceGroup> groups = new HashSet<>();
        groups.add(createGroup(List.of("ebs"), temporaryStorage, "master"));
        groups.add(createGroup(List.of(AwsDiskType.Ephemeral.value(), AwsDiskType.Gp2.value()), temporaryStorage, "worker"));

        when(componentConfigProviderService.getCloudbreakDetails(any())).thenReturn(new CloudbreakDetails("2.47.0-bXX"));

        Assertions.assertEquals(StopRestrictionReason.EPHEMERAL_VOLUMES, underTest.isInfrastructureStoppable(createStack("AWS", groups)));

        when(componentConfigProviderService.getCloudbreakDetails(any())).thenReturn(new CloudbreakDetails("2.48.0-bXX"));

        Assertions.assertEquals(StopRestrictionReason.NONE, underTest.isInfrastructureStoppable(createStack("AWS", groups)));
    }

    @Test
    public void infrastructureShouldBeStoppableForNonAWSClusters() {
        StopRestrictionReason actual = underTest.isInfrastructureStoppable(createStack("GCP", null));
        Assertions.assertEquals(StopRestrictionReason.NONE, actual);
    }

    @Test
    public void infrastructureShouldBeStoppableForValidInstanceGroups() {
        Set<InstanceGroup> groups = new HashSet<>();
        groups.add(createGroup(List.of("ebs"), temporaryStorage, "worker"));
        InstanceGroup master = createGroup(List.of("ebs"), temporaryStorage, "master");
        master.getTemplate().setAttributes(
                new JsonToString().convertToEntityAttribute(
                        "{\"sshLocation\":\"0.0.0.0/0\",\"encrypted\":false}"));
        groups.add(master);

        when(componentConfigProviderService.getCloudbreakDetails(any())).thenReturn(new CloudbreakDetails("2.47.0-bXX"));

        Assertions.assertEquals(StopRestrictionReason.NONE, underTest.isInfrastructureStoppable(createStack("AWS", groups)));

        when(componentConfigProviderService.getCloudbreakDetails(any())).thenReturn(new CloudbreakDetails("2.48.0-bXX"));

        Assertions.assertEquals(StopRestrictionReason.NONE, underTest.isInfrastructureStoppable(createStack("AWS", groups)));
    }

    private InstanceGroup createGroup(List<String> volumeTypes, TemporaryStorage temporaryStorage, String groupName) {
        InstanceGroup group = new InstanceGroup();
        group.setGroupName(groupName);
        Template template = new Template();
        template.setTemporaryStorage(temporaryStorage);
        group.setTemplate(template);
        template.setVolumeTemplates(Sets.newHashSet());
        for (String volumeType: volumeTypes) {
            VolumeTemplate volumeTemplate = new VolumeTemplate();
            volumeTemplate.setVolumeType(volumeType);
            template.getVolumeTemplates().add(volumeTemplate);
        }
        return group;
    }

    private Stack createStack(String cloudProvider, Set<InstanceGroup> groups) {
        Stack stack = new Stack();
        stack.setCloudPlatform(cloudProvider);
        stack.setId(42L);
        stack.setInstanceGroups(groups);
        Cluster cluster = new Cluster();
        cluster.setId(42L);
        Blueprint blueprint = new Blueprint();
        blueprint.setBlueprintText("blueprint");
        cluster.setBlueprint(blueprint);
        stack.setCluster(cluster);
        if (groups != null) {
            groups.forEach(ig -> ig.setStack(stack));
        }
        return stack;
    }

    private void createServiceComponentMapWithPermittedServices() {
        createServiceComponentMap(Set.of(
                ServiceComponent.of("YARN", "NODEMANAGER"),
                ServiceComponent.of("HDFS", "GATEWAY"),
                ServiceComponent.of("YARN", "GATEWAY")));
    }

    private void createServiceComponentMap(Set<ServiceComponent> worker) {
        Map<String, Set<ServiceComponent>> serviceCompontnsByInstanceGroup = new HashMap<>();

        serviceCompontnsByInstanceGroup.put("master", Set.of(
                ServiceComponent.of("HDFS", "NAMENODE"),
                ServiceComponent.of("HDFS", "DATANODE")));

        serviceCompontnsByInstanceGroup.put("worker", worker);

        when(cmTemplateProcessor.getServiceComponentsByHostGroup()).thenReturn(serviceCompontnsByInstanceGroup);
        when(cmTemplateProcessorFactory.get(any())).thenReturn(cmTemplateProcessor);
    }
}
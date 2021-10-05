package com.sequenceiq.cloudbreak.cloud.openstack.common;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.openstack4j.api.OSClient;
import org.openstack4j.api.compute.ComputeService;
import org.openstack4j.api.compute.FlavorService;
import org.openstack4j.model.compute.Flavor;

import com.google.common.collect.ImmutableList;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.GroupNetwork;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.common.type.TemporaryStorage;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;

@RunWith(MockitoJUnitRunner.class)
public class OpenStackFlavorVerifierTest {

    @InjectMocks
    private final OpenStackFlavorVerifier underTest = new OpenStackFlavorVerifier();

    @Mock
    private OSClient<?> osClient;

    @Mock
    private ComputeService computeService;

    @Mock
    private FlavorService flavorService;

    @Before
    public void setUp() {
        when(osClient.compute()).thenReturn(computeService);
        when(computeService.flavors()).thenReturn(flavorService);
    }

    @Test
    public void testAllFlavorsFound() {
        List<Group> instanceGroups = ImmutableList.of(createGroup("flavor1"), createGroup("flavor2"));
        doReturn(ImmutableList.of(createFlavor("flavor1"), createFlavor("flavor2"))).when(flavorService).list();
        underTest.flavorsExist(osClient, instanceGroups);
    }

    @Test(expected = CloudConnectorException.class)
    public void nullInstanceGroup() {
        try {
            underTest.flavorsExist(osClient, null);
        } catch (Exception e) {
            Assert.assertEquals("Cannot validate Flavors if InstanceGroup is null", e.getMessage());
            throw e;
        }
    }

    @Test(expected = CloudConnectorException.class)
    public void openStackNullFlavor() {
        try {
            Group g1 = new Group("name", InstanceGroupType.GATEWAY, new ArrayList<>(), null, null,
                    null, "loginUserName", "publicKey", 50, Optional.empty(), createGroupNetwork());
            List<Group> instanceGroups = ImmutableList.of(g1);
            when(flavorService.list()).thenReturn(null);
            underTest.flavorsExist(osClient, instanceGroups);
        } catch (Exception e) {
            Assert.assertEquals("No flavor found on OpenStack", e.getMessage());
            throw e;
        }
    }

    @Test(expected = CloudConnectorException.class)
    public void openStackFlavorsNotFound() {
        try {
            List<Group> instanceGroups = ImmutableList.of(createGroup("flavor3"), createGroup("flavor2"));
            doReturn(ImmutableList.of(createFlavor("flavor1"), createFlavor("flavor2"))).when(flavorService).list();
            underTest.flavorsExist(osClient, instanceGroups);
        } catch (Exception e) {
            Assert.assertEquals("Not found flavors: [flavor3]", e.getMessage());
            throw e;
        }
    }

    private Group createGroup(String flavor) {
        InstanceTemplate template =
                new InstanceTemplate(flavor, null, null, new ArrayList<>(), null, null, null, null, TemporaryStorage.ATTACHED_VOLUMES, 0L);
        CloudInstance skeleton = new CloudInstance("id1", template, null, "subnet-1", "az1");

        Group group = new Group("name", InstanceGroupType.GATEWAY, new ArrayList<>(), null, skeleton,
                null, "loginUserName", "publicKey",
                50, Optional.empty(), createGroupNetwork());
        return group;
    }

    private GroupNetwork createGroupNetwork() {
        return new GroupNetwork(OutboundInternetTraffic.DISABLED, new HashSet<>(), new HashMap<>());
    }

    private Flavor createFlavor(String flavorName) {
        Flavor flavor = Mockito.mock(Flavor.class);
        when(flavor.getName()).thenReturn(flavorName);
        return flavor;
    }
}

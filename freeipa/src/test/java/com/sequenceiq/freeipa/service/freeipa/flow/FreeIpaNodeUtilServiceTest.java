package com.sequenceiq.freeipa.service.freeipa.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Template;

@ExtendWith(MockitoExtension.class)
public class FreeIpaNodeUtilServiceTest {

    private static final String PRIVATE_IP = "10.0.0.1";

    private static final String PUBLIC_IP = "1.2.3.4";

    private static final String INSTANCE_ID = "i-1";

    private static final String INSTANCE_TYPE = "instance-type";

    private static final String FQDN = "www.example.com";

    private static final String GROUP_NAME = "group1";

    private InstanceMetaData im;

    @InjectMocks
    private FreeIpaNodeUtilService underTest;

    @BeforeEach
    void before() {
        Template template = new Template();
        template.setInstanceType(INSTANCE_TYPE);
        InstanceGroup ig = new InstanceGroup();
        ig.setTemplate(template);
        ig.setGroupName(GROUP_NAME);
        im = new InstanceMetaData();
        im.setPrivateIp(PRIVATE_IP);
        im.setInstanceId(INSTANCE_ID);
        im.setInstanceGroup(ig);
        im.setDiscoveryFQDN(FQDN);
    }

    @Test
    public void testMapInstancesToNodesThrowsWhenProvidedEmptySet() {
        assertThrows(IllegalStateException.class, () -> underTest.mapInstancesToNodes(Set.of()));
    }

    @Test
    public void testMapInstancesToNodesConvertsInstanceWithPublicIp() {
        im.setPublicIp(PUBLIC_IP);

        Set<Node> nodes = underTest.mapInstancesToNodes(Set.of(im));

        assertEquals(1, nodes.size());
        Node node = nodes.iterator().next();
        assertEquals(PRIVATE_IP, node.getPrivateIp());
        assertEquals(PUBLIC_IP, node.getPublicIp());
        assertEquals(INSTANCE_ID, node.getInstanceId());
        assertEquals(INSTANCE_TYPE, node.getInstanceType());
        assertEquals(FQDN, node.getHostname());
        assertEquals(GROUP_NAME, node.getHostGroup());
    }

    @Test
    public void testMapInstancesToNodesConvertsInstanceWithPrivateIp() {
        Set<Node> nodes = underTest.mapInstancesToNodes(Set.of(im));

        assertEquals(1, nodes.size());
        Node node = nodes.iterator().next();
        assertEquals(PRIVATE_IP, node.getPrivateIp());
        assertEquals(PRIVATE_IP, node.getPublicIp());
        assertEquals(INSTANCE_ID, node.getInstanceId());
        assertEquals(INSTANCE_TYPE, node.getInstanceType());
        assertEquals(FQDN, node.getHostname());
        assertEquals(GROUP_NAME, node.getHostGroup());
    }
}

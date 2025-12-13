package com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.yarn.YarnConstants;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.yarn.YarnRoles;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.template.model.ServiceAttributes;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.cloudbreak.util.StackUtil;

@ExtendWith(MockitoExtension.class)
class HostAttributeDecoratorTest {

    @InjectMocks
    private HostAttributeDecorator underTest;

    @Mock
    private StackDto stack;

    @Mock
    private Cluster cluster;

    @Mock
    private CmTemplateProcessorFactory cmTemplateProcessorFactory;

    @Mock
    private StackUtil stackUtil;

    @Test
    void testAddHostAttributes() {

        when(stack.getBlueprintJsonText()).thenReturn("");
        when(stackUtil.collectNodes(any())).thenReturn(Set.of(new Node(null, null, null, null, "fqdn1", "hg1"),
                new Node(null, null, null, null, "fqdn2", "hg2"),
                new Node(null, null, null, null, "fqdn3", "hg3"),
                new Node(null, null, null, null, "fqdn4", null)));

        Map<String, Map<String, ServiceAttributes>> yarnAttrs = new HashMap<>();
        yarnAttrs.put("hg3",
                Collections.singletonMap(
                        YarnRoles.YARN,
                        new ServiceAttributes(ServiceComponent.of(YarnRoles.YARN, YarnRoles.NODEMANAGER),
                                Collections.singletonMap(YarnConstants.ATTRIBUTE_NAME_NODE_INSTANCE_TYPE,
                                        YarnConstants.ATTRIBUTE_NODE_INSTANCE_TYPE_COMPUTE))));


        CmTemplateProcessor blueprintTextProcessor = mock(CmTemplateProcessor.class);
        when(blueprintTextProcessor.getHostGroupBasedServiceAttributes(any())).thenReturn(yarnAttrs);

        when(cmTemplateProcessorFactory.get(any(String.class))).thenReturn(blueprintTextProcessor);

        Map<String, SaltPillarProperties> result = underTest.createHostAttributePillars(stack);

        SaltPillarProperties resultPillar = result.get("hostattrs");
        assertEquals("/nodes/hostattrs.sls", resultPillar.getPath());
        Map<String, Object> props = resultPillar.getProperties();
        Map<String, Object> values = (Map<String, Object>) props.get("hostattrs");
        assertEquals(4, values.size());
        assertNotNull(values.get("fqdn1"));
        assertNotNull(values.get("fqdn2"));
        assertNotNull(values.get("fqdn3"));
        assertNotNull(values.get("fqdn4"));

        Map<String, Object> nodeValue;

        Map<String, Map<String, String>> attrs = null;
        nodeValue = (Map<String, Object>) values.get("fqdn1");
        assertEquals(2, nodeValue.size());
        assertEquals("hg1", nodeValue.get("hostGroup"));
        attrs  = (Map<String, Map<String, String>>) nodeValue.get("attributes");
        assertEquals(0, attrs.size());

        nodeValue = (Map<String, Object>) values.get("fqdn2");
        assertEquals(2, nodeValue.size());
        assertEquals("hg2", nodeValue.get("hostGroup"));
        attrs  = (Map<String, Map<String, String>>) nodeValue.get("attributes");
        assertEquals(0, attrs.size());

        nodeValue = (Map<String, Object>) values.get("fqdn3");
        assertEquals(2, nodeValue.size());
        assertEquals("hg3", nodeValue.get("hostGroup"));
        attrs  = (Map<String, Map<String, String>>) nodeValue.get("attributes");
        assertEquals(1, attrs.size());
        assertEquals(1, attrs.get(YarnRoles.YARN).size());
        assertEquals(YarnConstants.ATTRIBUTE_NAME_NODE_INSTANCE_TYPE, attrs.get(YarnRoles.YARN).entrySet().iterator().next().getKey());
        assertEquals(YarnConstants.ATTRIBUTE_NODE_INSTANCE_TYPE_COMPUTE, attrs.get(YarnRoles.YARN).entrySet().iterator().next().getValue());

        nodeValue = (Map<String, Object>) values.get("fqdn4");
        assertEquals(1, nodeValue.size());
        attrs  = (Map<String, Map<String, String>>) nodeValue.get("attributes");
        assertEquals(0, attrs.size());
    }
}

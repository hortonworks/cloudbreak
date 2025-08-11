package com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator;

import static java.util.Collections.singletonMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessorFactory;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.common.type.Versioned;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.template.model.ServiceAttributes;
import com.sequenceiq.cloudbreak.template.processor.BlueprintTextProcessor;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Component
public class HostAttributeDecorator {

    private static final Logger LOGGER = LoggerFactory.getLogger(HostAttributeDecorator.class);

    private final CmTemplateProcessorFactory cmTemplateProcessorFactory;

    private final StackUtil stackUtil;

    public HostAttributeDecorator(CmTemplateProcessorFactory cmTemplateProcessorFactory, StackUtil stackUtil) {
        this.cmTemplateProcessorFactory = cmTemplateProcessorFactory;
        this.stackUtil = stackUtil;
    }

    public Map<String, SaltPillarProperties> createHostAttributePillars(StackDto stackDto) {
        Set<Node> allNodes = stackUtil.collectNodes(stackDto);
        BlueprintTextProcessor blueprintTextProcessor = cmTemplateProcessorFactory.get(stackDto.getBlueprintJsonText());
        Versioned blueprintVersion = () -> blueprintTextProcessor.getVersion().get();

        Map<String, Map<String, ServiceAttributes>> serviceAttributes = blueprintTextProcessor.getHostGroupBasedServiceAttributes(blueprintVersion);

        Map<String, Map<String, Object>> attributes = new HashMap<>();
        for (Node node : allNodes) {
            Map<String, Map<String, String>> hgAttributes = getAttributesForHostGroup(node.getHostGroup(), serviceAttributes);
            Map<String, Object> hostAttributes = new HashMap<>();

            hostAttributes.put("attributes", hgAttributes);

            if (node.getHostGroup() != null) {
                hostAttributes.put("hostGroup", node.getHostGroup());
            }
            attributes.put(node.getHostname(), hostAttributes);
        }
        return Map.of("hostattrs", new SaltPillarProperties("/nodes/hostattrs.sls", singletonMap("hostattrs", attributes)));
    }

    private Map<String, Map<String, String>> getAttributesForHostGroup(String hostGroup, Map<String, Map<String, ServiceAttributes>> serviceAttributes) {
        Map<String, Map<String, String>> hgAttributes = Optional.ofNullable(serviceAttributes.get(hostGroup)).orElse(Map.of())
                .entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        v -> v.getValue().getAttributes()));
        LOGGER.debug("Attributes for hostGroup={}: [{}]", hostGroup, hgAttributes);
        return hgAttributes;
    }
}

package com.sequenceiq.freeipa.service.rotation;

import static java.util.Collections.singletonMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import jakarta.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.config.FreeIpaConfigService;
import com.sequenceiq.freeipa.service.freeipa.config.FreeIpaConfigView;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaNodeUtilService;

@Component
public class FreeIpaDefaultPillarGenerator implements Function<Stack, Map<String, SaltPillarProperties>> {

    @Inject
    private FreeIpaConfigService freeIpaConfigService;

    @Inject
    private FreeIpaNodeUtilService freeIpaNodeUtilService;

    @Override
    public Map<String, SaltPillarProperties> apply(Stack stack) {
        Map<String, SaltPillarProperties> freeIpaPillar = new HashMap<>();
        Set<InstanceMetaData> instanceMetaDatas = stack.getNotDeletedInstanceMetaDataSet();
        Set<Node> allNodes = freeIpaNodeUtilService.mapInstancesToNodes(instanceMetaDatas);
        FreeIpaConfigView freeIpaConfigView = freeIpaConfigService.createFreeIpaConfigs(stack, allNodes);
        freeIpaPillar.put("freeipa", new SaltPillarProperties("/freeipa/init.sls", singletonMap("freeipa", freeIpaConfigView.toMap())));
        return freeIpaPillar;
    }
}

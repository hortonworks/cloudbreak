package com.sequenceiq.cloudbreak.service.loadbalancer;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox.KnoxRoles;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.common.api.type.InstanceGroupType;

@Service
public class KnoxGroupDeterminer {

    private static final Logger LOGGER = LoggerFactory.getLogger(KnoxGroupDeterminer.class);

    public Set<String> getKnoxGatewayGroupNames(Stack stack) {
        MDCBuilder.buildMdcContext(stack);
        LOGGER.debug("Fetching list of instance groups with Knox gateway installed");
        Set<String> groupNames = new HashSet<>();
        Cluster cluster = stack.getCluster();
        if (cluster != null) {
            LOGGER.debug("Checking if Knox gateway is explicitly defined");
            CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(getBlueprintTest(cluster));
            groupNames = cmTemplateProcessor.getHostGroupsWithComponent(KnoxRoles.KNOX_GATEWAY);
        }
        Set<String> gatewayGroupNames = stack.getInstanceGroups().stream()
                .filter(i -> InstanceGroupType.isGateway(i.getInstanceGroupType()))
                .map(InstanceGroup::getGroupName)
                .collect(Collectors.toSet());

        if (groupNames.isEmpty()) {
            LOGGER.debug("Knox gateway is not explicitly defined; searching for CM gateway hosts");
            groupNames = gatewayGroupNames;
        } else if (!gatewayGroupNames.containsAll(groupNames)) {
            LOGGER.error("KNOX can only be installed on instance group where type is GATEWAY. As per the template " + getBlueprintName(cluster) +
                    " KNOX_GATEWAY role config is present in groups " + groupNames  + " while the GATEWAY nodeType is available for instance group " +
                    gatewayGroupNames);
            throw new CloudbreakServiceException("KNOX can only be installed on instance group where type is GATEWAY. As per the template " +
                    getBlueprintName(cluster) + " KNOX_GATEWAY role config is present in groups " + groupNames  +
                    " while the GATEWAY nodeType is available for instance group " + gatewayGroupNames);
        }

        if (groupNames.isEmpty()) {
            LOGGER.info("No Knox gateway instance groups found");
        }
        return groupNames;
    }

    private String getBlueprintName(Cluster cluster) {
        if (cluster != null) {
            Blueprint blueprint = cluster.getBlueprint();
            if (blueprint != null) {
                return blueprint.getName();
            }
        }
        return null;
    }

    private String getBlueprintTest(Cluster cluster) {
        if (cluster != null) {
            Blueprint blueprint = cluster.getBlueprint();
            if (blueprint != null) {
                return blueprint.getBlueprintJsonText();
            }
        }
        return null;
    }

}

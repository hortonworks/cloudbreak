package com.sequenceiq.cloudbreak.service.loadbalancer;

import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.CLOUDERA_STACK_VERSION_7_2_11;
import static com.sequenceiq.cloudbreak.cmtemplate.CMRepositoryVersionUtil.isVersionNewerOrEqualThanLimited;
import static com.sequenceiq.cloudbreak.service.loadbalancer.NetworkLoadBalancerAttributeUtil.getLoadBalancerAttributeIfExists;
import static com.sequenceiq.cloudbreak.service.loadbalancer.NetworkLoadBalancerAttributeUtil.isSessionStickyForTargetGroup;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.oozie.OozieRoles;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.loadbalancer.TargetGroup;
import com.sequenceiq.common.api.type.TargetGroupType;

@Service
public class OozieTargetGroupProvisioner {

    private static final Logger LOGGER = LoggerFactory.getLogger(OozieTargetGroupProvisioner.class);

    public Optional<TargetGroup> setupOozieHATargetGroup(Stack stack, boolean dryRun) {
        Optional<InstanceGroup> oozieInstanceGroup = getOozieHAInstanceGroup(stack);

        if (oozieInstanceGroup.isPresent()) {
            LOGGER.info("Oozie HA instance group found; enabling Oozie load balancer configuration.");
            TargetGroup oozieTargetGroup = new TargetGroup();
            if (CloudPlatform.GCP.equalsIgnoreCase(stack.getCloudPlatform())) {
                oozieTargetGroup.setType(TargetGroupType.OOZIE_GCP);
            } else {
                oozieTargetGroup.setType(TargetGroupType.OOZIE);
            }
            if (CloudPlatform.AWS.name().equalsIgnoreCase(stack.getCloudPlatform()) && stack.getNetwork() != null) {
                Optional<Map<String, Object>> loadBalancerAttributes = getLoadBalancerAttributeIfExists(stack.getNetwork().getAttributes());
                if (loadBalancerAttributes.isPresent()) {
                    oozieTargetGroup.setUseStickySession(isSessionStickyForTargetGroup(stack.getNetwork().getAttributes()));
                }
            }

            if (!dryRun) {
                LOGGER.debug("Adding target group to Oozie HA instance group.");
                oozieInstanceGroup.get().addTargetGroup(oozieTargetGroup);
            } else {
                LOGGER.debug("Dry run, skipping instance group/target group linkage for Oozie.");
            }
            return Optional.of(oozieTargetGroup);
        } else {
            LOGGER.info("Oozie HA instance group not found; not enabling Oozie load balancer configuration.");
            return Optional.empty();
        }
    }

    private Optional<InstanceGroup> getOozieHAInstanceGroup(Stack stack) {
        Cluster cluster = stack.getCluster();
        if (cluster != null) {
            CmTemplateProcessor cmTemplateProcessor = new CmTemplateProcessor(cluster.getBlueprint().getBlueprintJsonText());
            String cdhVersion = cmTemplateProcessor.getStackVersion();
            if (cdhVersion != null && isVersionNewerOrEqualThanLimited(cdhVersion, CLOUDERA_STACK_VERSION_7_2_11)) {
                Set<String> oozieGroupNames = getOozieGroups(cmTemplateProcessor);
                return stack.getInstanceGroups().stream()
                        .filter(ig -> oozieGroupNames.contains(ig.getGroupName()))
                        .filter(ig -> ig.getNodeCount() > 1)
                        .findFirst();
            }
        }
        return Optional.empty();
    }

    private Set<String> getOozieGroups(CmTemplateProcessor cmTemplateProcessor) {
        LOGGER.debug("Fetching list of instance groups with Oozie installed");
        Set<String> groupNames = cmTemplateProcessor.getHostGroupsWithComponent(OozieRoles.OOZIE_SERVER);
        LOGGER.info("Oozie instance groups are {}", groupNames);
        return groupNames;
    }
}

package com.sequenceiq.datalake.service.upgrade;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.osupgrade.OrderedOSUpgradeSet;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.osupgrade.OrderedOSUpgradeSetRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@Component
public class OrderedOSUpgradeRequestProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderedOSUpgradeRequestProvider.class);

    @Inject
    private DefaultOrderedOSUpgrade defaultOrderedOSUpgrade;

    @Inject
    private EnterpriseOrderedOSUpgrade enterpriseOrderedOSUpgrade;

    public OrderedOSUpgradeSetRequest createDatalakeOrderedOSUpgradeSetRequest(StackV4Response stackV4Response, String targetImageId,
            SdxClusterShape clusterShape) {
        LOGGER.debug("Creating OrderedOSUpgradeSetRequest for rolling OS upgrade");
        List<OrderedOSUpgradeSet> osUpgradeByUpgradeSets = new ArrayList<>();
        AbstractOrderedOSUpgrade orderedOSUpgrade = getOrderedOSUpgradeByShape(clusterShape);
        osUpgradeByUpgradeSets.addAll(orderedOSUpgrade.createDatalakeOrderedOSUpgrade(stackV4Response, targetImageId));
        OrderedOSUpgradeSetRequest request = new OrderedOSUpgradeSetRequest();
        request.setOrderedOsUpgradeSets(osUpgradeByUpgradeSets);
        request.setImageId(targetImageId);

        return request;
    }

    private AbstractOrderedOSUpgrade getOrderedOSUpgradeByShape(SdxClusterShape clusterShape) {
        if (SdxClusterShape.ENTERPRISE.equals(clusterShape)) {
            return enterpriseOrderedOSUpgrade;
        } else {
            return defaultOrderedOSUpgrade;
        }
    }
}

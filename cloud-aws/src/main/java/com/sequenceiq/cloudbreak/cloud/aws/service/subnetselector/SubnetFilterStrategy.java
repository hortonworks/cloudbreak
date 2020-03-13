package com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;

public abstract class SubnetFilterStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubnetFilterStrategy.class);

    public abstract SubnetSelectionResult filter(Collection<CloudSubnet> subnets, int azCount);

    public abstract SubnetFilterStrategyType getType();

}

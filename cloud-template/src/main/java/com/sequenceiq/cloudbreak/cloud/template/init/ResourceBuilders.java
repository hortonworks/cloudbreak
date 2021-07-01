package com.sequenceiq.cloudbreak.cloud.template.init;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.GroupResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.NetworkResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.OrderedBuilder;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;

@Component
public class ResourceBuilders {

    private static final Logger LOGGER = getLogger(ResourceBuilders.class);

    @Autowired(required = false)
    private List<NetworkResourceBuilder> network = new ArrayList<>();

    @Autowired(required = false)
    private List<ComputeResourceBuilder> compute = new ArrayList<>();

    @Autowired(required = false)
    private List<GroupResourceBuilder> group = new ArrayList<>();

    private final Map<Variant, List<NetworkResourceBuilder<ResourceBuilderContext>>> networkChain = new HashMap<>();

    private final Map<Variant, List<GroupResourceBuilder<ResourceBuilderContext>>> groupChain = new HashMap<>();

    private final Map<Variant, List<ComputeResourceBuilder<ResourceBuilderContext>>> computeChain = new HashMap<>();

    @PostConstruct
    public void init() {
        Comparator<OrderedBuilder> comparator = new BuilderComparator();
        initNetwork(comparator);
        initGroup(comparator);
        initCompute(comparator);
    }

    public List<NetworkResourceBuilder<ResourceBuilderContext>> network(Variant platformVariant) {
        List<NetworkResourceBuilder<ResourceBuilderContext>> networkResourceBuilders = networkChain.get(platformVariant);
        if (networkResourceBuilders == null) {
            LOGGER.info("Cannot find NetworkResourceBuilder for platform variant: '{}'", platformVariant);
            return Collections.emptyList();
        }
        return new ArrayList<>(networkResourceBuilders);
    }

    public List<ComputeResourceBuilder<ResourceBuilderContext>> compute(Variant platformVariant) {
        List<ComputeResourceBuilder<ResourceBuilderContext>> computeResourceBuilders = computeChain.get(platformVariant);
        if (computeResourceBuilders == null) {
            LOGGER.info("Cannot find ComputeResourceBuilder for platform variant: '{}'", platformVariant);
            return Collections.emptyList();
        }
        return new ArrayList<>(computeResourceBuilders);
    }

    public List<GroupResourceBuilder<ResourceBuilderContext>> group(Variant platformVariant) {
        List<GroupResourceBuilder<ResourceBuilderContext>> groupResourceBuilders = groupChain.get(platformVariant);
        if (groupResourceBuilders == null) {
            LOGGER.info("Cannot find GroupResourceBuilder for platform variant: '{}'", platformVariant);
            return Collections.emptyList();
        }
        return new ArrayList<>(groupResourceBuilders);
    }

    private void initNetwork(Comparator<OrderedBuilder> comparator) {
        for (NetworkResourceBuilder<ResourceBuilderContext> builder : network) {
            List<NetworkResourceBuilder<ResourceBuilderContext>> chain = networkChain.computeIfAbsent(builder.variant(), k -> new LinkedList<>());
            chain.add(builder);
            chain.sort(comparator);
        }
    }

    private void initCompute(Comparator<OrderedBuilder> comparator) {
        for (ComputeResourceBuilder<ResourceBuilderContext> builder : compute) {
            List<ComputeResourceBuilder<ResourceBuilderContext>> chain = computeChain.computeIfAbsent(builder.variant(), k -> new LinkedList<>());
            chain.add(builder);
            chain.sort(comparator);
        }
    }

    private void initGroup(Comparator<OrderedBuilder> comparator) {
        for (GroupResourceBuilder<ResourceBuilderContext> builder : group) {
            List<GroupResourceBuilder<ResourceBuilderContext>> chain = groupChain.computeIfAbsent(builder.variant(), k -> new LinkedList<>());
            chain.add(builder);
            chain.sort(comparator);
        }
    }

    private static class BuilderComparator implements Comparator<OrderedBuilder>, Serializable {
        @Override
        public int compare(OrderedBuilder o1, OrderedBuilder o2) {
            return Integer.compare(o1.order(), o2.order());
        }
    }

}

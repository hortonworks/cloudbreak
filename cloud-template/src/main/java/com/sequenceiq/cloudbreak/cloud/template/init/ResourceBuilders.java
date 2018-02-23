package com.sequenceiq.cloudbreak.cloud.template.init;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.GroupResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.NetworkResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.OrderedBuilder;

@Component
public class ResourceBuilders {

    @Inject
    private List<NetworkResourceBuilder> network;

    @Inject
    private List<ComputeResourceBuilder> compute;

    @Inject
    private List<GroupResourceBuilder> group;

    private final Map<Platform, List<NetworkResourceBuilder>> networkChain = new HashMap<>();

    private final Map<Platform, List<GroupResourceBuilder>> groupChain = new HashMap<>();

    private final Map<Platform, List<ComputeResourceBuilder>> computeChain = new HashMap<>();

    @PostConstruct
    public void init() {
        Comparator<OrderedBuilder> comparator = new BuilderComparator();
        initNetwork(comparator);
        initGroup(comparator);
        initCompute(comparator);
    }

    public List<NetworkResourceBuilder> network(Platform platform) {
        return new ArrayList<>(networkChain.get(platform));
    }

    public List<ComputeResourceBuilder> compute(Platform platform) {
        return new ArrayList<>(computeChain.get(platform));
    }

    public List<GroupResourceBuilder> group(Platform platform) {
        return new ArrayList<>(groupChain.get(platform));
    }

    private void initNetwork(Comparator<OrderedBuilder> comparator) {
        for (NetworkResourceBuilder<?> builder : network) {
            List<NetworkResourceBuilder> chain = networkChain.computeIfAbsent(builder.platform(), k -> new LinkedList<>());
            chain.add(builder);
            chain.sort(comparator);
        }
    }

    private void initCompute(Comparator<OrderedBuilder> comparator) {
        for (ComputeResourceBuilder<?> builder : compute) {
            List<ComputeResourceBuilder> chain = computeChain.computeIfAbsent(builder.platform(), k -> new LinkedList<>());
            chain.add(builder);
            chain.sort(comparator);
        }
    }

    private void initGroup(Comparator<OrderedBuilder> comparator) {
        for (GroupResourceBuilder<?> builder : group) {
            List<GroupResourceBuilder> chain = groupChain.computeIfAbsent(builder.platform(), k -> new LinkedList<>());
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

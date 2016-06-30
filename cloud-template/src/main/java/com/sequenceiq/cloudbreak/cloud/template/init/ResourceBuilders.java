package com.sequenceiq.cloudbreak.cloud.template.init;

import java.util.ArrayList;
import java.util.Collections;
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
    private Map<Platform, List<NetworkResourceBuilder>> networkChain = new HashMap<>();
    private Map<Platform, List<GroupResourceBuilder>> groupChain = new HashMap<>();
    private Map<Platform, List<ComputeResourceBuilder>> computeChain = new HashMap<>();

    @PostConstruct
    public void init() {
        BuilderComparator comparator = new BuilderComparator();
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

    private void initNetwork(BuilderComparator comparator) {
        for (NetworkResourceBuilder builder : network) {
            List<NetworkResourceBuilder> chain = this.networkChain.get(builder.platform());
            if (chain == null) {
                chain = new LinkedList<>();
                this.networkChain.put(builder.platform(), chain);
            }
            chain.add(builder);
            Collections.sort(chain, comparator);
        }
    }

    private void initCompute(BuilderComparator comparator) {
        for (ComputeResourceBuilder builder : compute) {
            List<ComputeResourceBuilder> chain = this.computeChain.get(builder.platform());
            if (chain == null) {
                chain = new LinkedList<>();
                this.computeChain.put(builder.platform(), chain);
            }
            chain.add(builder);
            Collections.sort(chain, comparator);
        }
    }

    private void initGroup(BuilderComparator comparator) {
        for (GroupResourceBuilder builder : group) {
            List<GroupResourceBuilder> chain = this.groupChain.get(builder.platform());
            if (chain == null) {
                chain = new LinkedList<>();
                this.groupChain.put(builder.platform(), chain);
            }
            chain.add(builder);
            Collections.sort(chain, comparator);
        }
    }

    private class BuilderComparator implements Comparator<OrderedBuilder> {
        @Override
        public int compare(OrderedBuilder o1, OrderedBuilder o2) {
            return Integer.compare(o1.order(), o2.order());
        }
    }

}

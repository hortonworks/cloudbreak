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

import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.NetworkResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.template.OrderedBuilder;

@Component
public class ResourceBuilders {

    @Inject
    private List<NetworkResourceBuilder> network;
    @Inject
    private List<ComputeResourceBuilder> compute;
    private Map<String, List<NetworkResourceBuilder>> networkChain = new HashMap<>();
    private Map<String, List<ComputeResourceBuilder>> computeChain = new HashMap<>();

    @PostConstruct
    public void init() {
        BuilderComparator comparator = new BuilderComparator();
        initNetwork(comparator);
        initCompute(comparator);
    }

    public List<NetworkResourceBuilder> network(String platform) {
        return new ArrayList<>(networkChain.get(platform));
    }

    public List<ComputeResourceBuilder> compute(String platform) {
        return new ArrayList<>(computeChain.get(platform));
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

    private class BuilderComparator implements Comparator<OrderedBuilder> {
        @Override
        public int compare(OrderedBuilder o1, OrderedBuilder o2) {
            return Integer.compare(o1.order(), o2.order());
        }
    }

}

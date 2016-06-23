package com.sequenceiq.cloudbreak.cloud.template.init

import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.HashMap
import java.util.LinkedList

import javax.annotation.PostConstruct
import javax.inject.Inject

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.template.ComputeResourceBuilder
import com.sequenceiq.cloudbreak.cloud.template.NetworkResourceBuilder
import com.sequenceiq.cloudbreak.cloud.template.OrderedBuilder

@Component
class ResourceBuilders {

    @Inject
    private val network: List<NetworkResourceBuilder<ResourceBuilderContext>>? = null
    @Inject
    private val compute: List<ComputeResourceBuilder<ResourceBuilderContext>>? = null
    private val networkChain = HashMap<Platform, List<NetworkResourceBuilder<ResourceBuilderContext>>>()
    private val computeChain = HashMap<Platform, List<ComputeResourceBuilder<ResourceBuilderContext>>>()

    @PostConstruct
    fun init() {
        val comparator = BuilderComparator()
        initNetwork(comparator)
        initCompute(comparator)
    }

    fun network(platform: Platform): List<NetworkResourceBuilder<ResourceBuilderContext>> {
        return ArrayList<NetworkResourceBuilder<ResourceBuilderContext>>(networkChain[platform])
    }

    fun compute(platform: Platform): List<ComputeResourceBuilder<ResourceBuilderContext>> {
        return ArrayList<ComputeResourceBuilder<ResourceBuilderContext>>(computeChain[platform])
    }

    private fun initNetwork(comparator: BuilderComparator) {
        for (builder in network!!) {
            var chain: MutableList<NetworkResourceBuilder<ResourceBuilderContext>>? = this.networkChain[builder.platform()]
            if (chain == null) {
                chain = LinkedList<NetworkResourceBuilder>()
                this.networkChain.put(builder.platform(), chain)
            }
            chain.add(builder)
            Collections.sort(chain, comparator)
        }
    }

    private fun initCompute(comparator: BuilderComparator) {
        for (builder in compute!!) {
            var chain: MutableList<ComputeResourceBuilder<ResourceBuilderContext>>? = this.computeChain[builder.platform()]
            if (chain == null) {
                chain = LinkedList<ComputeResourceBuilder>()
                this.computeChain.put(builder.platform(), chain)
            }
            chain.add(builder)
            Collections.sort(chain, comparator)
        }
    }

    private inner class BuilderComparator : Comparator<OrderedBuilder> {
        override fun compare(o1: OrderedBuilder, o2: OrderedBuilder): Int {
            return Integer.compare(o1.order(), o2.order())
        }
    }

}

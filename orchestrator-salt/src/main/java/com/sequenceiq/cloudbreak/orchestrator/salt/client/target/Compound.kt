package com.sequenceiq.cloudbreak.orchestrator.salt.client.target

import java.util.Collections
import java.util.stream.Collectors

class Compound @JvmOverloads constructor(private val targets: Collection<String>, private val type: Compound.CompoundType = Compound.CompoundType.IP) : Target<String> {

    constructor(nodeIP: String) : this(listOf<String>(nodeIP), CompoundType.IP) {
    }

    override val target: String
        get() = type.delimiter + targets.stream().collect(Collectors.joining(" or " + type.delimiter))

    override fun getType(): String {
        return "compound"
    }

    enum class CompoundType private constructor(private val delimiter: String) {
        IP("S@"),
        HOST("L@"),
        GRAINS("G@"),
        PILLAR("I@");

        fun delimiter(): String {
            return delimiter
        }
    }
}
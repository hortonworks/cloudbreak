package com.sequenceiq.cloudbreak.controller.validation.blueprint

class StackServiceComponentDescriptor(val name: String, val category: String, val minCardinality: Int, val maxCardinality: Int) {

    val isMaster: Boolean
        get() = MASTER == category

    companion object {
        private val MASTER = "MASTER"
    }
}

package com.sequenceiq.it.util

class ServerAddressGenerator(private val numberOfServers: Int) {

    fun iterateOver(serverAddressGeneratorFunction: ServerAddressGeneratorFunction) {
        for (i in 0..numberOfServers / ADDRESS_RANGE) {
            val subAddress = Integer.min(ADDRESS_RANGE, numberOfServers - i * ADDRESS_RANGE)
            for (j in 1..subAddress) {
                serverAddressGeneratorFunction.doSomething("192.168.$i.$j")
            }
        }
    }

    fun iterateOver(serverAddressGeneratorWithNumberFunction: ServerAddressGeneratorWithNumberFunction) {
        for (i in 0..numberOfServers / ADDRESS_RANGE) {
            val subAddress = Integer.min(ADDRESS_RANGE, numberOfServers - i * ADDRESS_RANGE)
            for (j in 1..subAddress) {
                serverAddressGeneratorWithNumberFunction.doSomething("192.168.$i.$j", i * ADDRESS_RANGE + j - 1)
            }
        }
    }

    @FunctionalInterface
    interface ServerAddressGeneratorFunction {
        fun doSomething(address: String)
    }

    @FunctionalInterface
    interface ServerAddressGeneratorWithNumberFunction {
        fun doSomething(address: String, number: Int)
    }

    companion object {

        val ADDRESS_RANGE = 254
    }
}

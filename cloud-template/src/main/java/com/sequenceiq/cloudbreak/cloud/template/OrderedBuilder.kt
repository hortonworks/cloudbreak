package com.sequenceiq.cloudbreak.cloud.template

/**
 * Compute and network builders are made of ordered chain of builders. This interface provides the order of the builder in the chain. The lower the order is
 * the earlier the builder will be called.
 */
interface OrderedBuilder {

    /**
     * Specifies the order of the builder in the chain. Lower value means higher precedence.
     */
    fun order(): Int

    companion object {

        /**
         * Useful constant for the highest precedence value.

         * @see java.lang.Integer.MIN_VALUE
         */
        val HIGHEST_PRECEDENCE = Integer.MIN_VALUE

        /**
         * Useful constant for the lowest precedence value.

         * @see java.lang.Integer.MAX_VALUE
         */
        val LOWEST_PRECEDENCE = Integer.MAX_VALUE
    }
}

package com.sequenceiq.cloudbreak.cloud.model

class CloudPlatformVariant(val platform: Platform, variant: Variant?) {
    var variant: Variant? = null
        private set

    init {
        if (variant == null) {
            this.variant = Variant.EMPTY
        } else {
            this.variant = variant
        }
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o == null || javaClass != o.javaClass) {
            return false
        }

        val that = o as CloudPlatformVariant?

        if (platform != that.platform) {
            return false
        }
        return variant == that.variant

    }

    override fun hashCode(): Int {
        return 31 * platform.hashCode() + variant!!.hashCode()
    }

    override fun toString(): String {
        return "{"
        +"platform='" + platform.value() + '\''
        +", variant='" + variant!!.value() + '\''
        +'}'
    }
}

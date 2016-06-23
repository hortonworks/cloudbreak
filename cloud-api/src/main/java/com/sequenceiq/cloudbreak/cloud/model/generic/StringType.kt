package com.sequenceiq.cloudbreak.cloud.model.generic

abstract class StringType protected constructor(private val value: String?) {

    fun value(): String {
        return this.value
    }

    override fun toString(): String {
        return "StringType{value='$value\'}"
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o == null || javaClass != o.javaClass) {
            return false
        }
        val that = o as StringType?
        return !if (value != null) value != that.value else that.value != null

    }

    override fun hashCode(): Int {
        return if (value != null) value.hashCode() else 0
    }
}

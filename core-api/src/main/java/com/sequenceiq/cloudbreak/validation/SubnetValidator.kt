package com.sequenceiq.cloudbreak.validation

import java.util.Arrays
import java.util.function.ToIntFunction

import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

import org.apache.commons.net.util.SubnetUtils

class SubnetValidator : ConstraintValidator<ValidSubnet, String> {

    override fun initialize(constraintAnnotation: ValidSubnet) {
    }

    override fun isValid(value: String?, context: ConstraintValidatorContext): Boolean {
        if (value == null) {
            return true
        } else if (value.isEmpty()) {
            return false
        }
        try {
            val info = SubnetUtils(value).info
            val addr = toInt(info.address)
            val lowerAddr = toInt(info.lowAddress) - 1
            if (addr != lowerAddr) {
                return false
            }
            val ip = Ip(info.address)
            return Ip("10.0.0.0").compareTo(ip) <= 0 && Ip("10.255.255.255").compareTo(ip) >= 0
                    || Ip("172.16.0.0").compareTo(ip) <= 0 && Ip("172.31.255.255").compareTo(ip) >= 0
                    || Ip("192.168.0.0").compareTo(ip) <= 0 && Ip("192.168.255.255").compareTo(ip) >= 0
        } catch (e: RuntimeException) {
            return false
        }

    }

    private fun toInt(addr: String): Int {
        return Integer.parseInt(addr.replace(".", ""))
    }

    private class Ip internal constructor(ip: String) : Comparable<Ip> {
        private val parts: IntArray

        init {
            parts = Arrays.asList<String>(*ip.split("\\.".toRegex()).dropLastWhile({ it.isEmpty() }).toTypedArray()).stream().mapToInt(ToIntFunction<kotlin.String> { value -> Integer.parseInt(value) }).toArray()
        }

        override fun compareTo(o: Ip): Int {
            if (this === o) {
                return 0
            }
            for (i in parts.indices) {
                if (parts[i] < o.parts[i]) {
                    return -1
                } else if (parts[i] > o.parts[i]) {
                    return 1
                }
            }
            return 0
        }
    }
}
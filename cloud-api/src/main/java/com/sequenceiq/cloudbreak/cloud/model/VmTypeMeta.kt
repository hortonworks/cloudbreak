package com.sequenceiq.cloudbreak.cloud.model

import java.util.HashMap

class VmTypeMeta {

    var magneticConfig: VolumeParameterConfig? = null
    var autoAttachedConfig: VolumeParameterConfig? = null
    var ssdConfig: VolumeParameterConfig? = null
    var ephemeralConfig: VolumeParameterConfig? = null
    var st1Config: VolumeParameterConfig? = null
    var properties: Map<String, String> = HashMap()

    class VmTypeMetaBuilder private constructor() {

        private var magneticConfig: VolumeParameterConfig? = null
        private var autoAttachedConfig: VolumeParameterConfig? = null
        private var ssdConfig: VolumeParameterConfig? = null
        private var ephemeralConfig: VolumeParameterConfig? = null
        private var st1Config: VolumeParameterConfig? = null
        private val properties = HashMap<String, String>()

        fun withMagneticConfig(minimumSize: Int?, maximumSize: Int?, minimumNumber: Int?, maximumNumber: Int?): VmTypeMetaBuilder {
            this.magneticConfig = VolumeParameterConfig(VolumeParameterType.MAGNETIC, minimumSize, maximumSize, minimumNumber, maximumNumber)
            return this
        }

        fun withMagneticConfig(volumeParameterConfig: VolumeParameterConfig): VmTypeMetaBuilder {
            this.magneticConfig = volumeParameterConfig
            return this
        }

        fun withAutoAttachedConfig(minimumSize: Int?, maximumSize: Int?, minimumNumber: Int?, maximumNumber: Int?): VmTypeMetaBuilder {
            this.autoAttachedConfig = VolumeParameterConfig(VolumeParameterType.AUTO_ATTACHED, minimumSize, maximumSize, minimumNumber, maximumNumber)
            return this
        }

        fun withAutoAttachedConfig(volumeParameterConfig: VolumeParameterConfig): VmTypeMetaBuilder {
            this.autoAttachedConfig = volumeParameterConfig
            return this
        }

        fun withSsdConfig(minimumSize: Int?, maximumSize: Int?, minimumNumber: Int?, maximumNumber: Int?): VmTypeMetaBuilder {
            this.ssdConfig = VolumeParameterConfig(VolumeParameterType.SSD, minimumSize, maximumSize, minimumNumber, maximumNumber)
            return this
        }

        fun withSsdConfig(volumeParameterConfig: VolumeParameterConfig): VmTypeMetaBuilder {
            this.ssdConfig = volumeParameterConfig
            return this
        }

        fun withEphemeralConfig(minimumSize: Int?, maximumSize: Int?, minimumNumber: Int?, maximumNumber: Int?): VmTypeMetaBuilder {
            this.ephemeralConfig = VolumeParameterConfig(VolumeParameterType.EPHEMERAL, minimumSize, maximumSize, minimumNumber, maximumNumber)
            return this
        }

        fun withEphemeralConfig(volumeParameterConfig: VolumeParameterConfig): VmTypeMetaBuilder {
            this.ephemeralConfig = volumeParameterConfig
            return this
        }

        fun withSt1Config(minimumSize: Int?, maximumSize: Int?, minimumNumber: Int?, maximumNumber: Int?): VmTypeMetaBuilder {
            this.st1Config = VolumeParameterConfig(VolumeParameterType.ST1, minimumSize, maximumSize, minimumNumber, maximumNumber)
            return this
        }

        fun withSt1Config(volumeParameterConfig: VolumeParameterConfig): VmTypeMetaBuilder {
            this.st1Config = volumeParameterConfig
            return this
        }

        fun withProperty(name: String, value: String): VmTypeMetaBuilder {
            this.properties.put(name, value)
            return this
        }

        fun withCpuAndMemory(cpu: Int?, memory: Float?): VmTypeMetaBuilder {
            this.properties.put(CPU, cpu!!.toString())
            this.properties.put(MEMORY, memory!!.toString())
            return this
        }

        fun withCpuAndMemory(cpu: String, memory: String): VmTypeMetaBuilder {
            this.properties.put(CPU, cpu)
            this.properties.put(MEMORY, memory)
            return this
        }

        fun withMaximumPersistentDisksSizeGb(maximumPersistentDisksSizeGb: Float?): VmTypeMetaBuilder {
            this.properties.put(MAXIMUM_PERSISTENT_DISKS_SIZE_GB, maximumPersistentDisksSizeGb!!.toString())
            return this
        }

        fun withMaximumPersistentDisksSizeGb(maximumPersistentDisksSizeGb: String): VmTypeMetaBuilder {
            this.properties.put(MAXIMUM_PERSISTENT_DISKS_SIZE_GB, maximumPersistentDisksSizeGb)
            return this
        }

        fun create(): VmTypeMeta {
            val vmTypeMeta = VmTypeMeta()
            vmTypeMeta.autoAttachedConfig = this.autoAttachedConfig
            vmTypeMeta.ephemeralConfig = this.ephemeralConfig
            vmTypeMeta.magneticConfig = this.magneticConfig
            vmTypeMeta.ssdConfig = this.ssdConfig
            vmTypeMeta.st1Config = this.st1Config
            vmTypeMeta.properties = this.properties
            return vmTypeMeta
        }

        companion object {

            fun builder(): VmTypeMetaBuilder {
                return VmTypeMetaBuilder()
            }
        }
    }

    companion object {

        val CPU = "Cpu"
        val MEMORY = "Memory"
        val MAXIMUM_PERSISTENT_DISKS_SIZE_GB = "maximumPersistentDisksSizeGb"
    }
}

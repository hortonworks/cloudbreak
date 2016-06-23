package com.sequenceiq.cloudbreak.cloud.model

import com.sequenceiq.cloudbreak.cloud.model.generic.StringType

class VmType : StringType {

    val metaData: VmTypeMeta?

    private constructor(vmType: String) : super(vmType) {
    }

    private constructor(vmType: String, meta: VmTypeMeta) : super(vmType) {
        this.metaData = meta
    }

    fun getVolumeParameterbyVolumeParameterType(volumeParameterType: VolumeParameterType): VolumeParameterConfig {
        return volumeParameterType.getVolumeParameterbyType(this.metaData)
    }

    fun getMetaDataValue(key: String): String {
        return metaData!!.properties[key]
    }

    val isMetaSet: Boolean
        get() = metaData != null

    companion object {

        fun vmType(vmType: String): VmType {
            return VmType(vmType)
        }

        fun vmTypeWithMeta(vmType: String, meta: VmTypeMeta): VmType {
            return VmType(vmType, meta)
        }
    }
}

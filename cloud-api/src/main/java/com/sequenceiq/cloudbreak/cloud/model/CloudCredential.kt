package com.sequenceiq.cloudbreak.cloud.model

import java.util.HashMap

import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel

class CloudCredential @JvmOverloads constructor(val id: Long?, val name: String, val publicKey: String, val loginUserName: String, parameters: MutableMap<String, Any> = HashMap<String, Any>()) : DynamicModel(parameters)

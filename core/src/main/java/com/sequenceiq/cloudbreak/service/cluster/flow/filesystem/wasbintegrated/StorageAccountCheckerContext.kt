package com.sequenceiq.cloudbreak.service.cluster.flow.filesystem.wasbintegrated

class StorageAccountCheckerContext(val tenantId: String, val subscriptionId: String, val appId: String, val appPassword: String, val storageAccountName: String,
                                   val resourceGroupName: String)

package com.sequenceiq.cloudbreak.cloud.mock

import java.security.KeyManagementException
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.util.Arrays

import javax.annotation.PostConstruct
import javax.net.ssl.SSLContext

import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.conn.ssl.TrustSelfSignedStrategy
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.ssl.SSLContexts
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

import com.google.gson.Gson
import com.mashape.unirest.http.ObjectMapper
import com.mashape.unirest.http.Unirest
import com.mashape.unirest.http.exceptions.UnirestException
import com.sequenceiq.cloudbreak.cloud.MetadataCollector
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance
import com.sequenceiq.cloudbreak.cloud.model.CloudResource
import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus

@Service
class MockMetadataCollector : MetadataCollector {

    @Value("${mock.spi.endpoint:https://localhost:9443}")
    private val mockServerAddress:String? = null

    @PostConstruct
    fun setUp() {
    setObjectMapper()
    disableSSLCheck()
    }

    private fun setObjectMapper() {
    val gson = Gson()
    Unirest.setObjectMapper(object:ObjectMapper {
    public override fun <T> readValue(value:String, valueType:Class<T>):T {
    return gson.fromJson<T>(value, valueType)
    }

    public override fun writeValue(value:Any):String {
    return gson.toJson(value)
    }
    })
    }

    private fun disableSSLCheck() {
    try
    {
    val sslcontext = SSLContexts.custom().loadTrustMaterial(null, TrustSelfSignedStrategy()).build()
    val sslsf = SSLConnectionSocketFactory(sslcontext)
    val httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build()
    Unirest.setHttpClient(httpclient)
    }
    catch (e:NoSuchAlgorithmException) {
    throw RuntimeException("can't create ssl settings")
}
catch (e:KeyManagementException) {
    throw RuntimeException("can't create ssl settings")
}
catch (e:KeyStoreException) {
    throw RuntimeException("can't create ssl settings")
}

}

override fun collect(authenticatedContext: AuthenticatedContext, resources: List<CloudResource>, vms: List<CloudInstance>): List<CloudVmMetaDataStatus> {
    try {
        LOGGER.info("collect metadata from mock spi, server address: " + mockServerAddress!!)
        val response = Unirest.post(mockServerAddress!! + "/spi/cloud_metadata_statuses").body(vms).asObject<Array<CloudVmMetaDataStatus>>(Array<CloudVmMetaDataStatus>::class.java).body
        return Arrays.asList(*response)
    } catch (e: UnirestException) {
        throw RuntimeException("can't convert to object", e)
    }

}

companion object {

    private val LOGGER = LoggerFactory.getLogger(MockMetadataCollector::class.java)
}
}

package com.sequenceiq.cloudbreak.service.registry

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.util.StringUtils
import org.xbill.DNS.Lookup
import org.xbill.DNS.Record
import org.xbill.DNS.SRVRecord
import org.xbill.DNS.TextParseException
import org.xbill.DNS.Type

@Component
class DNSServiceAddressResolver : ServiceAddressResolver {

    @Throws(ServiceAddressResolvingException::class)
    override fun resolveUrl(serviceUrl: String, protocol: String, serviceId: String): String {
        val resolvedAddress: String
        if (!StringUtils.isEmpty(serviceUrl)) {
            resolvedAddress = serviceUrl
        } else if (!StringUtils.isEmpty(protocol) && !StringUtils.isEmpty(serviceId)) {
            resolvedAddress = protocol + "://" + dnsSrvLookup(serviceId)
        } else {
            throw IllegalArgumentException("serviceUrl or (protocol, serviceId) must be given!")
        }
        return resolvedAddress
    }

    @Throws(ServiceAddressResolvingException::class)
    override fun resolveHostPort(serviceHost: String, servicePort: String, serviceId: String): String {
        val resolvedAddress: String
        if (!StringUtils.isEmpty(serviceHost) && !StringUtils.isEmpty(servicePort)) {
            resolvedAddress = serviceHost + ":" + servicePort
        } else if (!StringUtils.isEmpty(serviceId)) {
            resolvedAddress = dnsSrvLookup(serviceId)
        } else {
            throw IllegalArgumentException("(serviceHost, servicePort) or serviceId must be given!")
        }
        return resolvedAddress
    }

    @Throws(ServiceAddressResolvingException::class)
    private fun dnsSrvLookup(query: String): String {
        var result: String? = null
        try {
            val records = Lookup(query, Type.SRV).run()
            if (records != null && records.size > 0) {
                val srv = records[0] as SRVRecord
                result = srv.target.toString().replaceFirst("\\.$".toRegex(), "") + ":" + srv.port
            } else {
                throw ServiceAddressResolvingException("The Service $query cannot be resolved")
            }
        } catch (e: TextParseException) {
            throw ServiceAddressResolvingException("The Service $query cannot be resolved", e)
        }

        return result
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(DNSServiceAddressResolver::class.java)
    }
}

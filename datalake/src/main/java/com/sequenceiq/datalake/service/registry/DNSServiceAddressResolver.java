package com.sequenceiq.datalake.service.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

@Component
public class DNSServiceAddressResolver implements ServiceAddressResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(DNSServiceAddressResolver.class);

    @Override
    public String resolveUrl(String serviceUrl, String protocol, String serviceId)  throws ServiceAddressResolvingException {
        String resolvedAddress;
        if (!StringUtils.isEmpty(serviceUrl)) {
            resolvedAddress = serviceUrl;
        } else if (!StringUtils.isEmpty(protocol) && !StringUtils.isEmpty(serviceId)) {
            resolvedAddress = protocol + "://" + dnsSrvLookup(serviceId);
        } else {
            throw new IllegalArgumentException("serviceUrl or (protocol, serviceId) must be given!");
        }
        return resolvedAddress;
    }

    @Override
    public String resolveHostPort(String serviceHost, String servicePort, String serviceId) throws ServiceAddressResolvingException {
        String resolvedAddress;
        if (!StringUtils.isEmpty(serviceHost) && !StringUtils.isEmpty(servicePort)) {
            resolvedAddress = serviceHost + ':' + servicePort;
        } else if (!StringUtils.isEmpty(serviceId)) {
            resolvedAddress = dnsSrvLookup(serviceId);
        } else {
            throw new IllegalArgumentException("(serviceHost, servicePort) or serviceId must be given!");
        }
        return resolvedAddress;
    }

    private String dnsSrvLookup(String query) throws ServiceAddressResolvingException {
        String result;
        try {
            Record[] records = new Lookup(query, Type.SRV).run();
            if (records != null && records.length > 0) {
                SRVRecord srv = (SRVRecord) records[0];
                result = srv.getTarget().toString().replaceFirst("\\.$", "") + ':' + srv.getPort();
            } else {
                throw new ServiceAddressResolvingException("The Service " + query + " cannot be resolved");
            }
        } catch (TextParseException e) {
            throw new ServiceAddressResolvingException("The Service " + query + " cannot be resolved", e);
        }
        return result;
    }
}

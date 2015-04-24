package com.sequenceiq.cloudbreak.service.network;

import static java.util.Collections.unmodifiableSet;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Subnet;

@Service
public class SecurityService {

    @Value("${cb.public.ip:0.0.0.0/0}")
    private String publicIp;
    private Set<String> cbAddresses;

    @PostConstruct
    public void parseAddresses() {
        Set<String> tempAddresses = new HashSet<>();
        String[] addresses = publicIp.split(",");
        for (String address : addresses) {
            String cidr = address;
            if (cidr.indexOf("/") == -1) {
                cidr += NetworkConfig.NETMASK_32;
            }
            tempAddresses.add(cidr);
        }
        cbAddresses = unmodifiableSet(tempAddresses);
    }

    public Set<Subnet> getCloudbreakSubnets(Stack stack) {
        Set<Subnet> result = new HashSet<>(cbAddresses.size());
        for (String cidr : cbAddresses) {
            result.add(new Subnet(cidr, false, stack));
        }
        return result;
    }
}

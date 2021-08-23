package com.sequenceiq.cloudbreak.domain.stack.loadbalancer.azure;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The top Azure specific target group metadata database object. For Azure we keep track of a mapping of
 * the traffic port to the list of availability sets the traffic on that port is routed to.
 *
 * A note about the list of availability sets: The way this is currently implemented in the CB load balancer
 * model is that there is a 1-to-1 relationship between instance groups and availabilty sets. The Azure load
 * balancer is limited so that each load balancing rule routes traffic to a single instance group/availability set.
 *
 * It is possible in the future that the Azure load balancer logic will change to route traffic to multiple
 * availability sets in a single load balancer rule. To avoid the need for database and API changes if that
 * happens, the availiabilty set name is stored in a List. However, in the current implementation this list
 * will always be of size one.
 */
public class AzureTargetGroupConfigDb {

    private Map<Integer, List<String>> portAvailabilitySetMapping = new HashMap<>();

    public Map<Integer, List<String>> getPortAvailabilitySetMapping() {
        return portAvailabilitySetMapping;
    }

    public void setPortAvailabilitySetMapping(Map<Integer, List<String>> portAvailabilitySetMapping) {
        this.portAvailabilitySetMapping = portAvailabilitySetMapping;
    }

    public void addPortAvailabilitySetMapping(Integer port, List<String> availabilitySets) {
        portAvailabilitySetMapping.put(port, availabilitySets);
    }

    @Override
    public String toString() {
        return "AzureTargetGroupConfigDb{" +
                "portAvailabilitySetMapping=" + portAvailabilitySetMapping +
                '}';
    }
}

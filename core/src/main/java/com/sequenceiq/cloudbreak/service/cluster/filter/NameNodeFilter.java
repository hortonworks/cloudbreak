package com.sequenceiq.cloudbreak.service.cluster.filter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

@Component
public class NameNodeFilter implements HostFilter {

    @Override
    public List<InstanceMetaData> filter(long clusterId, Map<String, String> config, List<InstanceMetaData> hosts) throws HostFilterException {
        List<InstanceMetaData> result = new ArrayList<>(hosts);
        try {
            String nameNode = config.get(ConfigParam.NAMENODE_HTTP_ADDRESS.key());
            String secondaryNameNode = config.get(ConfigParam.SECONDARY_NAMENODE_HTTP_ADDRESS.key());
            String nameNodeHost = nameNode.substring(0, nameNode.lastIndexOf(':'));
            String secondaryNameNodeHost = secondaryNameNode.substring(0, secondaryNameNode.lastIndexOf(':'));
            Iterator<InstanceMetaData> iterator = result.iterator();
            while (iterator.hasNext()) {
                String hostName = iterator.next().getDiscoveryFQDN();
                if (hostName.equals(nameNodeHost) || hostName.equals(secondaryNameNodeHost)) {
                    iterator.remove();
                }
            }
        } catch (RuntimeException e) {
            throw new HostFilterException("Cannot check the address of the NN and SNN", e);
        }
        return result;
    }

}

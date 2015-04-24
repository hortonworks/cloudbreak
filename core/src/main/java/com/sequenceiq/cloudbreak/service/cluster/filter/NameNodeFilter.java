package com.sequenceiq.cloudbreak.service.cluster.filter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.service.cluster.ConfigParam;

@Component
public class NameNodeFilter implements HostFilter {

    @Override
    public List<HostMetadata> filter(long stackId, Map<String, String> config, List<HostMetadata> hosts) throws HostFilterException {
        List<HostMetadata> result = new ArrayList<>(hosts);
        try {
            String nameNode = config.get(ConfigParam.NAMENODE_HTTP_ADDRESS.key());
            String secondaryNameNode = config.get(ConfigParam.SECONDARY_NAMENODE_HTTP_ADDRESS.key());
            String nameNodeHost = nameNode.substring(0, nameNode.lastIndexOf(':'));
            String secondaryNameNodeHost = secondaryNameNode.substring(0, secondaryNameNode.lastIndexOf(':'));
            Iterator<HostMetadata> iterator = result.iterator();
            while (iterator.hasNext()) {
                String hostName = iterator.next().getHostName();
                if (hostName.equals(nameNodeHost) || hostName.equals(secondaryNameNodeHost)) {
                    iterator.remove();
                }
            }
        } catch (Exception e) {
            throw new HostFilterException("Cannot check the address of the NN and SNN", e);
        }
        return result;
    }

}

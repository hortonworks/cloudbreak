package com.sequenceiq.cloudbreak.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ClusterDefinitionUpdaterConnectors {

    @Autowired(required = false)
    private List<ClusterDefinitionUpdater> clusterDefinitionUpdaters = new ArrayList<>();

    private Map<String, ClusterDefinitionUpdater> map;

    @PostConstruct
    private void initClusterDefinitionMap() {
        map = new HashMap<>();
        for (ClusterDefinitionUpdater cdu : clusterDefinitionUpdaters) {
            map.put(cdu.getVariant(), cdu);
        }
    }

    public String getClusterDefinitionText(TemplatePreparationObject templatePreparationObject) {
        ClusterDefinitionUpdater cdu = map.get(templatePreparationObject.getGeneralClusterConfigs().getVariant());
        return cdu == null ? "" : cdu.getClusterDefinitionText(templatePreparationObject);
    }

}

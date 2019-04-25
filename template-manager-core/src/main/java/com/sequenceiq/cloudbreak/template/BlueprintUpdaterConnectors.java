package com.sequenceiq.cloudbreak.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BlueprintUpdaterConnectors {

    @Autowired(required = false)
    private List<BlueprintUpdater> blueprintUpdaters = new ArrayList<>();

    private Map<String, BlueprintUpdater> map;

    @PostConstruct
    private void initBlueprintMap() {
        map = new HashMap<>();
        for (BlueprintUpdater cdu : blueprintUpdaters) {
            map.put(cdu.getVariant(), cdu);
        }
    }

    public String getBlueprintText(TemplatePreparationObject templatePreparationObject) {
        BlueprintUpdater cdu = map.get(templatePreparationObject.getGeneralClusterConfigs().getVariant());
        return cdu == null ? "" : cdu.getBlueprintText(templatePreparationObject);
    }

}

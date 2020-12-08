package com.sequenceiq.cloudbreak.template;

import java.util.List;
import java.util.Map;

public interface BlueprintUpdater {
    String getBlueprintText(TemplatePreparationObject source);

    String getBlueprintText(TemplatePreparationObject source, Map<String, List<Map<String, String>>> hostGroupMappings);

    String getVariant();
}

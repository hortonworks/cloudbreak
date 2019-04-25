package com.sequenceiq.cloudbreak.template;

public interface BlueprintUpdater {
    String getBlueprintText(TemplatePreparationObject source);

    String getVariant();
}

package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.CrossRealmTrustModelDescriptions.BASE_CLUSTER_CM_INSTRUCTIONS;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.CrossRealmTrustModelDescriptions.BASE_CLUSTER_CM_INSTRUCTIONS_DOCLINK;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClouderaManagerSetupInstructions {
    @Schema(description = BASE_CLUSTER_CM_INSTRUCTIONS)
    private String explanation;

    @Schema(description = BASE_CLUSTER_CM_INSTRUCTIONS_DOCLINK)
    private String docs;

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getDocs() {
        return docs;
    }

    public void setDocs(String docs) {
        this.docs = docs;
    }

    @Override
    public String toString() {
        return "ClouderaManagerSetupInstructions{" +
                "explanation='" + explanation + '\'' +
                ", docs='" + docs + '\'' +
                '}';
    }
}

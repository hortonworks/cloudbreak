package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands;

import static com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.CrossRealmTrustModelDescriptions.BASE_CLUSTER_CM_INSTRUCTIONS;
import static com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions.CrossRealmTrustModelDescriptions.BASE_CLUSTER_KRB5_CONF;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Deserializer;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Serializer;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseClusterTrustSetupCommands {
    @Schema(description = BASE_CLUSTER_KRB5_CONF, requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonSerialize(using = Base64Serializer.class)
    @JsonDeserialize(using = Base64Deserializer.class)
    private String krb5Conf;

    @Schema(description = BASE_CLUSTER_CM_INSTRUCTIONS)
    private ClouderaManagerSetupInstructions clouderaManagerSetupInstructions;

    public String getKrb5Conf() {
        return krb5Conf;
    }

    public void setKrb5Conf(String krb5Conf) {
        this.krb5Conf = krb5Conf;
    }

    public ClouderaManagerSetupInstructions getClouderaManagerSetupInstructions() {
        return clouderaManagerSetupInstructions;
    }

    public void setClouderaManagerSetupInstructions(ClouderaManagerSetupInstructions clouderaManagerSetupInstructions) {
        this.clouderaManagerSetupInstructions = clouderaManagerSetupInstructions;
    }

    @Override
    public String toString() {
        return "BaseClusterTrustSetupCommands{" +
                "krb5Conf='" + krb5Conf + '\'' +
                ", clouderaManagerSetupInstructions=" + clouderaManagerSetupInstructions +
                '}';
    }
}

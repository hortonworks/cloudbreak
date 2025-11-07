package com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Deserializer;
import com.sequenceiq.cloudbreak.structuredevent.json.Base64Serializer;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.doc.FreeIpaModelDescriptions;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MitTrustSetupCommands {
    @Schema(description = FreeIpaModelDescriptions.CrossRealmTrustModelDescriptions.MIT_TRUST_SETUP_COMMANDS, requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonSerialize(using = Base64Serializer.class)
    @JsonDeserialize(using = Base64Deserializer.class)
    private String kdcCommands;

    @Schema(description = FreeIpaModelDescriptions.CrossRealmTrustModelDescriptions.DNS_SETUP_INSTRUCTIONS, requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonSerialize(using = Base64Serializer.class)
    @JsonDeserialize(using = Base64Deserializer.class)
    private String dnsSetupInstructions;

    public String getKdcCommands() {
        return kdcCommands;
    }

    public void setKdcCommands(String kdcCommands) {
        this.kdcCommands = kdcCommands;
    }

    public String getDnsSetupInstructions() {
        return dnsSetupInstructions;
    }

    public void setDnsSetupInstructions(String dnsSetupInstructions) {
        this.dnsSetupInstructions = dnsSetupInstructions;
    }

    @Override
    public String toString() {
        return "MitTrustSetupCommands{" +
                "dnsSetupInstructions='" + dnsSetupInstructions + '\'' +
                '}';
    }
}

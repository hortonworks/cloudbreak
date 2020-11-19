package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.annotations.SerializedName;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FullNodeResponse {

    @JsonProperty("jid")
    @SerializedName("jid")
    private String jid;

    @JsonProperty("retcode")
    @SerializedName("retcode")
    private int retcode;

    @JsonProperty("ret")
    @SerializedName("ret")
    private JsonNode ret;

    public String getJid() {
        return jid;
    }

    public void setJid(String jid) {
        this.jid = jid;
    }

    public int getRetcode() {
        return retcode;
    }

    public void setRetcode(int retcode) {
        this.retcode = retcode;
    }

    public JsonNode getRet() {
        return ret;
    }

    public void setRet(JsonNode ret) {
        this.ret = ret;
    }

    @Override
    public String toString() {
        return "FullResponse{" +
                "jid='" + jid + '\'' +
                ", retcode=" + retcode +
                ", ret=" + ret +
                '}';
    }
}

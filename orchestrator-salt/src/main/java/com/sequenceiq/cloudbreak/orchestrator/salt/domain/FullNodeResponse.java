package com.sequenceiq.cloudbreak.orchestrator.salt.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FullNodeResponse {

    @JsonProperty("jid")
    private String jid;

    @JsonProperty("retcode")
    private int retcode;

    @JsonProperty("ret")
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

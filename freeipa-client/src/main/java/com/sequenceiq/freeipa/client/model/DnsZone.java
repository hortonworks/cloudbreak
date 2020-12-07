package com.sequenceiq.freeipa.client.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sequenceiq.freeipa.client.deserializer.ListFlatteningDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DnsZone {
    /**
     * On FreeIPA DNS zone (name) always ends with a '.'
     * eg. ipaserver0.test.xcu2-8y8x.wl.cloudera.site.
     */
    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private String idnsname;

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private Boolean idnszoneactive;

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private String idnssoamname;

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private String idnssoarname;

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private Integer idnssoaserial;

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private Integer idnssoarefresh;

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private Integer idnssoaretry;

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private Integer idnssoaexpire;

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private Integer idnssoaminimum;

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private String idnsallowquery;

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private String idnsallowtransfer;

    public String getIdnsname() {
        return idnsname;
    }

    public void setIdnsname(String idnsname) {
        this.idnsname = idnsname;
    }

    public Boolean getIdnszoneactive() {
        return idnszoneactive;
    }

    public void setIdnszoneactive(Boolean idnszoneactive) {
        this.idnszoneactive = idnszoneactive;
    }

    public void setIdnszoneactive(String idnszoneactive) {
        this.idnszoneactive = Boolean.valueOf(idnszoneactive);
    }

    public String getIdnssoamname() {
        return idnssoamname;
    }

    public void setIdnssoamname(String idnssoamname) {
        this.idnssoamname = idnssoamname;
    }

    public String getIdnssoarname() {
        return idnssoarname;
    }

    public void setIdnssoarname(String idnssoarname) {
        this.idnssoarname = idnssoarname;
    }

    public Integer getIdnssoaserial() {
        return idnssoaserial;
    }

    public void setIdnssoaserial(Integer idnssoaserial) {
        this.idnssoaserial = idnssoaserial;
    }

    public Integer getIdnssoarefresh() {
        return idnssoarefresh;
    }

    public void setIdnssoarefresh(Integer idnssoarefresh) {
        this.idnssoarefresh = idnssoarefresh;
    }

    public Integer getIdnssoaretry() {
        return idnssoaretry;
    }

    public void setIdnssoaretry(Integer idnssoaretry) {
        this.idnssoaretry = idnssoaretry;
    }

    public Integer getIdnssoaexpire() {
        return idnssoaexpire;
    }

    public void setIdnssoaexpire(Integer idnssoaexpire) {
        this.idnssoaexpire = idnssoaexpire;
    }

    public Integer getIdnssoaminimum() {
        return idnssoaminimum;
    }

    public void setIdnssoaminimum(Integer idnssoaminimum) {
        this.idnssoaminimum = idnssoaminimum;
    }

    public String getIdnsallowquery() {
        return idnsallowquery;
    }

    public void setIdnsallowquery(String idnsallowquery) {
        this.idnsallowquery = idnsallowquery;
    }

    public String getIdnsallowtransfer() {
        return idnsallowtransfer;
    }

    public void setIdnsallowtransfer(String idnsallowtransfer) {
        this.idnsallowtransfer = idnsallowtransfer;
    }

    @Override
    public String toString() {
        return "DnsZone{"
                + "idnsname='" + idnsname + '\''
                + ", idnszoneactive=" + idnszoneactive
                + ", idnssoamname='" + idnssoamname + '\''
                + ", idnssoarname='" + idnssoarname + '\''
                + ", idnssoaserial=" + idnssoaserial
                + ", idnssoarefresh=" + idnssoarefresh
                + ", idnssoaretry=" + idnssoaretry
                + ", idnssoaexpire=" + idnssoaexpire
                + ", idnssoaminimum=" + idnssoaminimum
                + ", idnsallowquery=" + idnsallowquery
                + ", idnsallowtransfer='" + idnsallowtransfer + '\''
                + '}';
    }
}

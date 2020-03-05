package com.sequenceiq.freeipa.client.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.sequenceiq.freeipa.client.deserializer.ListFlatteningDeserializer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DnsRecord {

    @JsonDeserialize(using = ListFlatteningDeserializer.class)
    private String idnsname;

    private List<String> arecord;

    private List<String> sshfprecord;

    private List<String> ptrrecord;

    public String getIdnsname() {
        return idnsname;
    }

    public void setIdnsname(String idnsname) {
        this.idnsname = idnsname;
    }

    public List<String> getArecord() {
        return arecord;
    }

    public void setArecord(List<String> arecord) {
        this.arecord = arecord;
    }

    public List<String> getSshfprecord() {
        return sshfprecord;
    }

    public void setSshfprecord(List<String> sshfprecord) {
        this.sshfprecord = sshfprecord;
    }

    public List<String> getPtrrecord() {
        return ptrrecord;
    }

    public void setPtrrecord(List<String> ptrrecord) {
        this.ptrrecord = ptrrecord;
    }

    public boolean isHostRelatedRecord(String fqdn, String domain) {
        if (isARecord()) {
            String hostname = StringUtils.substringBefore(fqdn, domain);
            return idnsname.equalsIgnoreCase(StringUtils.removeEnd(hostname, "."));
        }
        if (isPtrRecord()) {
            return ptrrecord.contains(StringUtils.appendIfMissing(fqdn, "."));
        }
        return false;
    }

    public boolean isIpRelatedRecord(String ip, String zone) {
        if (isARecord()) {
            return arecord.stream().anyMatch(record -> record.equals(ip));
        } else if (isPtrRecord()) {
            return isIpRelatedPtrRecord(ip, zone);
        } else {
            return false;
        }
    }

    private boolean isIpRelatedPtrRecord(String ip, String zone) {
        String ipStart = StringUtils.removeEnd(zone, ".in-addr.arpa.");
        String reversedIp = idnsname + '.' + ipStart;
        List<String> ipParts = Arrays.asList(reversedIp.split("\\."));
        Collections.reverse(ipParts);
        String recordIp = StringUtils.joinWith(".", ipParts.toArray(new String[ipParts.size()]));
        return ip.equals(recordIp);
    }

    public boolean isARecord() {
        return arecord != null && !arecord.isEmpty();
    }

    public boolean isPtrRecord() {
        return ptrrecord != null && !ptrrecord.isEmpty();
    }

    @Override
    public String toString() {
        return "DnsRecord{"
                + "idnsname='" + idnsname + '\''
                + ", arecord=" + arecord
                + ", sshfprecord=" + sshfprecord
                + ", ptrrecord=" + ptrrecord
                + '}';
    }
}

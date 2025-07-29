package com.sequenceiq.freeipa.client.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    private List<String> srvrecord;

    private List<String> cnamerecord;

    private List<String> nsrecord;

    private String dn;

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

    public List<String> getSrvrecord() {
        return srvrecord;
    }

    public void setPtrrecord(List<String> ptrrecord) {
        this.ptrrecord = ptrrecord;
    }

    public void setSrvrecord(List<String> srvrecord) {
        this.srvrecord = srvrecord;
    }

    public List<String> getCnamerecord() {
        return cnamerecord;
    }

    public void setCnamerecord(List<String> cnamerecord) {
        this.cnamerecord = cnamerecord;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    public List<String> getNsrecord() {
        return nsrecord;
    }

    public void setNsrecord(List<String> nsrecord) {
        this.nsrecord = nsrecord;
    }

    public boolean isHostRelatedRecord(String fqdn, String domain) {
        String hostname = StringUtils.substringBefore(fqdn, domain);
        if (isARecord()) {
            return idnsname.equalsIgnoreCase(StringUtils.removeEnd(hostname, "."));
        }
        if (isPtrRecord()) {
            return ptrrecord.contains(StringUtils.appendIfMissing(fqdn, "."));
        }
        if (isSshfpRecord()) {
            return idnsname.equalsIgnoreCase(StringUtils.removeEnd(hostname, "."));
        }
        return false;
    }

    public boolean isHostRelatedSrvRecord(String fqdn) {
        if (isSrvRecord()) {
            String searchString = " " + StringUtils.appendIfMissing(fqdn, ".");
            return srvrecord.stream()
                    .anyMatch(record -> record.endsWith(searchString));
        }
        return false;
    }

    public List<String> getHostRelatedSrvRecords(String fqdn) {
        if (isSrvRecord()) {
            return srvrecord.stream()
                    .filter(str -> str.contains(StringUtils.appendIfMissing(fqdn, ".")))
                    .collect(Collectors.toList());
        }
        return List.of();
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
        String recordIp = StringUtils.joinWith(".", (Object[]) ipParts.toArray(new String[ipParts.size()]));
        return ip.equals(recordIp);
    }

    public boolean isARecord() {
        return arecord != null && !arecord.isEmpty();
    }

    public boolean isPtrRecord() {
        return ptrrecord != null && !ptrrecord.isEmpty();
    }

    public boolean isSshfpRecord() {
        return sshfprecord != null && !sshfprecord.isEmpty();
    }

    public boolean isSrvRecord() {
        return srvrecord != null && !srvrecord.isEmpty();
    }

    public boolean isCnameRecord() {
        return cnamerecord != null && !cnamerecord.isEmpty();
    }

    public boolean isNsRecord() {
        return "@".equals(idnsname) && nsrecord != null && !nsrecord.isEmpty();
    }

    /**
     * This is how a dn looks like for an nsrecord. This give back the zone
     * "dn": "idnsname=191.84.10.in-addr.arpa.,cn=dns,dc=hybrid,dc=xcu2-8y8x,dc=wl,dc=cloudera,dc=site"
     *
     * @return 191.84.10.in-addr.arpa.
     */
    public Optional<String> calcZoneFromNsRecord() {
        if (isNsRecord()) {
            String recordWithoutLdapParts = StringUtils.substringBefore(dn, ",");
            return Optional.ofNullable(StringUtils.removeStart(recordWithoutLdapParts, "idnsname="));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return "DnsRecord{" +
                "idnsname='" + idnsname + '\'' +
                ", arecord=" + arecord +
                ", sshfprecord=" + sshfprecord +
                ", ptrrecord=" + ptrrecord +
                ", srvrecord=" + srvrecord +
                ", cnamerecord=" + cnamerecord +
                ", nsrecord=" + nsrecord +
                ", dn='" + dn + '\'' +
                '}';
    }
}

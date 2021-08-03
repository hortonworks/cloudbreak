package com.sequenceiq.cloudbreak.clusterproxy;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ReadConfigResponse {

    private String crn;

    private List<String> aliases;

    private String uriOfKnox;

    private List<ReadConfigService> services;

    public String getCrn() {
        return crn;
    }

    public void setCrn(String crn) {
        this.crn = crn;
    }

    public List<ReadConfigService> getServices() {
        return services;
    }

    public void setServices(List<ReadConfigService> services) {
        this.services = services;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases = aliases;
    }

    public String getUriOfKnox() {
        return uriOfKnox;
    }

    public void setUriOfKnox(String uriOfKnox) {
        this.uriOfKnox = uriOfKnox;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ReadConfigResponse that = (ReadConfigResponse) o;
        return Objects.equals(crn, that.crn) &&
                Objects.equals(aliases, that.aliases) &&
                Objects.equals(uriOfKnox, that.uriOfKnox) &&
                Objects.equals(services, that.services);
    }

    @Override
    public int hashCode() {
        return Objects.hash(crn, aliases, uriOfKnox, services);
    }

    @Override
    public String toString() {
        return "ReadConfigResponse{" +
                "crn='" + crn + '\'' +
                ", aliases=" + aliases +
                ", uriOfKnox='" + uriOfKnox + '\'' +
                ", services=" + services +
                '}';
    }

    public String toHumanReadableString() {
        return "ClusterProxy for crn: [" + crn + "], " +
                "services: [" + services.stream().map(ReadConfigService::toHumanReadableString).collect(Collectors.joining(", ")) + "], " +
                "aliases: [" + aliases + "], " +
                "Knox URI: [" + uriOfKnox +  "]";
    }
}

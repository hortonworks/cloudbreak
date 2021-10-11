package com.sequenceiq.freeipa.entity;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.converter.OutboundInternetTrafficConverter;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;

@Entity
public class Network {
    private static final String DELIMITER = ",";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "network_generator")
    @SequenceGenerator(name = "network_generator", sequenceName = "network_id_seq", allocationSize = 1)
    private Long id;

    private String name;

    private String cloudPlatform;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json attributes;

    @Column(nullable = false)
    @Convert(converter = OutboundInternetTrafficConverter.class)
    private OutboundInternetTraffic outboundInternetTraffic = OutboundInternetTraffic.ENABLED;

    private String networkCidrs;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String cloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public Json getAttributes() {
        return attributes;
    }

    public void setAttributes(Json attributes) {
        this.attributes = attributes;
    }

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public OutboundInternetTraffic getOutboundInternetTraffic() {
        return outboundInternetTraffic;
    }

    public void setOutboundInternetTraffic(OutboundInternetTraffic outboundInternetTraffic) {
        this.outboundInternetTraffic = outboundInternetTraffic;
    }

    public List<String> getNetworkCidrs() {
        return StringUtils.isNotEmpty(networkCidrs) ? Stream.of(networkCidrs.split(DELIMITER)).collect(Collectors.toList()) : List.of();
    }

    public void setNetworkCidrs(Collection<String> networkCidrs) {
        if (CollectionUtils.isNotEmpty(networkCidrs)) {
            this.networkCidrs = String.join(DELIMITER, networkCidrs);
        } else {
            this.networkCidrs = null;
        }
    }

    @Override
    public String toString() {
        return "Network{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", cloudPlatform='" + cloudPlatform + '\'' +
                ", attributes=" + attributes +
                ", outboundInternetTraffic=" + outboundInternetTraffic +
                ", networkCidrs='" + networkCidrs + '\'' +
                '}';
    }
}

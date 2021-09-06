package com.sequenceiq.freeipa.entity;

import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;

@Entity
public class SecurityRule {

    public static final String ICMP = "icmp";

    private static final String PORT_DELIMITER = ",";

    private static final String[] EMPTY_PORTS = new String[0];

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "securityrule_generator")
    @SequenceGenerator(name = "securityrule_generator", sequenceName = "securityrule_id_seq", allocationSize = 1)
    private Long id;

    @ManyToOne
    private SecurityGroup securityGroup;

    private String cidr;

    private String ports;

    private String protocol;

    private boolean modifiable;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCidr() {
        return cidr;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }

    public SecurityGroup getSecurityGroup() {
        return securityGroup;
    }

    public void setSecurityGroup(SecurityGroup securityGroup) {
        this.securityGroup = securityGroup;
    }

    public String[] getPorts() {
        if (ICMP.equalsIgnoreCase(protocol)) {
            return EMPTY_PORTS;
        }
        return ports.split(PORT_DELIMITER);
    }

    public void setPorts(String ports) {
        this.ports = ports;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public boolean isModifiable() {
        return modifiable;
    }

    public void setModifiable(boolean modifiable) {
        this.modifiable = modifiable;
    }

    @Override
    public String toString() {
        return "SecurityRule{"
                + "id=" + id
                + ", securityGroup=" + securityGroup
                + ", cidr='" + cidr + '\''
                + ", ports='" + ports + '\''
                + ", protocol='" + protocol + '\''
                + ", modifiable=" + modifiable
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        } else {
            SecurityRule that = (SecurityRule) o;
            return modifiable == that.modifiable && Objects.equals(id, that.id) && Objects.equals(cidr, that.cidr) && Objects.equals(ports, that.ports)
                    && Objects.equals(protocol, that.protocol);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, cidr, ports, protocol, modifiable);
    }
}

package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;

@Entity
@NamedQueries({
        @NamedQuery(
                name = "SecurityGroup.findAllBySecurityGroupId",
                query = "SELECT r FROM SecurityRule r "
                        + "WHERE r.securityGroup.id= :securityGroupId")
})
public class SecurityRule implements ProvisionEntity {
    private static final String PORT_DELIMITER = ",";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "security_rule_sequence_generator")
    @SequenceGenerator(name = "security_rule_sequence_generator", sequenceName = "security_rule_seq")
    private Long id;
    @ManyToOne
    private SecurityGroup securityGroup;
    private String cidr;
    private String ports;
    private String protocol;
    private boolean modifiable;

    public SecurityRule() { }

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
}

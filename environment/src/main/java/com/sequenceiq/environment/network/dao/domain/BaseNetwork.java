package com.sequenceiq.environment.network.dao.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import org.hibernate.annotations.Where;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.converter.LoadBalancerCreationConverter;
import com.sequenceiq.cloudbreak.converter.OutboundInternetTrafficConverter;
import com.sequenceiq.cloudbreak.converter.PublicEndpointAccessGatewayConverter;
import com.sequenceiq.common.api.type.LoadBalancerCreation;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.common.api.type.ServiceEndpointCreation;
import com.sequenceiq.environment.api.v1.environment.model.base.PrivateSubnetCreation;
import com.sequenceiq.environment.environment.domain.EnvironmentAwareResource;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.network.dao.domain.converter.ServiceEndpointCreationConverter;
import com.sequenceiq.environment.parameters.dao.converter.PrivateSubnetCreationConverter;
import com.sequenceiq.environment.parameters.dao.converter.RegistrationTypeConverter;

@Entity
@Where(clause = "archived = false")
@Table(name = "environment_network")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "network_platform")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonSubTypes({ @JsonSubTypes.Type(value = AwsNetwork.class, name = "awsNetwork"),
        @JsonSubTypes.Type(value = AzureNetwork.class, name = "azureNetwork"),
        @JsonSubTypes.Type(value = GcpNetwork.class, name = "gcpNetwork"),
        @JsonSubTypes.Type(value = MockNetwork.class, name = "mockNetwork"),
        @JsonSubTypes.Type(value = YarnNetwork.class, name = "yarnNetwork") })
public abstract class BaseNetwork implements EnvironmentAwareResource {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "environment_network_generator")
    @SequenceGenerator(name = "environment_network_generator", sequenceName = "environment_network_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @OneToOne
    @JsonIgnore
    @JoinColumn(nullable = false)
    private EnvironmentView environment;

    private boolean archived;

    private Long deletionTimestamp = -1L;

    private String networkCidr;

    private String networkCidrs;

    @Convert(converter = RegistrationTypeConverter.class)
    private RegistrationType registrationType;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT", nullable = false)
    private Json subnetMetas;

    @Column(nullable = false)
    @Convert(converter = PrivateSubnetCreationConverter.class)
    private PrivateSubnetCreation privateSubnetCreation;

    @Column(nullable = false)
    @Convert(converter = ServiceEndpointCreationConverter.class)
    private ServiceEndpointCreation serviceEndpointCreation;

    @Convert(converter = PublicEndpointAccessGatewayConverter.class)
    private PublicEndpointAccessGateway publicEndpointAccessGateway;

    @Convert(converter = LoadBalancerCreationConverter.class)
    private LoadBalancerCreation loadBalancerCreation = LoadBalancerCreation.ENABLED;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json endpointGatewaySubnetMetas = new Json(Map.of());

    @Column(nullable = false)
    @Convert(converter = OutboundInternetTrafficConverter.class)
    private OutboundInternetTraffic outboundInternetTraffic = OutboundInternetTraffic.ENABLED;

    @Column(nullable = false)
    private String accountId;

    @Column(nullable = false)
    private String resourceCrn;

    public BaseNetwork() {
        subnetMetas = new Json(new HashMap<String, CloudSubnet>());
    }

    public EnvironmentView getEnvironment() {
        return environment;
    }

    public void setEnvironment(EnvironmentView environment) {
        this.environment = environment;
    }

    @Override
    @JsonIgnore
    public Set<EnvironmentView> getEnvironments() {
        Set<EnvironmentView> environmentViews = new HashSet<>();
        environmentViews.add(environment);
        return environmentViews;
    }

    @Override
    @JsonIgnore
    public void setEnvironments(Set<EnvironmentView> environments) {
        if (environments.size() != 1) {
            throw new IllegalArgumentException("Environment set size cannot differ from 1.");
        }
        this.environment = environments.iterator().next();
    }

    @Override
    public String getAccountId() {
        return accountId;
    }

    @Override
    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public void setDeletionTimestamp(Long timestampMillisecs) {
        deletionTimestamp = timestampMillisecs;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getNetworkCidr() {
        return networkCidr;
    }

    public void setNetworkCidr(String networkCidr) {
        this.networkCidr = networkCidr;
    }

    public String getNetworkCidrs() {
        return networkCidrs;
    }

    public void setNetworkCidrs(String networkCidrs) {
        this.networkCidrs = networkCidrs;
    }

    public RegistrationType getRegistrationType() {
        return registrationType;
    }

    public void setRegistrationType(RegistrationType registrationType) {
        this.registrationType = registrationType;
    }

    public void setSubnetMetas(Map<String, CloudSubnet> subnetMetas) {
        this.subnetMetas = new Json(subnetMetas);
    }

    public Map<String, CloudSubnet> getSubnetMetas() {
        return JsonUtil.jsonToType(subnetMetas.getValue(), new TypeReference<>() {
        });
    }

    public PublicEndpointAccessGateway getPublicEndpointAccessGateway() {
        return publicEndpointAccessGateway;
    }

    public void setPublicEndpointAccessGateway(PublicEndpointAccessGateway publicEndpointAccessGateway) {
        this.publicEndpointAccessGateway = publicEndpointAccessGateway;
    }

    public void setEndpointGatewaySubnetMetas(Map<String, CloudSubnet> endpointGatewaySubnetMetas) {
        this.endpointGatewaySubnetMetas = new Json(endpointGatewaySubnetMetas);
    }

    public Map<String, CloudSubnet> getEndpointGatewaySubnetMetas() {
        return JsonUtil.jsonToType(endpointGatewaySubnetMetas.getValue(), new TypeReference<>() {
        });
    }

    public abstract String getNetworkId();

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public Long getDeletionTimestamp() {
        return deletionTimestamp;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PrivateSubnetCreation getPrivateSubnetCreation() {
        return privateSubnetCreation;
    }

    public void setPrivateSubnetCreation(PrivateSubnetCreation privateSubnetCreation) {
        this.privateSubnetCreation = privateSubnetCreation;
    }

    public ServiceEndpointCreation getServiceEndpointCreation() {
        return serviceEndpointCreation;
    }

    public void setServiceEndpointCreation(ServiceEndpointCreation serviceEndpointCreation) {
        this.serviceEndpointCreation = serviceEndpointCreation;
    }

    public OutboundInternetTraffic getOutboundInternetTraffic() {
        return outboundInternetTraffic;
    }

    public void setOutboundInternetTraffic(OutboundInternetTraffic outboundInternetTraffic) {
        this.outboundInternetTraffic = outboundInternetTraffic;
    }

    public LoadBalancerCreation getLoadBalancerCreation() {
        return loadBalancerCreation;
    }

    public void setLoadBalancerCreation(LoadBalancerCreation loadBalancerCreation) {
        this.loadBalancerCreation = loadBalancerCreation;
    }

    @Override
    public String getResourceCrn() {
        return resourceCrn;
    }

    @Override
    public void setResourceCrn(String resourceCrn) {
        this.resourceCrn = resourceCrn;
    }

    @Override
    public String toString() {
        return "BaseNetwork{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", environment=" + environment.getName() +
                ", archived=" + archived +
                ", deletionTimestamp=" + deletionTimestamp +
                ", networkCidr='" + networkCidr + '\'' +
                ", networkCidrs='" + networkCidrs + '\'' +
                ", registrationType=" + registrationType +
                ", subnetMetas=" + subnetMetas +
                ", privateSubnetCreation=" + privateSubnetCreation +
                ", serviceEndpointCreation=" + serviceEndpointCreation +
                ", publicEndpointAccessGateway=" + publicEndpointAccessGateway +
                ", loadBalancerCreation=" + loadBalancerCreation +
                ", endpointGatewaySubnetMetas=" + endpointGatewaySubnetMetas +
                ", outboundInternetTraffic=" + outboundInternetTraffic +
                ", accountId='" + accountId + '\'' +
                ", resourceCrn='" + resourceCrn + '\'' +
                '}';
    }
}

package com.sequenceiq.cloudbreak.converter;

import static com.sequenceiq.cloudbreak.api.model.Status.REQUESTED;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.ClusterRequest;
import com.sequenceiq.cloudbreak.api.model.FileSystemBase;
import com.sequenceiq.cloudbreak.api.model.KerberosRequest;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.FileSystem;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;

@Component
public class JsonToClusterConverter extends AbstractConversionServiceAwareConverter<ClusterRequest, Cluster> {
    @Override
    public Cluster convert(ClusterRequest source) {
        Cluster cluster = new Cluster();
        cluster.setName(source.getName());
        cluster.setStatus(REQUESTED);
        cluster.setDescription(source.getDescription());
        cluster.setEmailNeeded(source.getEmailNeeded());
        cluster.setUserName(source.getUserName());
        cluster.setPassword(source.getPassword());
        Boolean enableSecurity = source.getEnableSecurity();
        cluster.setSecure(enableSecurity == null ? false : enableSecurity);
        KerberosRequest kerberos = source.getKerberos();
        KerberosConfig kerberosConfig = new KerberosConfig();
        if (source.getKerberos() != null) {
            kerberosConfig.setKerberosMasterKey(kerberos.getMasterKey());
            kerberosConfig.setKerberosAdmin(kerberos.getAdmin());
            kerberosConfig.setKerberosPassword(kerberos.getPassword());
            kerberosConfig.setKerberosUrl(kerberos.getUrl());
            kerberosConfig.setKerberosRealm(kerberos.getRealm());
            kerberosConfig.setKerberosTcpAllowed(kerberos.getTcpAllowed());
            kerberosConfig.setKerberosPrincipal(kerberos.getPrincipal());
            kerberosConfig.setKerberosLdapUrl(kerberos.getLdapUrl());
            kerberosConfig.setKerberosContainerDn(kerberos.getContainerDn());
        }
        cluster.setKerberosConfig(kerberosConfig);
        cluster.setLdapRequired(source.getLdapRequired());
        cluster.setConfigStrategy(source.getConfigStrategy());
        cluster.setEnableShipyard(source.getEnableShipyard());
        cluster.setEmailTo(source.getEmailTo());
        FileSystemBase fileSystem = source.getFileSystem();
        if (fileSystem != null) {
            cluster.setFileSystem(getConversionService().convert(fileSystem, FileSystem.class));
        }
        return cluster;
    }
}

package com.sequenceiq.cloudbreak.converter.util;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.SSOType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.gateway.GatewayV4Request;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.client.PkiUtil;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.Gateway;
import com.sequenceiq.cloudbreak.domain.stack.cluster.gateway.GatewayTopology;
import com.sequenceiq.cloudbreak.util.PasswordUtil;

@Component
public class GatewayConvertUtil {

    @Inject
    private ConverterUtil converterUtil;

    public void setTopologies(GatewayV4Request source, Gateway gateway) {
        if (!CollectionUtils.isEmpty(source.getTopologies())) {
            Set<GatewayTopology> gatewayTopologies = source.getTopologies().stream()
                    .map(g -> converterUtil.convert(g, GatewayTopology.class))
                    .collect(Collectors.toSet());
            gateway.setTopologies(gatewayTopologies);
            gatewayTopologies.forEach(g -> g.setGateway(gateway));
        }
    }

    public void setGatewayPathAndSsoProvider(GatewayV4Request source, Gateway gateway) {
        if (source.getPath() != null) {
            gateway.setPath(source.getPath());
        }
        if (gateway.getSsoProvider() == null) {
            gateway.setSsoProvider('/' + gateway.getPath() + "/sso/api/v1/websso");
        }
    }

    public void setBasicProperties(GatewayV4Request source, Gateway gateway) {
        if (source.getGatewayType() != null) {
            gateway.setGatewayType(source.getGatewayType());
        }
        gateway.setSsoType(source.getSsoType() != null ? source.getSsoType() : SSOType.NONE);
        gateway.setTokenCert(source.getTokenCert());
        gateway.setKnoxMasterSecret(PasswordUtil.generatePassword());
    }

    public void generateSignKeys(Gateway gateway) {
        if (gateway != null) {
            if (gateway.getSignCert() == null) {
                KeyPair identityKey = PkiUtil.generateKeypair();
                KeyPair signKey = PkiUtil.generateKeypair();
                X509Certificate cert = PkiUtil.cert(identityKey, "signing", signKey);

                gateway.setSignKey(PkiUtil.convert(identityKey.getPrivate()));
                gateway.setSignPub(PkiUtil.convert(identityKey.getPublic()));
                gateway.setSignCert(PkiUtil.convert(cert));
            }
        }
    }
}

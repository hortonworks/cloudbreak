package com.sequenceiq.notification.sender;

import static com.sequenceiq.notification.domain.ChannelType.LOCAL_EMAIL;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.notification.domain.ChannelType;

@Service
public class LocalEmailProvider {

    @Value("${notification.local.email.fromAddress:}")
    private String emailFromAddress;

    @Value("${notification.local.email.toAddress:}")
    private String emailToAddress;

    @Value("${notification.local.email.secret:}")
    private String emailApplicationSecret;

    public boolean emailSendingConfigured() {
        return StringUtils.isNoneBlank(emailFromAddress)
                && StringUtils.isNoneBlank(emailToAddress)
                && StringUtils.isNoneBlank(emailApplicationSecret);
    }

    public Set<ChannelType> getLocalChannelIfConfigured(Set<ChannelType> channelTypes) {
        if (emailSendingConfigured()) {
            return Set.of(LOCAL_EMAIL);
        }
        return channelTypes;
    }

    public String getEmailFromAddress() {
        return emailFromAddress;
    }

    public String getEmailToAddress() {
        return emailToAddress;
    }

    public String getEmailApplicationSecret() {
        return emailApplicationSecret;
    }
}

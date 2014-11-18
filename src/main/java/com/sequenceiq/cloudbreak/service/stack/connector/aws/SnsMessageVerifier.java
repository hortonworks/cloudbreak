package com.sequenceiq.cloudbreak.service.stack.connector.aws;

import java.io.InputStream;
import java.net.URL;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.apache.commons.codec.binary.Base64;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.SnsRequest;

@Service
public class SnsMessageVerifier {

    public void verifyMessageSignature(SnsRequest snsRequest) {
        if (!"1".equals(snsRequest.getSignatureVersion())) {
            throw new SnsMessageInvalidException("Signature version must be '1'.");
        }
        try {
            URL url = new URL(snsRequest.getSigningCertURL());
            InputStream inStream = url.openStream();
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) cf.generateCertificate(inStream);
            inStream.close();

            Signature sig = Signature.getInstance("SHA1withRSA");
            sig.initVerify(cert.getPublicKey());
            sig.update(getMessageBytesToSign(snsRequest));
            if (!sig.verify(Base64.decodeBase64(snsRequest.getSignature()))) {
                throw new SnsMessageInvalidException("Failed to verify SNS message signature.");
            }
        } catch (Exception e) {
            throw new SnsMessageInvalidException(e.getMessage(), e);
        }
    }

    /**
     * These helper methods are copied from http://docs.aws.amazon.com/sns/latest/dg/SendMessageToHttp.example.java.html
     */
    private static byte[] getMessageBytesToSign(SnsRequest msg) {
        byte[] bytesToSign = null;
        if ("Notification".equals(msg.getType())) {
            bytesToSign = buildNotificationStringToSign(msg).getBytes();
        } else if ("SubscriptionConfirmation".equals(msg.getType()) || "UnsubscribeConfirmation".equals(msg.getType())) {
            bytesToSign = buildSubscriptionStringToSign(msg).getBytes();
        }
        return bytesToSign;
    }

    private static String buildNotificationStringToSign(SnsRequest msg) {
        StringBuilder stringBuilder = new StringBuilder()
                .append("Message\n")
                .append(msg.getMessage() + "\n")
                .append("MessageId\n")
                .append(msg.getMessageId() + "\n");
        if (msg.getSubject() != null) {
            stringBuilder.append("Subject\n")
                    .append(msg.getSubject() + "\n");
        }
        stringBuilder.append("Timestamp\n")
                .append(msg.getTimestamp() + "\n")
                .append("TopicArn\n")
                .append(msg.getTopicArn() + "\n")
                .append("Type\n")
                .append(msg.getType() + "\n");
        return stringBuilder.toString();
    }

    private static String buildSubscriptionStringToSign(SnsRequest msg) {
        StringBuilder stringBuilder = new StringBuilder()
                .append("Message\n")
                .append(msg.getMessage() + "\n")
                .append("MessageId\n")
                .append(msg.getMessageId() + "\n")
                .append("SubscribeURL\n")
                .append(msg.getSubscribeURL() + "\n")
                .append("Timestamp\n")
                .append(msg.getTimestamp() + "\n")
                .append("Token\n")
                .append(msg.getToken() + "\n")
                .append("TopicArn\n")
                .append(msg.getTopicArn() + "\n")
                .append("Type\n")
                .append(msg.getType() + "\n");
        return stringBuilder.toString();
    }

}

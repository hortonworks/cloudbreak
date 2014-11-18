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
        String stringToSign = null;
        stringToSign = "Message\n";
        stringToSign += msg.getMessage() + "\n";
        stringToSign += "MessageId\n";
        stringToSign += msg.getMessageId() + "\n";
        if (msg.getSubject() != null) {
            stringToSign += "Subject\n";
            stringToSign += msg.getSubject() + "\n";
        }
        stringToSign += "Timestamp\n";
        stringToSign += msg.getTimestamp() + "\n";
        stringToSign += "TopicArn\n";
        stringToSign += msg.getTopicArn() + "\n";
        stringToSign += "Type\n";
        stringToSign += msg.getType() + "\n";
        return stringToSign;
    }

    private static String buildSubscriptionStringToSign(SnsRequest msg) {
        String stringToSign = null;
        stringToSign = "Message\n";
        stringToSign += msg.getMessage() + "\n";
        stringToSign += "MessageId\n";
        stringToSign += msg.getMessageId() + "\n";
        stringToSign += "SubscribeURL\n";
        stringToSign += msg.getSubscribeURL() + "\n";
        stringToSign += "Timestamp\n";
        stringToSign += msg.getTimestamp() + "\n";
        stringToSign += "Token\n";
        stringToSign += msg.getToken() + "\n";
        stringToSign += "TopicArn\n";
        stringToSign += msg.getTopicArn() + "\n";
        stringToSign += "Type\n";
        stringToSign += msg.getType() + "\n";
        return stringToSign;
    }

}

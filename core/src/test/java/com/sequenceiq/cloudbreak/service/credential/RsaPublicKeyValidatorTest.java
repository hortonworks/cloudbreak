package com.sequenceiq.cloudbreak.service.credential;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.AwsCredential;
import com.sequenceiq.cloudbreak.domain.Credential;

public class RsaPublicKeyValidatorTest {

    private RsaPublicKeyValidator rsaPublicKeyValidator;

    @Before
    public void setUp() {
        rsaPublicKeyValidator = new RsaPublicKeyValidator();
    }

    @Test
    public void validPublicKeyWillNotFail() {
        rsaPublicKeyValidator.validate(
                azureCredential(
                        "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCasJyap4swb4Hk4xOlnF3OmKVwzmv2e053yrtvcUPaxCeboSltOBReuT"
                                + "QxX+kYCgKCdtEwpIvEDXk16T6nCI4tSptAalFgpUWn+JOysCuLuWnwrk6mSKOzEiPYCrB54444mDY6rbBDSRuE/V"
                                + "UYQ/yi0imocARlOiFdPRlZGTN0XGE1V8LSo+m0oIzTwBKn58I4v5iB4ZUL/6adGXo7dgdBh/Fmm4uYbgrCZnL1EaK"
                                + "pMxSG76XWhuzFpHjLkRndz88ha0rB6davag6nZGdno5IepLAWg9oB4jTApHwhN2j1rWLN2y1c+pTxsF6LxBiN5rsY"
                                + "KR495VFmuOepLYz5I8Dn sequence-eu")
        );
    }

    public void inValidPublicKeyWillCorrectWhenMissingUser() {
        rsaPublicKeyValidator.validate(
                azureCredential(
                        "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCasJyap4swb4Hk4xOlnF3OmKVwzmv2e053yrtvcUPaxCeboSltOBReuT"
                                + "QxX+kYCgKCdtEwpIvEDXk16T6nCI4tSptAalFgpUWn+JOysCuLuWnwrk6mSKOzEiPYCrB54444mDY6rbBDSRuE/V"
                                + "UYQ/yi0imocARlOiFdPRlZGTN0XGE1V8LSo+m0oIzTwBKn58I4v5iB4ZUL/6adGXo7dgdBh/Fmm4uYbgrCZnL1EaK"
                                + "pMxSG76XWhuzFpHjLkRndz88ha0rB6davag6nZGdno5IepLAWg9oB4jTApHwhN2j1rWLN2y1c+pTxsF6LxBiN5rsY"
                                + "KR495VFmuOepLYz5I8Dn")
        );
    }

    @Test(expected = BadRequestException.class)
    public void inValidPublicKeyWillFailWhenMissingSshRsa() {
        rsaPublicKeyValidator.validate(
                azureCredential(
                        "AAAAB3NzaC1yc2EAAAADAQABAAABAQCasJyap4swb4Hk4xOlnF3OmKVwzmv2e053yrtvcUPaxCeboSltOBReuT"
                                + "QxX+kYCgKCdtEwpIvEDXk16T6nCI4tSptAalFgpUWn+JOysCuLuWnwrk6mSKOzEiPYCrB54444mDY6rbBDSRuE/V"
                                + "UYQ/yi0imocARlOiFdPRlZGTN0XGE1V8LSo+m0oIzTwBKn58I4v5iB4ZUL/6adGXo7dgdBh/Fmm4uYbgrCZnL1EaK"
                                + "pMxSG76XWhuzFpHjLkRndz88ha0rB6davag6nZGdno5IepLAWg9oB4jTApHwhN2j1rWLN2y1c+pTxsF6LxBiN5rsY"
                                + "KR495VFmuOepLYz5I8Dn sequence-eu")
        );
    }

    @Test(expected = BadRequestException.class)
    public void inValidPublicKeyWillFailWhenTooShortBody() {
        rsaPublicKeyValidator.validate(
                azureCredential(
                        "ssh-rsa AAAA sequence-eu")
        );
    }

    @Test(expected = BadRequestException.class)
    public void inValidPublicKeyWillFailWhenMissingUserAndSshRsa() {
        rsaPublicKeyValidator.validate(
                azureCredential(
                        "AAAAB3NzaC1yc2EAAAADAQABAAABAQCasJyap4swb4Hk4xOlnF3OmKVwzmv2e053yrtvcUPaxCeboSltOBReuT"
                                + "QxX+kYCgKCdtEwpIvEDXk16T6nCI4tSptAalFgpUWn+JOysCuLuWnwrk6mSKOzEiPYCrB54444mDY6rbBDSRuE/V"
                                + "UYQ/yi0imocARlOiFdPRlZGTN0XGE1V8LSo+m0oIzTwBKn58I4v5iB4ZUL/6adGXo7dgdBh/Fmm4uYbgrCZnL1EaK"
                                + "pMxSG76XWhuzFpHjLkRndz88ha0rB6davag6nZGdno5IepLAWg9oB4jTApHwhN2j1rWLN2y1c+pTxsF6LxBiN5rsY"
                                + "KR495VFmuOepLYz5I8Dn")
        );
    }

    @Test(expected = BadRequestException.class)
    public void inValidPublicKeyWillFailWhenTotallyInvalidString() {
        rsaPublicKeyValidator.validate(
                azureCredential(
                        "ImBAD")
        );
    }

    @Test
    public void validSsh2PublicKeyWillNotFail() {
        rsaPublicKeyValidator.validate(
                azureCredential(
                        "---- BEGIN SSH2 PUBLIC KEY ----\n"
                            + "Comment: \"2048-bit RSA, converted by ricsi@Richards-MacBook-Pro.local \"\n"
                                + "AAAAB3NzaC1yc2EAAAADAQABAAABAQC0Rfl2G2vDs6yc19RxCqReunFgpYj+ucyLobpTCB\n"
                            + "tfDwzIbJot2Fmife6M42mBtiTmAK6x8kcUEeab6CB4MUzsqF7vGTFUjwWirG/XU5pYXFUB\n"
                            + "hi8xzey+KS9KVrQ+UuKJh/AN9iSQeMV+rgT1yF5+etVH+bK1/37QCKp3+mCqjFzPyQOrvk\n"
                            + "GZv4sYyRwX7BKBLleQmIVWpofpjT7BfcCxH877RzC5YMIi65aBc82Dl6tH6OEiP7mzByU5\n"
                            + "2yvH6JFuwZ/9fWj1vXCWJzxx2w0F1OU8Zwg8gNNzL+SVb9+xfBE7xBHMpYFg72hBWPh862\n"
                            + "Ce36F4NZd3MpWMSjMmpDPh\n"
                            + "---- END SSH2 PUBLIC KEY ----"
                )
        );
    }

    @Test(expected = BadRequestException.class)
    public void validSsh2PublicKeyWillNotFailWhenBeginAndEndSsh2Missing() {
        rsaPublicKeyValidator.validate(
                azureCredential(
                                "Comment: \"2048-bit RSA, converted by ricsi@Richards-MacBook-Pro.local \"\n"
                                + "AAAAB3NzaC1yc2EAAAADAQABAAABAQC0Rfl2G2vDs6yc19RxCqReunFgpYj+ucyLobpTCB\n"
                                + "tfDwzIbJot2Fmife6M42mBtiTmAK6x8kcUEeab6CB4MUzsqF7vGTFUjwWirG/XU5pYXFUB\n"
                                + "hi8xzey+KS9KVrQ+UuKJh/AN9iSQeMV+rgT1yF5+etVH+bK1/37QCKp3+mCqjFzPyQOrvk\n"
                                + "GZv4sYyRwX7BKBLleQmIVWpofpjT7BfcCxH877RzC5YMIi65aBc82Dl6tH6OEiP7mzByU5\n"
                                + "2yvH6JFuwZ/9fWj1vXCWJzxx2w0F1OU8Zwg8gNNzL+SVb9+xfBE7xBHMpYFg72hBWPh862\n"
                                + "Ce36F4NZd3MpWMSjMmpDPh\n"
                )
        );
    }

    @Test(expected = BadRequestException.class)
    public void validSsh2PublicKeyWillFailWhenKeyMissing() {
        rsaPublicKeyValidator.validate(
                azureCredential(
                                "Comment: \"2048-bit RSA, converted by ricsi@Richards-MacBook-Pro.local \"\n"
                                + "AAAAB3NzaC1yc2EAAAADAQABAAABAQC0Rfl2G2vDs6yc19RxCqReunFgpYj+ucyLobpTCB\n"
                                + "tfDwzIbJot2Fmife6M42mBtiTmAK6x8kcUEeab6CB4MUzsqF7vGTFUjwWirG/XU5pYXFUB\n"
                                + "hi8xzey+KS9KVrQ+UuKJh/AN9iSQeMV+rgT1yF5+etVH+bK1/37QCKp3+mCqjFzPyQOrvk\n"
                                + "GZv4sYyRwX7BKBLleQmIVWpofpjT7BfcCxH877RzC5YMIi65aBc82Dl6tH6OEiP7mzByU5\n"
                                + "2yvH6JFuwZ/9fWj1vXCWJzxx2w0F1OU8Zwg8gNNzL+SVb9+xfBE7xBHMpYFg72hBWPh862\n"
                                + "Ce36F4NZd3MpWMSjMmpDPh\n"
                                + "---- END SSH2 PUBLIC KEY ----"
                )
        );
    }

    private Credential azureCredential(String publicKey) {
        Credential credential = new AwsCredential();
        credential.setPublicKey(publicKey);
        return credential;
    }
}
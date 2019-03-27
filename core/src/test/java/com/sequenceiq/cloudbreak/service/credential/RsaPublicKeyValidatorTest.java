package com.sequenceiq.cloudbreak.service.credential;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;

public class RsaPublicKeyValidatorTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    private OpenSshPublicKeyValidator openSshPublicKeyValidator;

    @Before
    public void setUp() {
        openSshPublicKeyValidator = new OpenSshPublicKeyValidator();
    }

    @Test
    public void validPublicKeyWillNotFail() {
        openSshPublicKeyValidator.validate(
                        "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCasJyap4swb4Hk4xOlnF3OmKVwzmv2e053yrtvcUPaxCeboSltOBReuT"
                                + "QxX+kYCgKCdtEwpIvEDXk16T6nCI4tSptAalFgpUWn+JOysCuLuWnwrk6mSKOzEiPYCrB54444mDY6rbBDSRuE/V"
                                + "UYQ/yi0imocARlOiFdPRlZGTN0XGE1V8LSo+m0oIzTwBKn58I4v5iB4ZUL/6adGXo7dgdBh/Fmm4uYbgrCZnL1EaK"
                                + "pMxSG76XWhuzFpHjLkRndz88ha0rB6davag6nZGdno5IepLAWg9oB4jTApHwhN2j1rWLN2y1c+pTxsF6LxBiN5rsY"
                                + "KR495VFmuOepLYz5I8Dn sequence-eu"
        );
    }

    @Test
    public void inValidPublicKeyWillCorrectWhenMissingUser() {
        openSshPublicKeyValidator.validate(
                        "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCasJyap4swb4Hk4xOlnF3OmKVwzmv2e053yrtvcUPaxCeboSltOBReuT"
                                + "QxX+kYCgKCdtEwpIvEDXk16T6nCI4tSptAalFgpUWn+JOysCuLuWnwrk6mSKOzEiPYCrB54444mDY6rbBDSRuE/V"
                                + "UYQ/yi0imocARlOiFdPRlZGTN0XGE1V8LSo+m0oIzTwBKn58I4v5iB4ZUL/6adGXo7dgdBh/Fmm4uYbgrCZnL1EaK"
                                + "pMxSG76XWhuzFpHjLkRndz88ha0rB6davag6nZGdno5IepLAWg9oB4jTApHwhN2j1rWLN2y1c+pTxsF6LxBiN5rsY"
                                + "KR495VFmuOepLYz5I8Dn"
        );
    }

    @Test
    public void inValidPublicKeyWillFailWhenMissingSshRsa() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("detailed message: Corrupt or unknown public key file format");
        openSshPublicKeyValidator.validate(
                        "AAAAB3NzaC1yc2EAAAADAQABAAABAQCasJyap4swb4Hk4xOlnF3OmKVwzmv2e053yrtvcUPaxCeboSltOBReuT"
                                + "QxX+kYCgKCdtEwpIvEDXk16T6nCI4tSptAalFgpUWn+JOysCuLuWnwrk6mSKOzEiPYCrB54444mDY6rbBDSRuE/V"
                                + "UYQ/yi0imocARlOiFdPRlZGTN0XGE1V8LSo+m0oIzTwBKn58I4v5iB4ZUL/6adGXo7dgdBh/Fmm4uYbgrCZnL1EaK"
                                + "pMxSG76XWhuzFpHjLkRndz88ha0rB6davag6nZGdno5IepLAWg9oB4jTApHwhN2j1rWLN2y1c+pTxsF6LxBiN5rsY"
                                + "KR495VFmuOepLYz5I8Dn sequence-eu"
        );
    }

    @Test
    public void inValidPublicKeyWillFailWhenTooShortBody() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("detailed message: Index 3 out of bounds for length 3");
        openSshPublicKeyValidator.validate(
                        "ssh-rsa AAAA sequence-eu"
        );
    }

    @Test
    public void inValidPublicKeyWillFailWhenTotallyInvalidString() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("detailed message: Corrupt or unknown public key file format");
        openSshPublicKeyValidator.validate(
                        "ImBAD"
        );
    }

    @Test
    public void validSsh2PublicKeyWillNotFail() {
        openSshPublicKeyValidator.validate(
                        "---- BEGIN SSH2 PUBLIC KEY ----\n"
                            + "Comment: \"2048-bit RSA, converted by ricsi@Richards-MacBook-Pro.local \"\n"
                                + "AAAAB3NzaC1yc2EAAAADAQABAAABAQC0Rfl2G2vDs6yc19RxCqReunFgpYj+ucyLobpTCB\n"
                            + "tfDwzIbJot2Fmife6M42mBtiTmAK6x8kcUEeab6CB4MUzsqF7vGTFUjwWirG/XU5pYXFUB\n"
                            + "hi8xzey+KS9KVrQ+UuKJh/AN9iSQeMV+rgT1yF5+etVH+bK1/37QCKp3+mCqjFzPyQOrvk\n"
                            + "GZv4sYyRwX7BKBLleQmIVWpofpjT7BfcCxH877RzC5YMIi65aBc82Dl6tH6OEiP7mzByU5\n"
                            + "2yvH6JFuwZ/9fWj1vXCWJzxx2w0F1OU8Zwg8gNNzL+SVb9+xfBE7xBHMpYFg72hBWPh862\n"
                            + "Ce36F4NZd3MpWMSjMmpDPh\n"
                            + "---- END SSH2 PUBLIC KEY ----"
        );
    }

    @Test
    public void validSsh2PublicKeyWillFailWhenBeginAndEndSsh2Missing() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("[certificate: 'Comment: \"2048-bit RSA, converted by ricsi@Richards-MacBook-Pro.local \"");
        openSshPublicKeyValidator.validate(
                                "Comment: \"2048-bit RSA, converted by ricsi@Richards-MacBook-Pro.local \"\n"
                                + "AAAAB3NzaC1yc2EAAAADAQABAAABAQC0Rfl2G2vDs6yc19RxCqReunFgpYj+ucyLobpTCB\n"
                                + "tfDwzIbJot2Fmife6M42mBtiTmAK6x8kcUEeab6CB4MUzsqF7vGTFUjwWirG/XU5pYXFUB\n"
                                + "hi8xzey+KS9KVrQ+UuKJh/AN9iSQeMV+rgT1yF5+etVH+bK1/37QCKp3+mCqjFzPyQOrvk\n"
                                + "GZv4sYyRwX7BKBLleQmIVWpofpjT7BfcCxH877RzC5YMIi65aBc82Dl6tH6OEiP7mzByU5\n"
                                + "2yvH6JFuwZ/9fWj1vXCWJzxx2w0F1OU8Zwg8gNNzL+SVb9+xfBE7xBHMpYFg72hBWPh862\n"
                                + "Ce36F4NZd3MpWMSjMmpDPh\n"
        );
    }

    @Test
    public void validSsh2PublicKeyWillFailWhenKeyMissing() {
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("[certificate: 'Comment: \"2048-bit RSA, converted by ricsi@Richards-MacBook-Pro.local \"");
        openSshPublicKeyValidator.validate(
                                "Comment: \"2048-bit RSA, converted by ricsi@Richards-MacBook-Pro.local \"\n"
                                + "AAAAB3NzaC1yc2EAAAADAQABAAABAQC0Rfl2G2vDs6yc19RxCqReunFgpYj+ucyLobpTCB\n"
                                + "tfDwzIbJot2Fmife6M42mBtiTmAK6x8kcUEeab6CB4MUzsqF7vGTFUjwWirG/XU5pYXFUB\n"
                                + "hi8xzey+KS9KVrQ+UuKJh/AN9iSQeMV+rgT1yF5+etVH+bK1/37QCKp3+mCqjFzPyQOrvk\n"
                                + "GZv4sYyRwX7BKBLleQmIVWpofpjT7BfcCxH877RzC5YMIi65aBc82Dl6tH6OEiP7mzByU5\n"
                                + "2yvH6JFuwZ/9fWj1vXCWJzxx2w0F1OU8Zwg8gNNzL+SVb9+xfBE7xBHMpYFg72hBWPh862\n"
                                + "Ce36F4NZd3MpWMSjMmpDPh\n"
                                + "---- END SSH2 PUBLIC KEY ----"
        );
    }
}
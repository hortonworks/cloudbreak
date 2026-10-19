package com.sequenceiq.cloudbreak.service.environment.credential;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mockStatic;

import java.security.Security;

import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.util.PublicKeyReaderUtil;
import com.sequenceiq.cloudbreak.util.RsaPublicKeyTestUtil;
import com.sequenceiq.common.model.OsType;

class OpenSshPublicKeyValidatorTest {

    private static final String VALID_PUBLIC_KEY_ED25519 = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIMQltFutaGpkyuDLScqHRZtknBd4c/IJCkVsY7WFS+gK";

    private OpenSshPublicKeyValidator underTest;

    @BeforeEach
    public void setUp() {
        underTest = new OpenSshPublicKeyValidator();
    }

    @Test
    public void validRsaPublicKeyWillNotFail() {
        underTest.validate(
                        "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCasJyap4swb4Hk4xOlnF3OmKVwzmv2e053yrtvcUPaxCeboSltOBReuT"
                                + "QxX+kYCgKCdtEwpIvEDXk16T6nCI4tSptAalFgpUWn+JOysCuLuWnwrk6mSKOzEiPYCrB54444mDY6rbBDSRuE/V"
                                + "UYQ/yi0imocARlOiFdPRlZGTN0XGE1V8LSo+m0oIzTwBKn58I4v5iB4ZUL/6adGXo7dgdBh/Fmm4uYbgrCZnL1EaK"
                                + "pMxSG76XWhuzFpHjLkRndz88ha0rB6davag6nZGdno5IepLAWg9oB4jTApHwhN2j1rWLN2y1c+pTxsF6LxBiN5rsY"
                                + "KR495VFmuOepLYz5I8Dn sequence-eu", false, OsType.RHEL9
        );
    }

    @Test
    public void validRsaPublicKeyWillNotFailWhenMissingUser() {
        underTest.validate(
                        "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCasJyap4swb4Hk4xOlnF3OmKVwzmv2e053yrtvcUPaxCeboSltOBReuT"
                                + "QxX+kYCgKCdtEwpIvEDXk16T6nCI4tSptAalFgpUWn+JOysCuLuWnwrk6mSKOzEiPYCrB54444mDY6rbBDSRuE/V"
                                + "UYQ/yi0imocARlOiFdPRlZGTN0XGE1V8LSo+m0oIzTwBKn58I4v5iB4ZUL/6adGXo7dgdBh/Fmm4uYbgrCZnL1EaK"
                                + "pMxSG76XWhuzFpHjLkRndz88ha0rB6davag6nZGdno5IepLAWg9oB4jTApHwhN2j1rWLN2y1c+pTxsF6LxBiN5rsY"
                                + "KR495VFmuOepLYz5I8Dn", false, OsType.RHEL9
        );
    }

    @Test
    void rsaPublicKeyShorterThan2048BitsIsAcceptedForRhel8() {
        underTest.validate(RsaPublicKeyTestUtil.openSshRsaKey(2047), false, OsType.RHEL8);
    }

    @Test
    void rsaPublicKeyShorterThan2048BitsWillFailWithRotateHint() throws Exception {
        String key = RsaPublicKeyTestUtil.openSshRsaKey(2047);
        BadRequestException ex = assertThrows(BadRequestException.class, () -> underTest.validate(key, false, OsType.RHEL9));
        assertThat(ex).hasMessageContaining("RSA public key is 2047 bits, at least 2048 bits are required");
        assertThat(ex).hasMessageContaining("cdp environments update-ssh-key");
    }

    @Test
    void validEd25519PublicKeyWillNotFailWhenNotFipsMode() {
        underTest.validate(VALID_PUBLIC_KEY_ED25519, false, OsType.RHEL9);
    }

    @Test
    void validEd25519PublicKeyWillFailWhenFipsMode() {
        BadRequestException badRequestException = assertThrows(BadRequestException.class, () ->
                underTest.validate(VALID_PUBLIC_KEY_ED25519, true, OsType.RHEL9));
        assertThat(badRequestException).hasMessageEndingWith("detailed message: SSH2ED25519: this key type is not allowed when running clusters in FIPS mode");
    }

    @Test
    public void inValidPublicKeyWillFailWhenMissingSshRsa() {
        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> underTest.validate(
                "AAAAB3NzaC1yc2EAAAADAQABAAABAQCasJyap4swb4Hk4xOlnF3OmKVwzmv2e053yrtvcUPaxCeboSltOBReuT"
                + "QxX+kYCgKCdtEwpIvEDXk16T6nCI4tSptAalFgpUWn+JOysCuLuWnwrk6mSKOzEiPYCrB54444mDY6rbBDSRuE/V"
                + "UYQ/yi0imocARlOiFdPRlZGTN0XGE1V8LSo+m0oIzTwBKn58I4v5iB4ZUL/6adGXo7dgdBh/Fmm4uYbgrCZnL1EaK"
                + "pMxSG76XWhuzFpHjLkRndz88ha0rB6davag6nZGdno5IepLAWg9oB4jTApHwhN2j1rWLN2y1c+pTxsF6LxBiN5rsY"
                + "KR495VFmuOepLYz5I8Dn sequence-eu", false, OsType.RHEL9
        ));
        assertThat(badRequestException).hasMessageEndingWith("detailed message: Corrupt or unknown public key file format");
    }

    @Test
    public void inValidPublicKeyWillFailWhenTooShortBody() {
        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> underTest.validate(
                "ssh-rsa AAAA sequence-eu", false, OsType.RHEL9
        ));
        assertThat(badRequestException).hasMessageEndingWith("detailed message: Index 3 out of bounds for length 3");
    }

    @Test
    public void inValidPublicKeyWillFailWhenTotallyInvalidString() {
        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> underTest.validate(
                        "ImBAD", false, OsType.RHEL9
        ));
        assertThat(badRequestException).hasMessageEndingWith("detailed message: Corrupt or unknown public key file format");
    }

    @Test
    public void validSsh2PublicKeyWillNotFail() {
        underTest.validate(
                """
                        ---- BEGIN SSH2 PUBLIC KEY ----
                        Comment: "2048-bit RSA, converted by ricsi@Richards-MacBook-Pro.local "
                        AAAAB3NzaC1yc2EAAAADAQABAAABAQC0Rfl2G2vDs6yc19RxCqReunFgpYj+ucyLobpTCB
                        tfDwzIbJot2Fmife6M42mBtiTmAK6x8kcUEeab6CB4MUzsqF7vGTFUjwWirG/XU5pYXFUB
                        hi8xzey+KS9KVrQ+UuKJh/AN9iSQeMV+rgT1yF5+etVH+bK1/37QCKp3+mCqjFzPyQOrvk
                        GZv4sYyRwX7BKBLleQmIVWpofpjT7BfcCxH877RzC5YMIi65aBc82Dl6tH6OEiP7mzByU5
                        2yvH6JFuwZ/9fWj1vXCWJzxx2w0F1OU8Zwg8gNNzL+SVb9+xfBE7xBHMpYFg72hBWPh862
                        Ce36F4NZd3MpWMSjMmpDPh
                        ---- END SSH2 PUBLIC KEY ----""", false, OsType.RHEL9
        );
    }

    @Test
    public void validSsh2PublicKeyWillFailWhenBeginAndEndSsh2Missing() {
        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> underTest.validate(
                """
                        Comment: "2048-bit RSA, converted by ricsi@Richards-MacBook-Pro.local "
                        AAAAB3NzaC1yc2EAAAADAQABAAABAQC0Rfl2G2vDs6yc19RxCqReunFgpYj+ucyLobpTCB
                        tfDwzIbJot2Fmife6M42mBtiTmAK6x8kcUEeab6CB4MUzsqF7vGTFUjwWirG/XU5pYXFUB
                        hi8xzey+KS9KVrQ+UuKJh/AN9iSQeMV+rgT1yF5+etVH+bK1/37QCKp3+mCqjFzPyQOrvk
                        GZv4sYyRwX7BKBLleQmIVWpofpjT7BfcCxH877RzC5YMIi65aBc82Dl6tH6OEiP7mzByU5
                        2yvH6JFuwZ/9fWj1vXCWJzxx2w0F1OU8Zwg8gNNzL+SVb9+xfBE7xBHMpYFg72hBWPh862
                        Ce36F4NZd3MpWMSjMmpDPh
                        """, false, OsType.RHEL9
        ));
        assertThat(badRequestException).hasMessageEndingWith("detailed message: Corrupt or unknown public key file format");
    }

    @Test
    public void validSsh2PublicKeyWillFailWhenKeyMissing() {
        BadRequestException badRequestException = assertThrows(BadRequestException.class, () -> underTest.validate(
                """
                        Comment: "2048-bit RSA, converted by ricsi@Richards-MacBook-Pro.local "
                        AAAAB3NzaC1yc2EAAAADAQABAAABAQC0Rfl2G2vDs6yc19RxCqReunFgpYj+ucyLobpTCB
                        tfDwzIbJot2Fmife6M42mBtiTmAK6x8kcUEeab6CB4MUzsqF7vGTFUjwWirG/XU5pYXFUB
                        hi8xzey+KS9KVrQ+UuKJh/AN9iSQeMV+rgT1yF5+etVH+bK1/37QCKp3+mCqjFzPyQOrvk
                        GZv4sYyRwX7BKBLleQmIVWpofpjT7BfcCxH877RzC5YMIi65aBc82Dl6tH6OEiP7mzByU5
                        2yvH6JFuwZ/9fWj1vXCWJzxx2w0F1OU8Zwg8gNNzL+SVb9+xfBE7xBHMpYFg72hBWPh862
                        Ce36F4NZd3MpWMSjMmpDPh
                        ---- END SSH2 PUBLIC KEY ----""", false, OsType.RHEL9
        ));
        assertThat(badRequestException).hasMessageEndingWith("detailed message: Corrupt or unknown public key file format");
    }

    @Test
    public void rsaPublicKeyWithInsufficientStrengthWillNotFailWithBcFipsProviderBecauseOfBackwardCompatiblity() {
        Security.removeProvider("SunRsaSign");
        Security.addProvider(new BouncyCastleFipsProvider());
        underTest.validate(
                "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCasJyap4swb4Hk4xOlnF3OmKVwzmv2e053yrtvcUPaxCeboSltOBReuT"
                        + "QxX+kYCgKCdtEwpIvEDXk16T6nCI4tSptAalFgpUWn+JOysCuLuWnwrk6mSKOzEiPYCrB54444mDY6rbBDSRuE/V"
                        + "UYQ/yi0imocARlOiFdPRlZGTN0XGE1V8LSo+m0oIzTwBKn58I4v5iB4ZUL/6adGXo7dgdBh/Fmm4uYbgrCZnL1EaK"
                        + "pMxSG76XWhuzFpHjLkRndz88ha0rB6davag6nZGdno5IepLAWg9oB4jTApHwhN2j1rWLN2y1c+pTxsF6LxBiN5rsY"
                        + "KR495VFmuOepLYz5I8Dn sequence-eu", false, OsType.RHEL9
        );
    }

    @Test
    void testValidateFipsNonCompliantKeyIgnored() {
        Security.removeProvider("SunRsaSign");
        Security.addProvider(new BouncyCastleFipsProvider());
        Exception inner = new IllegalArgumentException("RSAnot modulus has a small prime factor");
        Exception outer = new RuntimeException(inner);

        try (MockedStatic<PublicKeyReaderUtil> mockedStatic = mockStatic(PublicKeyReaderUtil.class)) {
            mockedStatic.when(() -> PublicKeyReaderUtil.load(any(), anyBoolean()))
                    .thenThrow(outer);
            BadRequestException badRequestException = assertThrows(BadRequestException.class, () ->
                    underTest.validate("fips-noncompliant-key", true, OsType.RHEL9));
            assertThat(badRequestException).hasMessageEndingWith(
                            "The provided public key ['fips-noncompliant-key'] is not valid, " +
                            "possibly due to insufficient strength. Cause: RSAnot modulus " +
                            "has a small prime factor. Please create new SSH keys for the " +
                            "environment by editing 'Root SSH' on the environment's Summary" +
                            " page or with this command: 'cdp environments update-ssh-key'");
        }
    }

}
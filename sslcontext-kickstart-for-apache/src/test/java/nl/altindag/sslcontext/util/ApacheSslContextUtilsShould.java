package nl.altindag.sslcontext.util;

import nl.altindag.sslcontext.SSLFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import static org.assertj.core.api.Assertions.assertThat;

public class ApacheSslContextUtilsShould {

    private static final String IDENTITY_FILE_NAME = "identity.jks";
    private static final String TRUSTSTORE_FILE_NAME = "truststore.jks";

    private static final char[] IDENTITY_PASSWORD = "secret".toCharArray();
    private static final char[] TRUSTSTORE_PASSWORD = "secret".toCharArray();
    private static final String KEYSTORE_LOCATION = "keystores-for-unit-tests/";

    @Test
    public void createLayeredConnectionSocketFactoryWithTrustMaterial() throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException {
        KeyStore trustStore = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);

        SSLFactory sslFactory = SSLFactory.builder()
                .withTrustStore(trustStore, TRUSTSTORE_PASSWORD)
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getKeyManager()).isNotPresent();
        assertThat(sslFactory.getIdentities()).isEmpty();

        assertThat(sslFactory.getTrustManager()).isNotNull();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getTrustStores()).isNotEmpty();
        assertThat(sslFactory.getTrustStores().get(0).getKeyStorePassword()).isEqualTo(TRUSTSTORE_PASSWORD);
        assertThat(sslFactory.getTrustManager()).isNotNull();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();
        assertThat(sslFactory.getSslContext().getProtocol()).isEqualTo("TLSv1.2");

        LayeredConnectionSocketFactory socketFactory = ApacheSslContextUtils.toLayeredConnectionSocketFactory(sslFactory);
        assertThat(socketFactory).isNotNull();
    }

    @Test
    public void createLayeredConnectionSocketFactoryWithIdentityMaterialAndTrustMaterial() throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException {
        KeyStore identity = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + IDENTITY_FILE_NAME, IDENTITY_PASSWORD);
        KeyStore trustStore = KeyStoreUtils.loadKeyStore(KEYSTORE_LOCATION + TRUSTSTORE_FILE_NAME, TRUSTSTORE_PASSWORD);

        SSLFactory sslFactory = SSLFactory.builder()
                .withIdentity(identity, IDENTITY_PASSWORD)
                .withTrustStore(trustStore, TRUSTSTORE_PASSWORD)
                .build();

        assertThat(sslFactory.getSslContext()).isNotNull();

        assertThat(sslFactory.getKeyManager()).isPresent();
        assertThat(sslFactory.getIdentities()).isNotEmpty();
        assertThat(sslFactory.getIdentities().get(0).getKeyStorePassword()).isEqualTo(IDENTITY_PASSWORD);

        assertThat(sslFactory.getTrustManager()).isNotNull();
        assertThat(sslFactory.getTrustedCertificates()).isNotEmpty();
        assertThat(sslFactory.getTrustStores()).isNotEmpty();
        assertThat(sslFactory.getTrustStores().get(0).getKeyStorePassword()).isEqualTo(TRUSTSTORE_PASSWORD);
        assertThat(sslFactory.getTrustManager()).isNotNull();
        assertThat(sslFactory.getHostnameVerifier()).isNotNull();
        assertThat(sslFactory.getSslContext().getProtocol()).isEqualTo("TLSv1.2");

        LayeredConnectionSocketFactory socketFactory = ApacheSslContextUtils.toLayeredConnectionSocketFactory(sslFactory);
        assertThat(socketFactory).isNotNull();
    }

}

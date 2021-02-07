/*
 * Copyright 2019-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.altindag.ssl;

import nl.altindag.log.LogCaptor;
import nl.altindag.ssl.util.Apache4SslUtils;
import nl.altindag.ssl.util.KeyStoreUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Hakan Altindag
 */
class SSLFactoryIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(SSLFactoryIT.class);

    @Test
    void executeHttpsRequestWithMutualAuthentication() throws IOException {
        LogCaptor logCaptor = LogCaptor.forName("nl.altindag.ssl");

        SSLFactory sslFactory = SSLFactory.builder()
                .withIdentityMaterial("keystores-for-unit-tests/badssl-identity.p12", "badssl.com".toCharArray())
                .withTrustMaterial("keystores-for-unit-tests/badssl-truststore.p12", "badssl.com".toCharArray())
                .withTrustMaterial(KeyStoreUtils.createKeyStore()) // Adding additional trust material forces usage of CompositeX509ExtendedTrustManager and verbose logging
                .build();

        LayeredConnectionSocketFactory socketFactory = Apache4SslUtils.toSocketFactory(sslFactory);

        HttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(socketFactory)
                .build();

        HttpGet request = new HttpGet("https://client.badssl.com/");
        HttpResponse response = httpClient.execute(request);

        int statusCode = response.getStatusLine().getStatusCode();

        if (statusCode == 400) {
            LOGGER.warn("Certificate may have expired and needs to be updated");
        } else {
            assertThat(statusCode).isEqualTo(200);
            assertThat(logCaptor.getLogs()).containsExactly("Received the following server certificate: [CN=*.badssl.com, O=Lucas Garron Torres, L=Walnut Creek, ST=California, C=US]");
        }
    }

    @Test
    @Disabled("The server [https://prod.idrix.eu/secure/] has a rate limiter which will only allow couple of requests from a single machine. " +
              "Therefore this test is disabled to prevent a failing build.")
    void executeHttpsRequestWithMutualAuthenticationForMultipleClientIdentitiesWithSingleSslConfiguration() throws Exception {
        LogCaptor logCaptor = LogCaptor.forName("nl.altindag.ssl");

        SSLFactory sslFactory = SSLFactory.builder()
                .withIdentityMaterial("keystores-for-unit-tests/badssl-identity.p12", "badssl.com".toCharArray())
                .withIdentityMaterial("keystores-for-unit-tests/prod.idrix.eu-identity.jks", "secret".toCharArray())
                .withTrustMaterial("keystores-for-unit-tests/badssl-truststore.p12", "badssl.com".toCharArray())
                .withTrustMaterial("keystores-for-unit-tests/prod.idrix.eu-truststore.jks", "secret".toCharArray())
                .build();

        LayeredConnectionSocketFactory socketFactory = Apache4SslUtils.toSocketFactory(sslFactory);

        HttpClient httpClient = HttpClients.custom()
                .setSSLSocketFactory(socketFactory)
                .build();

        HttpGet request = new HttpGet("https://client.badssl.com/");
        HttpResponse response = httpClient.execute(request);

        int statusCode = response.getStatusLine().getStatusCode();

        if (statusCode == 400) {
            LOGGER.warn("Certificate may have expired and needs to be updated");
        } else {
            assertThat(statusCode).isEqualTo(200);
            assertThat(logCaptor.getLogs()).containsExactly("Received the following server certificate: [CN=*.badssl.com, O=Lucas Garron Torres, L=Walnut Creek, ST=California, C=US]");
        }

        request = new HttpGet("https://prod.idrix.eu/secure/");
        response = httpClient.execute(request);

        statusCode = response.getStatusLine().getStatusCode();

        if (statusCode == 400) {
            LOGGER.warn("Certificate may have expired and needs to be updated");
        } else {
            assertThat(statusCode).isEqualTo(200);
            String body = EntityUtils.toString(response.getEntity());
            assertThat(body).contains("SSL Authentication OK");
            assertThat(body).doesNotContain("No SSL client certificate presented");
            assertThat(logCaptor.getLogs()).contains("Received the following server certificate: [CN=prod.idrix.eu]");
        }
    }

}

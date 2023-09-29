/*
 * Engineering Ingegneria Informatica S.p.A.
 *
 * Copyright (C) 2023 Regione Emilia-Romagna
 * <p/>
 * This program is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package it.eng.parer.retry;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Test della modalità di retry
 *
 * @author Snidero_L
 */
public class TestRetry {

    private final Logger log = LoggerFactory.getLogger(TestRetry.class);

    private static final int TIMEOUT = 10000;

    private static final String CRYPTO_LOCALE = "http://localhost:8091/";
    private static final String CRYPTO_SVIL_OKD = "https://verificafirma-crypto-latest.apps.test-parerocp.ente.regione.emr.it/";
    private static final String CRYPTO_TEST_OKD = "https://verificafirma-crypto-latest.apps.test-parerocp.ente.regione.emr.it/";
    private static final List<URI> BAD_ENDPOINTS = new ArrayList<>();

    private RestTemplate restTemplate;

    private String preferredEndpoint;

    @BeforeClass
    public static void setUpClass() {
        BAD_ENDPOINTS.add(URI.create(CRYPTO_LOCALE));
        BAD_ENDPOINTS.add(URI.create("Br0kenUr1"));
        BAD_ENDPOINTS.add(URI.create("http://localhost:8092/"));
        BAD_ENDPOINTS.add(URI.create("http://localhost:8093/"));
        BAD_ENDPOINTS.add(URI.create("//////////"));
        BAD_ENDPOINTS.add(URI.create("../../"));
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        restTemplate = new RestTemplate();

        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        clientHttpRequestFactory.setReadTimeout(TIMEOUT);
        clientHttpRequestFactory.setConnectTimeout(TIMEOUT);

        restTemplate.setRequestFactory(clientHttpRequestFactory);

        preferredEndpoint = CRYPTO_TEST_OKD;
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testTST() {
        log.info("Test configurazione predefinita");
        HttpEntity<MultiValueMap<String, Object>> requestEntity = buildValidRequestEntity();

        String endpoint = preferredEndpoint + "api/tst";

        ParerRetryConfiguration retryClient = ParerRetryConfiguration.defaultInstance();
        restTemplate.getInterceptors().removeIf(i -> true);
        restTemplate.getInterceptors().add(new RestRetryInterceptor(BAD_ENDPOINTS, retryClient));

        String parerTST = restTemplate.postForObject(endpoint, requestEntity, String.class);
        Assert.assertNotNull(parerTST);
    }

    @Test(expected = RestClientException.class)
    public void testTSTWithNoValidURL() {

        log.info("Test senza URL raggiungibili");
        HttpEntity<MultiValueMap<String, Object>> requestEntity = buildValidRequestEntity();

        String preferredEndpoint = "http://localhost:8090/api/tst";

        ParerRetryConfiguration retryClient = ParerRetryConfiguration.defaultInstance();
        restTemplate.getInterceptors().removeIf(i -> true);
        restTemplate.getInterceptors().add(new RestRetryInterceptor(BAD_ENDPOINTS, retryClient));

        restTemplate.postForObject(preferredEndpoint, requestEntity, Object.class);
    }

    @Test
    public void testTSTWithSomeBadURI() {

        log.info("Test con alcune URL raggiungibili");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = buildValidRequestEntity();

        String preferredEndpoint = "http://localhost:8090/api/tst";

        // Endpoint errati
        List<URI> endPoints = new ArrayList<>(BAD_ENDPOINTS);
        // Aggiungo endpoint "buono
        endPoints.add(URI.create(CRYPTO_SVIL_OKD));

        ParerRetryConfiguration retryClient = ParerRetryConfiguration.defaultInstance();
        restTemplate.getInterceptors().removeIf(i -> true);
        restTemplate.getInterceptors().add(new RestRetryInterceptor(endPoints, retryClient));

        String parerTST = restTemplate.postForObject(preferredEndpoint, requestEntity, String.class);
        Assert.assertNotNull(parerTST);

    }

    @Test(expected = Exception.class)
    public void testTSTWitoutMandatoryParameter() {

        log.info("Test senza parametro obbligatorio");

        HttpEntity<MultiValueMap<String, Object>> requestEntity = buildInvalidRequestEntity();

        String preferredEndpoint = "http://localhost:8090/api/tst";

        // Endpoint errati
        List<URI> endPoints = new ArrayList<>(BAD_ENDPOINTS);
        // Aggiungo endpoint "buono
        endPoints.add(URI.create(CRYPTO_SVIL_OKD));

        ParerRetryConfiguration retryClient = ParerRetryConfiguration.defaultInstance();

        restTemplate.getInterceptors().removeIf(i -> true);
        restTemplate.getInterceptors().add(new RestRetryInterceptor(endPoints, retryClient));

        String parerTST = restTemplate.postForObject(preferredEndpoint, requestEntity, String.class);
        Assert.assertNotNull(parerTST);
    }

    /**
     * Test policy composita con logica "ottimistica": timeout molto piccolo e molte retry. Effettuerà tutte le retry.
     *
     */
    @Test
    public void testOptimisticParameter() {

        log.info("Test con policy composita e logica ottimistica");
        HttpEntity<MultiValueMap<String, Object>> requestEntity = buildValidRequestEntity();

        String preferredEndpoint = "http://localhost:8090/api/tst";

        // Endpoint errati
        List<URI> endPoints = new ArrayList<>(BAD_ENDPOINTS);
        // Aggiungo endpoint "buono
        endPoints.add(URI.create(CRYPTO_SVIL_OKD));

        ParerRetryConfiguration retryClientConfiguration = new ParerRetryConfigurationBuilder().withMaxAttemps(10)
                .withOptimisticCompositePolicy(true).withTimeout(1L).build();

        restTemplate.getInterceptors().removeIf(i -> true);
        restTemplate.getInterceptors().add(new RestRetryInterceptor(endPoints, retryClientConfiguration));

        String parerTST = restTemplate.postForObject(preferredEndpoint, requestEntity, String.class);
        Assert.assertNotNull(parerTST);
    }

    /**
     * Test policy composita con logica "pessimistica": timeout molto piccolo e molte retry. Effettuerà solo la prima.
     *
     */
    @Test(expected = RestClientException.class)
    public void testPessimisticParameter() {
        log.info("Test con policy composita e logica pessimistica");
        HttpEntity<MultiValueMap<String, Object>> requestEntity = buildValidRequestEntity();

        String preferredEndpoint = "http://localhost:8090/api/tst";

        // Endpoint errati
        List<URI> endPoints = new ArrayList<>(BAD_ENDPOINTS);
        // Aggiungo endpoint "buono
        endPoints.add(URI.create(CRYPTO_SVIL_OKD));

        ParerRetryConfiguration retryClientConfiguration = new ParerRetryConfigurationBuilder().withMaxAttemps(10)
                .withOptimisticCompositePolicy(false).withTimeout(1L).build();

        restTemplate.getInterceptors().removeIf(i -> true);
        restTemplate.getInterceptors().add(new RestRetryInterceptor(endPoints, retryClientConfiguration));

        String parerTST = restTemplate.postForObject(preferredEndpoint, requestEntity, String.class);
        Assert.assertNotNull(parerTST);
    }

    private HttpEntity<MultiValueMap<String, Object>> buildValidRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        org.springframework.core.io.Resource resource = new ByteArrayResource("Ceci n'est pas un test".getBytes()) {
            @Override
            public String getFilename() {
                return "requestTst";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("description", "Richiesta TST");
        body.add("file", resource);

        return new HttpEntity<>(body, headers);
    }

    private HttpEntity<MultiValueMap<String, Object>> buildInvalidRequestEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        org.springframework.core.io.Resource resource = new ByteArrayResource("Ceci n'est pas un test".getBytes()) {
            @Override
            public String getFilename() {
                return "requestTst";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        // Tolta la descrizione che è obbligatoria
        body.add("file", resource);

        return new HttpEntity<>(body, headers);
    }

}

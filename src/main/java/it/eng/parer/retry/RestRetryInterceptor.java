/*
 * Engineering Ingegneria Informatica S.p.A.
 *
 * Copyright (C) 2023 Regione Emilia-Romagna <p/> This program is free software: you can
 * redistribute it and/or modify it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version. <p/> This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Affero General Public License for more details. <p/> You should
 * have received a copy of the GNU Affero General Public License along with this program. If not,
 * see <https://www.gnu.org/licenses/>.
 */

package it.eng.parer.retry;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.web.client.RestClientException;

/**
 * Interceptor per RestTemplate per abilitare il retry.
 *
 * Attualmente non sono state implementate strategie di recover. Per maggiori informazioni guarda
 * https://github.com/spring-projects/spring-retry
 *
 * @author Snidero_L
 */
public class RestRetryInterceptor implements ClientHttpRequestInterceptor {

    private final Logger log = LoggerFactory.getLogger(RestRetryInterceptor.class);

    private final AtomicInteger indice = new AtomicInteger(0);

    private final List<URI> additionalEndpoints;
    private final ParerRetryConfiguration retryClient;

    public RestRetryInterceptor(List<URI> additionalEndpoints,
            ParerRetryConfiguration retryClient) {
        this.additionalEndpoints = additionalEndpoints;
        this.retryClient = retryClient;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest hr, byte[] bytes,
            ClientHttpRequestExecution chre) throws IOException {

        SpringCallBack callback = new SpringCallBack(hr, bytes, chre);

        return retryClient.execute(callback);
    }

    private class SpringCallBack implements RetryCallback<ClientHttpResponse, RestClientException> {

        private final HttpRequest hr;
        byte[] bytes;
        private final ClientHttpRequestExecution chre;

        public SpringCallBack(HttpRequest hr, byte[] bytes, ClientHttpRequestExecution chre) {
            this.chre = chre;
            this.hr = hr;
            this.bytes = bytes;

        }

        private URI nextEndPoint(String uriPath) {
            try {
                if (indice.get() >= additionalEndpoints.size()) {
                    indice.set(0);
                }
                return URI.create(additionalEndpoints.get(indice.get()).toASCIIString() + uriPath);
            } finally {
                indice.addAndGet(1);
            }
        }

        @Override
        public ClientHttpResponse doWithRetry(RetryContext context) {

            URI currentURI = hr.getURI();

            String uriPath = currentURI.getPath();

            if (currentURI.getQuery() != null && !currentURI.getQuery().isEmpty()) {
                uriPath += "?" + currentURI.getQuery();
            }

            if (context.getLastThrowable() != null) {
                log.debug("Eccezione di tipo " + context.getLastThrowable().getClass(),
                        context.getLastThrowable());

                // cycle URL
                currentURI = nextEndPoint(uriPath);

            }

            try {
                log.debug("{} # {} a  [{}]", hr.getMethod(), context.getRetryCount(), currentURI);

                HttpRequest wrapper = new SpringHttpRequestWrapper(currentURI, hr);
                ClientHttpResponse response = chre.execute(wrapper, bytes);
                // i response code "gestiti" dall'enpoint sono 200, 400, 404, 417, 500
                // qualunque altro codice che non rientra su questa lista scatena la retry
                if (!Arrays.asList(HttpStatus.OK, HttpStatus.INTERNAL_SERVER_ERROR,
                        HttpStatus.BAD_REQUEST, HttpStatus.EXPECTATION_FAILED, HttpStatus.NOT_FOUND)
                        .contains(response.getStatusCode())) {
                    throw new RestClientException(
                            "Response code ottenuto " + response.getStatusCode()
                                    + " invocando endpoint " + currentURI + " scateno retry....");
                }
                return response;
            } catch (IOException e) {
                throw new RestClientException("Impossibile raggiungere l'endpoint", e);
            }
        }

    }

    private class SpringHttpRequestWrapper implements HttpRequest {

        private final URI newUri;
        private final HttpRequest hr;

        SpringHttpRequestWrapper(URI newUri, HttpRequest hr) {
            this.newUri = newUri;
            this.hr = hr;
        }

        @Override
        public URI getURI() {
            return newUri;
        }

        @Override
        public HttpHeaders getHeaders() {
            return hr.getHeaders();
        }

        @Override
        public String getMethodValue() {
            return hr.getMethodValue();
        }
    }

}

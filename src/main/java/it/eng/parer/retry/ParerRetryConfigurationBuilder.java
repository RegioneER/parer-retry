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

import java.util.ArrayList;
import java.util.List;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.CircuitBreakerRetryPolicy;
import org.springframework.retry.policy.CompositeRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.policy.TimeoutRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

/**
 * Fluent builder per creare un retry client.
 *
 * @author Snidero_L
 */
public class ParerRetryConfigurationBuilder {

    public static final int DEFAULT_MAX_RETRY = 10;

    private BackOffPolicy backOffPolicy;
    private long cbOpenTimeout;
    private long cbResetTimeout;
    private final List<RetryPolicy> policies = new ArrayList<>();
    private boolean optimisticCompositePolicy = true;

    /**
     * Aggiunge una {@link TimeoutRetryPolicy}. Il valore del timeout è espresso in ms.
     *
     * @param timeout in ms
     *
     * @return {@link ParerRetryConfigurationBuilder}
     */
    public ParerRetryConfigurationBuilder withTimeout(long timeout) {
        TimeoutRetryPolicy policy = new TimeoutRetryPolicy();
        policy.setTimeout(timeout);
        this.policies.add(policy);

        return this;
    }

    /**
     * Aggiunge un {@link SimpleRetryPolicy}.
     *
     * @param maxAttemps numero massimo di tentativi
     *
     * @return {@link ParerRetryConfigurationBuilder}
     */
    public ParerRetryConfigurationBuilder withMaxAttemps(int maxAttemps) {
        SimpleRetryPolicy policy = new SimpleRetryPolicy();
        policy.setMaxAttempts(maxAttemps);
        this.policies.add(policy);
        return this;
    }

    /**
     * Imposta un periodo di backoff tra una chiamata e l'altra.
     *
     * @param backOffPeriod espresso in ms
     *
     * @return {@link ParerRetryConfigurationBuilder}
     */
    public ParerRetryConfigurationBuilder withBackoffPeriod(long backOffPeriod) {
        FixedBackOffPolicy policy = new FixedBackOffPolicy();
        policy.setBackOffPeriod(backOffPeriod);
        this.backOffPolicy = policy;
        return this;
    }

    /**
     * Imposta le configurazioni relative ad un'approccio a rottura di circuito.
     *
     * @param openTimeout  timeout di apertura
     * @param resetTimeout timeout di reset
     *
     * @return {@link ParerRetryConfigurationBuilder}
     */
    public ParerRetryConfigurationBuilder withCircuitBreaker(long openTimeout, long resetTimeout) {
        this.cbOpenTimeout = openTimeout;
        this.cbResetTimeout = resetTimeout;
        return this;
    }

    /**
     * Imposta le configurazioni relative all'approccio ottimistico o pessimistico per le policy
     * composite.
     *
     * @param compositePolicy valore da assegnare alla logica di valutazione delle policy composite
     *
     * @return {@link ParerRetryConfigurationBuilder}
     */
    public ParerRetryConfigurationBuilder withOptimisticCompositePolicy(boolean compositePolicy) {
        this.optimisticCompositePolicy = compositePolicy;
        return this;
    }

    /**
     * Costruttuttore del client concreto.
     *
     * @return {@link ParerRetryConfiguration}
     */
    public ParerRetryConfiguration build() {

        ParerRetryConfiguration restClient = new ParerRetryConfiguration();
        RetryTemplate retryTemplate = new RetryTemplate();

        if (this.backOffPolicy != null) {
            retryTemplate.setBackOffPolicy(this.backOffPolicy);
        }

        // predefinito, 10 tentativi.
        RetryPolicy policy = new SimpleRetryPolicy(DEFAULT_MAX_RETRY);
        if (!policies.isEmpty()) {
            CompositeRetryPolicy compositePolicy = new CompositeRetryPolicy();
            compositePolicy.setPolicies(policies.toArray(new RetryPolicy[] {}));
            compositePolicy.setOptimistic(this.optimisticCompositePolicy);

            policy = compositePolicy;
        }

        if (cbOpenTimeout > 0 && cbResetTimeout > 0) {
            CircuitBreakerRetryPolicy circuitBreakerPolicy = new CircuitBreakerRetryPolicy(policy);
            circuitBreakerPolicy.setOpenTimeout(cbOpenTimeout);
            circuitBreakerPolicy.setResetTimeout(cbResetTimeout);
            policy = circuitBreakerPolicy;
        }
        retryTemplate.setRetryPolicy(policy);

        restClient.setRetryTemplate(retryTemplate);

        return restClient;
    }
}

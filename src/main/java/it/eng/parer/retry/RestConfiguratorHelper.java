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

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Interfaccia per configurare i servizi Rest con meccanismo di retry.
 *
 * @author Snidero_L
 */
public interface RestConfiguratorHelper {

    /**
     * Timeout per le chiamate agli endpoint. Il valore predefinito è 5 minuti.
     *
     * @return il numero di ms indicanti il timeout.
     */
    default int clientTimeout() {
        final int fiveMinutes = 300_000;

        Long clientTimeoutInMinutesParam = getClientTimeoutInMinutesParam();
        if (clientTimeoutInMinutesParam == null) {
            return fiveMinutes;
        }
        long toMillis = TimeUnit.MINUTES.toMillis(clientTimeoutInMinutesParam);

        final int timeoutInMillis;
        if (toMillis < 0 || toMillis > Integer.MAX_VALUE) {
            // default 5 minuti
            timeoutInMillis = fiveMinutes;
        } else {
            timeoutInMillis = (int) toMillis;
        }

        return timeoutInMillis;
    }

    /**
     * Lista degli endpoint per i servizi REST. Tendenzialmente questa verrà trattata come una lista
     * circolare.
     *
     * @return lista di endpoint
     */
    List<String> endPoints();

    /**
     * Costruisce il RetryClient per il servizio di verifica firme.
     *
     * @return RetryClient configurato
     */
    default ParerRetryConfiguration retryClient() {

        ParerRetryConfigurationBuilder retryBuilder = ParerRetryConfiguration.builder();

        if (getMaxRetryParam() != null) {
            retryBuilder.withMaxAttemps(getMaxRetryParam());
        }
        if (getRetryTimeoutParam() != null) {
            retryBuilder.withTimeout(getRetryTimeoutParam());
        }
        if (getCircuitBreakerOpenTimeoutParam() != null
                && getCircuitBreakerResetTimeoutParam() != null) {
            retryBuilder.withCircuitBreaker(getCircuitBreakerOpenTimeoutParam(),
                    getCircuitBreakerResetTimeoutParam());
        }
        if (getPeriodoBackOffParam() != null) {
            retryBuilder.withBackoffPeriod(getPeriodoBackOffParam());
        }
        if (isCompositePolicyOptimisticParam() != null) {
            retryBuilder.withOptimisticCompositePolicy(isCompositePolicyOptimisticParam());
        }

        return retryBuilder.build();
    }

    /**
     * Parametro di configurazione relativo al timeout per il meccanismo di retry.
     *
     * Il parametro è opzionale.
     *
     * @return timeout in ms oppure null
     */
    Long getRetryTimeoutParam();

    /**
     * Numero di tentativi prima di considerare la chiamata fallita. Il parametro è opzionale.
     *
     * @return numero di tentativi oppure null
     */
    Integer getMaxRetryParam();

    /**
     * Indica, quando sono indicate più policy di retry, se utilizzare una politica di tipo:
     * <ul>
     * <li><strong>pessimistico</strong> se almeno una policy non è valida allora non effettuo più
     * retry</li>
     * <li><strong>ottimistico</strong> se almeno una policy è valida allora effettuo ancora
     * retry</li>
     * </ul>
     *
     * Il parametro è opzionale. Il suo valore predefinito è <em>true</em>
     *
     * @return true se la policy è di tipo ottimistico (default) false altrimenti
     */
    Boolean isCompositePolicyOptimisticParam();

    /**
     * Timeout della fase di "Open" del circuit breaker. Il parametro è opzionale e viene valutato
     * solo se è presente anche {@link #getCircuitBreakerResetTimeoutParam()}
     *
     *
     * @return timeout in ms oppure null
     */
    Long getCircuitBreakerOpenTimeoutParam();

    /**
     * Timeout della fase di "Reset" del circuit breaker. Il parametro è opzionale e viene valutato
     * solo se è presente anche {@link #getCircuitBreakerOpenTimeoutParam()}
     *
     * @return timeout in ms oppure null
     */
    Long getCircuitBreakerResetTimeoutParam();

    /**
     * Periodo di backoff (periodo di tempo in cui non verranno effettuate chiamate sulla rete). Il
     * parametro è opzionale.
     *
     * @return periodo di backoff in ms oppure null
     */
    Long getPeriodoBackOffParam();

    /**
     * Timeout del client per effettuare la chiamata. Il parametro è opzionale
     *
     * @return timeout in minuti oppure null
     */
    Long getClientTimeoutInMinutesParam();

    /**
     * Endpoint principale su cui effettuare le chiamate
     *
     * @return endpoint principale
     */
    String preferredEndpoint();

}

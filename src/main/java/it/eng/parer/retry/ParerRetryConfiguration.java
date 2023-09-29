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

import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;

/**
 * Client per ws rest con gestione automatica del retry e del fallback. Basato su {@link RetryTemplate}.
 *
 * @author Snidero_L
 */
public class ParerRetryConfiguration {

    private RetryTemplate retryTemplate;

    /**
     * Fluent builder del client.
     *
     * @return oggetto deputato alla costruzione del meccanismo di retry.
     */
    public static ParerRetryConfigurationBuilder builder() {
        return new ParerRetryConfigurationBuilder();
    }

    /**
     * Configurazione predefinita
     *
     * @return Client predefinito.
     */
    public static ParerRetryConfiguration defaultInstance() {
        return new ParerRetryConfigurationBuilder().build();
    }

    protected void setRetryTemplate(RetryTemplate retryTemplate) {
        this.retryTemplate = retryTemplate;
    }

    /**
     * Delegate per l'esecuzione del metodo sottoposto a retry.
     *
     * @param <R>
     *            Risultato atteso
     * @param <E>
     *            Eccezione per cui si effettua il retry
     * @param metodo
     *            callback, ovvero metodo che deve essere eseguito
     * @param fallback
     *            fallback, ovvero metodo che deve essere eseguito in caso falliscano tutti i tentativi di re-invio
     * 
     * @return Risultato atteso
     *
     * @throws E
     *             eccezione che innesca (possibilmente) una successiva esecuzione.
     */
    public <R, E extends Throwable> R execute(RetryCallback<R, E> metodo, RecoveryCallback<R> fallback) throws E {
        return retryTemplate.execute(metodo, fallback);
    }

    /**
     * Delegate per l'esecuzione del metodo sottoposto a retry.
     *
     * @param <R>
     *            Risultato atteso
     * @param <E>
     *            Eccezione per cui si effettua il retry
     * @param metodo
     *            callback, ovvero metodo che deve essere eseguito
     * 
     * @return Risultato atteso
     *
     * @throws E
     *             eccezione che innesca (possibilmente) una successiva esecuzione.
     */
    public <R, E extends Throwable> R execute(RetryCallback<R, E> metodo) throws E {
        return retryTemplate.execute(metodo);
    }

}

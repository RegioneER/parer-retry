# Retry client

Fonte template redazione documento:  https://www.makeareadme.com/.


# Descrizione

Il seguente progetto è utilizzato come **dipendenza** interna per definire strategie di retry sulle chiamate verso API REST, attraverso l'utilizzo del client RestTemplate (https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/client/RestTemplate.html).

# Installazione

Come già specificato nel paragrafo precedente [Descrizione](# Descrizione) si tratta di un progetto di tipo "libreria", quindi un modulo applicativo utilizzato attraverso la definzione della dipendenza Maven secondo lo standard previsto (https://maven.apache.org/): 

```xml
<dependency>
    <groupId>it.eng.parer</groupId>
    <artifactId>parer-retry</artifactId>
    <version>$VERSIONE</version>
</dependency>
```

# Utilizzo

Il modulo contiene al suo interno le logiche con le quali si può definire un client (di tipo RestTemplate) progettato per effettuare chiamate verso uno o più endpoint configurati; la modalità di chiamata prevede, tra le varie configurazioni possibili, quella di effettuare 1 o più tentativi successivi (retry) qualora si verificassero casi di timeout, l'HTTP code ricevuto non rientra tra i seguenti: OK (200) , INTERNAL_SERVER_ERROR (400), BAD_REQUEST (404), EXPECTATION_FAILED (417), NOT_FOUND (500); o qualunque altro errore generico in fase di comunicazione client-server.
Tale caratteristica è da ricondurre alla modalità "retry", ormai divenuto uno standard all'interno delle implementazioni più moderne, attraverso la metodologia del "circuit breaker", ossia la definizione di un set di policy con le quali definire le condizioni attraverso sui il client decide o meno di effettuare nuovamente una chiamata verso il singolo endpoint.

## Esempio di implementazione client

Implementazione della configurazione:

```java

public class RestConfiguratorHelper implements RestConfiguratorHelper {


    @Override
    public Long getRetryTimeoutParam() {
        return 10000L;
    }

    @Override
    public Integer getMaxRetryParam() {
        return 5;
    }

    @Override
    public Long getCircuitBreakerOpenTimeoutParam() {
        return 10000L;
    }

    @Override
    public Long getCircuitBreakerResetTimeoutParam() {
        return 10000L;
    }

    @Override
    public Long getPeriodoBackOffParam() {
        return 10000L;
    }

    @Override
    public Long getClientTimeoutInMinutesParam() {
        return 10000L;
    }

    @Override
    public Boolean isCompositePolicyOptimisticParam() {
        return true;
    }

    /**
     * Lista degli endpoint per i servizi REST. Tendenzialmente questa verrà trattata come una lista circolare.
     *
     * @return lista di endpoint
     */
    @Override
    public List<String> endPoints() {
        return "https://endpoint1,https://endpoint1, etc.";
    }

    @Override
    public String preferredEndpoint() {
        return endPoints().get(0);
    }
}
```
Implementazione del client con RestTemplate:

```java

  RestTemplate template = new RestTemplate();

  HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory();
  clientHttpRequestFactory.setReadTimeout(timeout);
  clientHttpRequestFactory.setConnectTimeout(timeout);
  clientHttpRequestFactory.setConnectionRequestTimeout(timeout);

  template.setRequestFactory(clientHttpRequestFactory);
  
  List<String> endpoints = restInvoker.endPoints();
  List<URI> endpointsURI = endpoints.stream().map(URI::create).collect(Collectors.toList());

  ParerRetryConfiguration retryClient = restInvoker.retryClient();
  template.getInterceptors().add(new RestRetryInterceptor(endpointsURI, retryClient));



```

# Supporto

Progetto a cura di [Engineering Ingegneria Informatica S.p.A.](https://www.eng.it/).

# Contributi

Se interessati a crontribuire alla crescita del progetto potete scrivere all'indirizzo email <a href="mailto:areasviluppoparer@regione.emilia-romagna.it">areasviluppoparer@regione.emilia-romagna.it</a>.

# Autori

Proprietà intellettuale del progetto di [Regione Emilia-Romagna](https://www.regione.emilia-romagna.it/) e [Polo Archivisitico](https://poloarchivistico.regione.emilia-romagna.it/).

# Licenza

Questo progetto è rilasciato sotto licenza GNU Affero General Public License v3.0 or later ([LICENSE.txt](LICENSE.txt)).

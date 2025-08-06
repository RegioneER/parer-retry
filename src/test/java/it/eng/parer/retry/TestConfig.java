/**
 *
 */
package it.eng.parer.retry;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:application.properties")
@ComponentScan("it.eng.parer.retry")
public class TestConfig {

}

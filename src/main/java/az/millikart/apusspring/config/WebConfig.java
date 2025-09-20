package az.millikart.apusspring.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WebConfig {

    private final Integer connectionTimeout;
    private final Integer readTimeout;

    public WebConfig(@Value("${service.forward-connection-timeout}") Integer connectionTimeout,
                     @Value("${service.forward-read-timeout}") Integer readTimeout) {
        this.connectionTimeout = connectionTimeout;
        this.readTimeout = readTimeout;
    }


    @Bean(name = "restTemplateBean")
    public RestTemplate restTemplate(final RestTemplateBuilder builder) {
        return builder
                .readTimeout(java.time.Duration.ofMillis(this.readTimeout))
                .connectTimeout(java.time.Duration.ofMillis(this.connectionTimeout))
                .build();
    }

}

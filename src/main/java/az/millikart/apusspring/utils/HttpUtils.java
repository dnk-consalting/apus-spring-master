package az.millikart.apusspring.utils;

import az.millikart.apusspring.txpg.domain.ExecTranResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j(topic = "service_logger")
public class HttpUtils {

    private final RestTemplate restTemplate;

    public HttpUtils(@Qualifier("restTemplateBean") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public <T> T sendHttpRequest(String url,
                                 HttpMethod httpMethod,
                                 HttpEntity<?> httpEntity,
                                 Class<T> responseClass)
            throws JsonProcessingException {
        try {
            ResponseEntity<T> response = restTemplate.exchange(
                    url,
                    httpMethod,
                    httpEntity,
                    responseClass
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
            throw new RuntimeException("Error response code : " + response.getStatusCode());
        } catch (HttpStatusCodeException exception) {
            String errorPayload = exception.getResponseBodyAsString();
            if (errorPayload.isEmpty()) {
                throw new RuntimeException("Error response code : " + exception.getStatusCode());
            }
            String errorDesc = this.parseTXPGServiceException(errorPayload);

            throw new RuntimeException("Error response : " + exception.getStatusCode() + "/" + errorDesc);
        }
    }

    public <T> T sendGetRequest(String url,
                                Class<T> responseClass)
            throws JsonProcessingException {

        try {
            ResponseEntity<T> response = restTemplate.getForEntity(
                    url,
                    responseClass
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }

            throw new RuntimeException("Error response code : " + response.getStatusCode());
        } catch (HttpStatusCodeException exception) {
            String errorPayload = exception.getResponseBodyAsString();
            if (errorPayload.isEmpty()) {
                throw new RuntimeException("Error response code : " + exception.getStatusCode());
            }
            String errorDesc = this.parseTXPGServiceException(errorPayload);

            throw new RuntimeException("Error response : " + errorDesc);
        }
    }


    private String parseTXPGServiceException(String errorPayload) throws JsonProcessingException {
        ExecTranResponse order = Utils.convert2Object(errorPayload, ExecTranResponse.class);
        return order.getErrorDescription();
    }

}

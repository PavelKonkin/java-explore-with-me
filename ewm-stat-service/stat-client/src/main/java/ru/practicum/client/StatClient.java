package ru.practicum.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import ru.practicum.HitDto;
import ru.practicum.ViewStatDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Validated
public class StatClient {
    private final RestTemplate rest;

    @Autowired
    public StatClient(@Value("${stat-server.url}") String serverUrl,
                      RestTemplateBuilder builder) {
        this.rest =
                builder
                        .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl))
                        .requestFactory(HttpComponentsClientHttpRequestFactory::new)
                        .build();
    }

    public void hits(String app, String uri, String ip, LocalDateTime timestamp) {
        HitDto hitDto = new HitDto(app, uri, ip, timestamp);
        try {
            post(hitDto);
        } catch (Exception ignore) {

        }
    }

    public List<ViewStatDto> getStat(String start, String end, List<String> uris, boolean unique) {
        String urisParam = String.join("&uris=", uris);
        String path = String.format("/stats?start=%s&end=%s&uris=%s&unique=%s",
                start, end, urisParam, unique);
        ResponseEntity<List<ViewStatDto>> responseEntity = get(path, new ParameterizedTypeReference<>() {
        });
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            return responseEntity.getBody();
        } else {
            return new ArrayList<>();
        }
    }

    private <T> ResponseEntity<T> get(String path, ParameterizedTypeReference<T> responseType) {
        return makeAndSendRequest(HttpMethod.GET, path, null, responseType);
    }

    private <T> void post(T body) {
        makeAndSendRequest(body);
    }

    private <T> void makeAndSendRequest(@Nullable T body) {
        makeAndSendRequest(HttpMethod.POST, "/hit", body, new ParameterizedTypeReference<>() {
        });
    }

    private <T, R> ResponseEntity<R>  makeAndSendRequest(HttpMethod method, String path, @Nullable T body, ParameterizedTypeReference<R> responseType) {
        HttpEntity<T> requestEntity = new HttpEntity<>(body, defaultHeaders());

        ResponseEntity<R> statServerResponse;
        try {
            statServerResponse = rest.exchange(path, method, requestEntity, responseType);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body(null);
        }
        return statServerResponse;
    }

    private HttpHeaders defaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        return headers;
    }
}

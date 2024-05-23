package ru.practicum.endpointHit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import ru.practicum.requestHit.dto.EndpointHitDto;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Validated
public class EndpointHitClient {
    private final RestTemplate rest;

    @Autowired
    public EndpointHitClient(@Value("${stat-server.url}") String serverUrl,
                             RestTemplateBuilder builder) {
        this.rest =
                builder
                        .uriTemplateHandler(new DefaultUriBuilderFactory(serverUrl))
                        .requestFactory(HttpComponentsClientHttpRequestFactory::new)
                        .build();
    }

    public void hits(String app, String uri, String ip, LocalDateTime timestamp) {
        EndpointHitDto endpointHitDto = new EndpointHitDto(app, uri, ip, timestamp);
        post(endpointHitDto);
    }

    public ResponseEntity<Object> getStat(String start, String end, List<String> uris, boolean unique) {
        String urisParam = String.join("&uris=", uris);
        String path = String.format("/stats?start=%s&end=%s&uris=%s&unique=%s",
                java.net.URLEncoder.encode(start, StandardCharsets.UTF_8),
                java.net.URLEncoder.encode(end, StandardCharsets.UTF_8), urisParam, unique);
        return get(path);
    }

    private ResponseEntity<Object> get(String path) {
        return makeAndSendRequest(HttpMethod.GET, path, null);
    }

    private  <T> void post(T body) {
        makeAndSendRequest(HttpMethod.POST, "/hit", body);
    }

    private <T> ResponseEntity<Object> makeAndSendRequest(HttpMethod method, String path, @Nullable T body) {
        HttpEntity<T> requestEntity = new HttpEntity<>(body, defaultHeaders());

        ResponseEntity<Object> statServerResponse;
        try {
            statServerResponse = rest.exchange(path, method, requestEntity, Object.class);
        } catch (HttpStatusCodeException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsByteArray());
        }
        return prepareGatewayResponse(statServerResponse);
    }

    private HttpHeaders defaultHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        return headers;
    }

    private static ResponseEntity<Object> prepareGatewayResponse(ResponseEntity<Object> response) {
        if (response.getStatusCode().is2xxSuccessful()) {
            return response;
        }

        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.status(response.getStatusCode());

        if (response.hasBody()) {
            return responseBuilder.body(response.getBody());
        }

        return responseBuilder.build();
    }
}

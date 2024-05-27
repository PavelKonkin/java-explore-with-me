package ru.practicum;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import ru.practicum.client.StatClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@SpringBootApplication
public class EwmMainService {
    public static void main(String[] args) {
        SpringApplication.run(EwmMainService.class, args);
    }

    @Bean
    public CommandLineRunner run(ApplicationContext appContext) {
        return args -> {
            // Получаем бин StatClient из контекста
            StatClient statClient = appContext.getBean(StatClient.class);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime past = now.minusHours(1);
            LocalDateTime future = now.plusHours(1);
            String convertedPast = past.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String convertedFuture = future.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            // Тестируем сохранение хита
            statClient.hits("myApp", "/test-uri", "127.0.0.1", now);

            // Тестируем получение статистики
            ResponseEntity<Object> response = statClient.getStat(convertedPast, convertedFuture,
                    List.of("/test-uri"), true);
            System.out.println("Response: " + response.getBody());

            // Тестируем сохранение второго хита
            statClient.hits("myApp2", "/test-uri2", "127.0.0.1", now);
            statClient.hits("myApp2", "/test-uri2", "127.0.0.1", now);

            // Тестируем получение статистики неуникальных хитов
            response = statClient.getStat(convertedPast, convertedFuture,
                    List.of("/test-uri", "/test-uri2"), false);
            System.out.println("Response: " + response.getBody());
        };
    }
}
package com.khm1102.mediprice.config;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Configuration
@ComponentScan(
        basePackages = "com.khm1102.mediprice",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ANNOTATION,
                classes = {Controller.class, RestController.class, ControllerAdvice.class}
        )
)
public class AppConfig {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();

        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yml"));

        Properties properties = yaml.getObject();
        if (properties == null) {
            throw new IllegalStateException("application.yml 로드 실패: 파일이 없거나 형식이 올바르지 않습니다.");
        }
        loadDotEnv(properties);
        configurer.setProperties(properties);

        return configurer;
    }

    /**
     * 프로젝트 루트의 .env 파일을 읽어 Properties에 추가한다.
     * <p>
     * OS 환경변수 또는 JVM 시스템 프로퍼티로 이미 설정된 값은 덮어쓰지 않는다.
     * .env 파일이 없거나 읽기 실패 시 무시한다 (운영 환경은 OS 환경변수 사용).
     */
    private static void loadDotEnv(Properties target) {
        File envFile = new File(System.getProperty("user.dir"), ".env");
        if (!envFile.exists()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(envFile, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int idx = line.indexOf('=');
                if (idx <= 0) {
                    continue;
                }
                String key = line.substring(0, idx).trim();
                String value = line.substring(idx + 1).trim();
                // OS 환경변수·JVM 프로퍼티가 우선 — .env는 개발 편의 용도
                if (System.getenv(key) == null && System.getProperty(key) == null) {
                    target.setProperty(key, value);
                }
            }
        } catch (IOException e) {
            System.err.println("[AppConfig] .env 로드 실패: " + e.getMessage());
        }
    }

    @Bean
    public JsonMapper jsonMapper() {
        return JsonMapper.builder()
                .findAndAddModules()
                .build();
    }
}

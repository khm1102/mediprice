package com.khm1102.mediprice.global.config;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.dataformat.xml.XmlMapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.Executor;

@Configuration
@EnableScheduling
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
                    System.setProperty(key, value); // 서블릿 컨텍스트에서도 읽을 수 있도록
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

    /** 심평원 API XML 응답 파싱 전용. RestClient는 인스턴스마다 별도 ObjectMapper 사용 권장. */
    @Bean
    public XmlMapper hiraXmlMapper() {
        return XmlMapper.builder()
                .findAndAddModules()
                .build();
    }

    /** 의료기관 상세 5개 API 병렬 호출 전용 풀. 동시에 5개 future 처리 가정. */
    @Bean(name = "hiraDetailExecutor")
    public Executor hiraDetailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("hira-detail-");
        executor.initialize();
        return executor;
    }

    /**
     * BatchService.syncAll이 7개 SyncService를 동시에 dispatch 하는 전용 풀.
     * <p>
     * 각 SyncService 내부는 자체 워커 풀을 띄우므로 본 풀의 스레드는 sync() 호출이 끝날 때까지
     * 점유. 7개 동시 + chain된 Price 1개 → corePool 8로 여유.
     */
    @Bean(name = "hiraBatchExecutor")
    public Executor hiraBatchExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("hira-batch-");
        executor.initialize();
        return executor;
    }

    /**
     * BatchAdminApiController가 수동 트리거를 백그라운드로 던질 때 사용하는 전용 풀.
     * <p>
     * RUNNING AtomicBoolean으로 한 번에 1건만 직렬 실행되므로 single-thread로 충분.
     * 별도 풀을 두는 이유는 (1) hiraBatchExecutor와 책임 분리, (2) 컨트롤러 테스트에서
     * direct executor({@code Runnable::run})를 주입해 동기 실행으로 만들기 위한 seam 제공.
     */
    @Bean(name = "batchAdminExecutor")
    public Executor batchAdminExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(8);
        executor.setThreadNamePrefix("batch-admin-");
        executor.initialize();
        return executor;
    }
}

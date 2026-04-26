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
        configurer.setProperties(properties);

        return configurer;
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

    /** 의료기관 상세 4개 API 병렬 호출 전용 풀. 동시에 4개 future 처리 가정. */
    @Bean(name = "hiraDetailExecutor")
    public Executor hiraDetailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("hira-detail-");
        executor.initialize();
        return executor;
    }
}

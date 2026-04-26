package com.khm1102.mediprice.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.JstlView;
import tools.jackson.databind.json.JsonMapper;

/**
 * Servlet 컨텍스트 전용 설정.
 * <p>
 * ComponentScan은 Controller/RestController/ControllerAdvice만 포함 — root 컨텍스트({@link AppConfig})에서는
 * 동일 어노테이션을 제외한다. 같은 빈이 두 컨텍스트에 중복 등록되는 것을 방지하기 위함.
 * <p>
 * CORS 정책은 SecurityConfig의 {@code CorsConfigurationSource} 빈에서 단일 관리 — 여기에 추가 정의 금지.
 */
@Configuration
@EnableWebMvc
@ComponentScan(
        basePackages = "com.khm1102.mediprice",
        includeFilters = @ComponentScan.Filter(
                type = FilterType.ANNOTATION,
                classes = {Controller.class, RestController.class, ControllerAdvice.class}
        ),
        useDefaultFilters = false
)
public class WebMvcConfig implements WebMvcConfigurer {

    private final JsonMapper jsonMapper;

    public WebMvcConfig(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Bean
    public ViewResolver viewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setViewClass(JstlView.class);
        resolver.setPrefix("/WEB-INF/views/");
        resolver.setSuffix(".jsp");
        return resolver;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("/static/")
                .setCachePeriod(3600);
    }

    @Override
    public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
        builder.withJsonConverter(new JacksonJsonHttpMessageConverter(jsonMapper));
    }

    /**
     * Hira 클라이언트가 jackson-dataformat-xml을 쓰면 Spring이 XML 컨버터까지 자동 등록한다.
     * 우리 REST API는 JSON 전용이므로 클라이언트 Accept 헤더를 무시하고 항상 JSON으로 응답.
     * (브라우저는 application/xml을 q=0.9로 보내서 별도 처리 없이는 XML이 골라짐)
     */
    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.ignoreAcceptHeader(true)
                .defaultContentType(MediaType.APPLICATION_JSON);
    }
}

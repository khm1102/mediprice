package com.khm1102.mediprice.config;

import com.khm1102.mediprice.filter.TraceIdFilter;
import jakarta.servlet.Filter;
import jakarta.servlet.ServletRegistration;
import org.jspecify.annotations.NullMarked;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

@NullMarked
public class WebAppInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {

    @Override
    protected Class<?>[] getRootConfigClasses() {
        return new Class[]{AppConfig.class, JpaConfig.class, SecurityConfig.class, CacheConfig.class};
    }

    @Override
    protected Class<?>[] getServletConfigClasses() {
        return new Class[]{WebMvcConfig.class};
    }

    @Override
    protected String[] getServletMappings() {
        return new String[]{"/"};
    }

    @Override
    protected Filter[] getServletFilters() {
        // TraceIdFilter를 가장 앞에 배치 — 다른 필터/컨트롤러의 모든 로그에 traceId가 잡힘
        TraceIdFilter traceIdFilter = new TraceIdFilter();

        CharacterEncodingFilter encodingFilter = new CharacterEncodingFilter();
        encodingFilter.setEncoding("UTF-8");
        encodingFilter.setForceEncoding(true);

        return new Filter[]{traceIdFilter, encodingFilter};
    }

    @Override
    protected void customizeRegistration(ServletRegistration.Dynamic registration) {
        registration.setInitParameter("throwExceptionIfNoHandlerFound", "true");
    }
}

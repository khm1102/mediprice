package com.khm1102.mediprice.global.config;

import com.khm1102.mediprice.global.filter.TraceIdFilter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import org.jspecify.annotations.NullMarked;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

import java.util.EnumSet;

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
    public void onStartup(ServletContext servletContext) throws ServletException {
        super.onStartup(servletContext);

        // JSP 페이지 응답은 Tomcat/JSP forward와 충돌할 수 있어 ETag 대상에서 제외한다.
        // API 응답만 body hash 기반 weak ETag를 생성한다.
        ShallowEtagHeaderFilter etagFilter = new ShallowEtagHeaderFilter();
        etagFilter.setWriteWeakETag(true);
        FilterRegistration.Dynamic etagRegistration =
                servletContext.addFilter("shallowEtagHeaderFilter", etagFilter);
        etagRegistration.addMappingForUrlPatterns(
                EnumSet.of(DispatcherType.REQUEST),
                false,
                "/api/*"
        );
    }

    @Override
    protected void customizeRegistration(ServletRegistration.Dynamic registration) {
        registration.setInitParameter("throwExceptionIfNoHandlerFound", "true");
    }
}

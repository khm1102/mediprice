package com.khm1102.mediprice.global.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 페이지 요청(@Controller) 전역 예외 핸들러.
 * <p>
 * {@link GlobalExceptionHandler}는 {@code @RestController} 전용(JSON 응답)이고,
 * 본 핸들러는 JSP 뷰를 반환하는 페이지 흐름과 디스패처 수준 404를 처리한다.
 * <p>
 * {@code @Order} 최하위로 등록해 RestController 예외는 GlobalExceptionHandler가 먼저 처리하도록 보장.
 */
@Slf4j
@ControllerAdvice
@Order(Integer.MAX_VALUE)
public class PageExceptionHandler {

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ModelAndView handleNotFound(HttpServletRequest request, HttpServletResponse response) {
        log.warn("404 Not Found: {}", request.getRequestURI());
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return new ModelAndView("error/404");
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleError(Exception e, HttpServletRequest request, HttpServletResponse response) {
        log.error("500 Internal Error: {}", request.getRequestURI(), e);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return new ModelAndView("error/500");
    }
}

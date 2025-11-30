package com.aishoppingguide.orchestrator.web;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AccelBufferingFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (response instanceof HttpServletResponse http) {
            http.setHeader("X-Accel-Buffering", "no");
        }
        chain.doFilter(request, response);
    }
}

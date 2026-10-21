package org.soroth.springauthorizationserver.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TrimTrailingSpaceFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        if (uri != null && uri.endsWith(" ")) {
            HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(request) {
                @Override
                public String getRequestURI() {
                    return uri.stripTrailing();
                }

                @Override
                public StringBuffer getRequestURL() {
                    return new StringBuffer(super.getRequestURL().toString().stripTrailing());
                }

                @Override
                public String getServletPath() {
                    return super.getServletPath().stripTrailing();
                }
            };
            filterChain.doFilter(wrappedRequest, response);
            return;
        }
        filterChain.doFilter(request, response);
    }
}

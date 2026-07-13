package bg.rezerv.cas.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Слага userId/correlationId от gateway headers в MDC за логовете. */
@Component
public class ContextHeaderFilter extends OncePerRequestFilter {

    static final String MDC_USER_ID = "userId";
    static final String MDC_CORRELATION_ID = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(ContextHeaders.CORRELATION_ID);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        String userId = request.getHeader(ContextHeaders.USER_ID);
        try {
            MDC.put(MDC_CORRELATION_ID, correlationId);
            if (userId != null) {
                MDC.put(MDC_USER_ID, userId);
            }
            response.setHeader(ContextHeaders.CORRELATION_ID, correlationId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_CORRELATION_ID);
            MDC.remove(MDC_USER_ID);
        }
    }
}

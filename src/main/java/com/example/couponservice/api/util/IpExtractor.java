package com.example.couponservice.api.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * Extracts the client IP address from an incoming HTTP request.
 *
 * <p><strong>Warning:</strong> The {@code X-Forwarded-For} header can be spoofed by clients.
 * In production, this service must be deployed behind a trusted load balancer that
 * overwrites this header with the actual client IP before forwarding the request.
 */
@Component
public class IpExtractor {

    /**
     * Returns the client IP address, consulting headers set by reverse proxies before
     * falling back to the direct remote address.
     *
     * <p>Priority order:
     * <ol>
     *   <li>{@code X-Forwarded-For} — first token only (may contain a comma-separated chain)</li>
     *   <li>{@code X-Real-IP}</li>
     *   <li>{@link HttpServletRequest#getRemoteAddr()}</li>
     * </ol>
     *
     * @param request the incoming HTTP servlet request
     * @return the resolved client IP address; never {@code null}
     */
    public String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}

package hn.shadowcore.mercadox.email.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Verifies the X-Hub-Signature-256 header on every POST to /webhook.
 *
 * Meta computes HMAC-SHA256 over the raw body using the app secret and sends the
 * result as "sha256=<hex>". We recompute and compare using MessageDigest.isEqual
 * (constant-time) to prevent timing attacks. GET requests (hub challenge) carry
 * no signature and are skipped entirely.
 *
 * Must run before Jackson reads the body — the filter wraps the request in a
 * CachedBodyHttpServletRequest so the body remains readable downstream.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WhatsAppSignatureVerificationFilter extends OncePerRequestFilter {

    private static final String SIGNATURE_HEADER = "X-Hub-Signature-256";
    private static final String HMAC_ALGORITHM    = "HmacSHA256";
    private static final String WEBHOOK_PATH      = "/webhook";

    @Value("${whatsapp.api.app-secret}")
    private String appSecret;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only intercept POST — GET (hub challenge) requires no signature
        return !HttpMethod.POST.matches(request.getMethod())
                || !WEBHOOK_PATH.equals(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(request);

        String signatureHeader = cachedRequest.getHeader(SIGNATURE_HEADER);
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            log.warn("Rejected webhook POST — missing or malformed {} header", SIGNATURE_HEADER);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Missing webhook signature");
            return;
        }

        String receivedHex = signatureHeader.substring("sha256=".length());

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(appSecret.getBytes(), HMAC_ALGORITHM));
            byte[] computed = mac.doFinal(cachedRequest.getCachedBody());
            String computedHex = HexFormat.of().formatHex(computed);

            // Constant-time comparison — must not short-circuit on first mismatch
            if (!MessageDigest.isEqual(computedHex.getBytes(), receivedHex.getBytes())) {
                log.warn("Rejected webhook POST — HMAC-SHA256 signature mismatch");
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid webhook signature");
                return;
            }

        } catch (Exception e) {
            log.error("HMAC verification failed with exception", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        chain.doFilter(cachedRequest, response);
    }
}

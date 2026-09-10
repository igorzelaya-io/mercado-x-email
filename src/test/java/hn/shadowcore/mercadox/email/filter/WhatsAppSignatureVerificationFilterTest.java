package hn.shadowcore.mercadox.email.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class WhatsAppSignatureVerificationFilterTest {

    private static final String APP_SECRET = "test-app-secret";

    private WhatsAppSignatureVerificationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new WhatsAppSignatureVerificationFilter();
        ReflectionTestUtils.setField(filter, "appSecret", APP_SECRET);
    }

    @Test
    void onlyWebhookPostRequestsAreFiltered() {
        MockHttpServletRequest get = request("GET", "/webhook", new byte[0]);
        MockHttpServletRequest otherPost = request("POST", "/health", new byte[0]);
        MockHttpServletRequest webhookPost = request("POST", "/webhook", new byte[0]);

        assertThat(filter.shouldNotFilter(get)).isTrue();
        assertThat(filter.shouldNotFilter(otherPost)).isTrue();
        assertThat(filter.shouldNotFilter(webhookPost)).isFalse();
    }

    @Test
    void missingOrMalformedSignatureReturnsForbidden() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletResponse missingResponse = new MockHttpServletResponse();
        filter.doFilterInternal(request("POST", "/webhook", "{}".getBytes(StandardCharsets.UTF_8)),
                missingResponse, chain);

        MockHttpServletRequest malformed = request("POST", "/webhook", new byte[0]);
        malformed.addHeader("X-Hub-Signature-256", "md5=bad");
        MockHttpServletResponse malformedResponse = new MockHttpServletResponse();
        filter.doFilterInternal(malformed, malformedResponse, chain);

        assertThat(missingResponse.getStatus()).isEqualTo(403);
        assertThat(malformedResponse.getStatus()).isEqualTo(403);
        verify(chain, never()).doFilter(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void invalidSignatureReturnsForbidden() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = request("POST", "/webhook", "payload".getBytes(StandardCharsets.UTF_8));
        request.addHeader("X-Hub-Signature-256", "sha256=deadbeef");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        verify(chain, never()).doFilter(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void validSignaturePassesAReplayableBodyToTheChain() throws Exception {
        byte[] body = "{\"message\":\"hello\"}".getBytes(StandardCharsets.UTF_8);
        MockHttpServletRequest request = request("POST", "/webhook", body);
        request.addHeader("X-Hub-Signature-256", "sha256=" + signature(body));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        ArgumentCaptor<ServletRequest> requestCaptor = ArgumentCaptor.forClass(ServletRequest.class);
        verify(chain).doFilter(requestCaptor.capture(), org.mockito.ArgumentMatchers.eq(response));
        CachedBodyHttpServletRequest cached = (CachedBodyHttpServletRequest) requestCaptor.getValue();
        assertThat(cached.getCachedBody()).isEqualTo(body);
        assertThat(cached.getInputStream().readAllBytes()).isEqualTo(body);
        assertThat(cached.getInputStream().readAllBytes()).isEqualTo(body);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void hmacSetupFailureReturnsServerError() throws Exception {
        ReflectionTestUtils.setField(filter, "appSecret", null);
        byte[] body = "payload".getBytes(StandardCharsets.UTF_8);
        MockHttpServletRequest request = request("POST", "/webhook", body);
        request.addHeader("X-Hub-Signature-256", "sha256=" + signature(body));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(500);
        verify(chain, never()).doFilter(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    private MockHttpServletRequest request(String method, String path, byte[] body) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setServletPath(path);
        request.setContent(body);
        return request;
    }

    private String signature(byte[] body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(APP_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return HexFormat.of().formatHex(mac.doFinal(body));
    }
}

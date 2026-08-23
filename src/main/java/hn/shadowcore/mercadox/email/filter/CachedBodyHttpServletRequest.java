package hn.shadowcore.mercadox.email.filter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Reads and caches the raw request body once so it can be replayed:
 * first by the HMAC filter for signature verification, then by Jackson
 * inside the controller for normal @RequestBody binding.
 */
public class CachedBodyHttpServletRequest extends HttpServletRequestWrapper {

    private final byte[] cachedBody;

    public CachedBodyHttpServletRequest(HttpServletRequest request) throws IOException {
        super(request);
        this.cachedBody = StreamUtils.copyToByteArray(request.getInputStream());
    }

    public byte[] getCachedBody() {
        return cachedBody;
    }

    @Override
    public ServletInputStream getInputStream() {
        return new ReplayableServletInputStream(cachedBody);
    }

    private static class ReplayableServletInputStream extends ServletInputStream {

        private final InputStream source;

        ReplayableServletInputStream(byte[] body) {
            this.source = new ByteArrayInputStream(body);
        }

        @Override public boolean isFinished() { return available() == 0; }
        @Override public boolean isReady() { return true; }
        @Override public void setReadListener(ReadListener listener) { }

        @Override
        public int read() throws IOException {
            return source.read();
        }

        @Override
        public int available() {
            try { return source.available(); } catch (IOException e) { return 0; }
        }
    }
}

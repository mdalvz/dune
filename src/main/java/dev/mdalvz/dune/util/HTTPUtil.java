package dev.mdalvz.dune.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.annotation.Nullable;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor(onConstructor_ = @__(@Inject))
public class HTTPUtil {

    @NonNull
    private final IOUtil ioUtil;

    @NonNull
    private final ObjectMapper objectMapper;

    @NonNull
    private final HostnameVerifier hostnameVerifier;

    @NonNull
    private final SSLSocketFactory sslSocketFactory;

    public <T> @NonNull T executeForJSON(final @NonNull AbstractRequest request,
                                         final @NonNull Class<T> type) throws IOException {
        return execute(request, (response) -> objectMapper.readValue(ioUtil.readString(response.getBody()), type));
    }

    public @NonNull Document executeForHTML(final @NonNull AbstractRequest request) throws IOException {
        return execute(request, (response) -> Jsoup.parse(ioUtil.readString(response.getBody())));
    }

    public <T> @NonNull T execute(final @NonNull AbstractRequest request,
                                  final @NonNull ResponseHandler<T> handler) throws IOException {
        if (new URL(request.getUrl()).openConnection() instanceof HttpURLConnection connection) {
            if (connection instanceof HttpsURLConnection sslConnection) {
                sslConnection.setHostnameVerifier(hostnameVerifier);
                sslConnection.setSSLSocketFactory(sslSocketFactory);
            }
            connection.setDoOutput(true);
            try {
                connection.setRequestMethod(request.getMethod());
                for (final Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                    connection.setRequestProperty(header.getKey(), header.getValue());
                }
                if (request.getBody() != null) {
                    request.getBody().transferTo(connection.getOutputStream());
                }
                final Response.ResponseBuilder builder = Response.builder();
                builder.status(connection.getResponseCode());
                for (final Map.Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
                    if (header.getKey() != null && !header.getValue().isEmpty()) {
                        builder.header(header.getKey(), header.getValue().get(0));
                    }
                }
                builder.body(connection.getInputStream());
                return handler.handle(builder.build());
            } finally {
                connection.disconnect();
            }
        } else {
            throw new IllegalStateException(String.format("URL \"%s\" is not HTTP", request.getUrl()));
        }
    }

    public interface AbstractRequest {

        @NonNull String getUrl();

        @NonNull String getMethod();

        @NonNull Map<String, String> getHeaders();

        @Nullable InputStream getBody();

    }

    @Getter
    public static class Request implements AbstractRequest {

        @NonNull
        private final String url;

        @NonNull
        private final String method;

        @NonNull
        private final Map<String, String> headers;

        @Nullable
        private final InputStream body;

        @Builder
        public Request(final @NonNull String url,
                       final @NonNull String method,
                       final @NonNull @Singular Map<String, String> headers,
                       final @Nullable InputStream body) {
            this.url = url;
            this.method = method;
            this.headers = headers;
            this.body = body;
        }

    }

    @Getter
    public static class URLEncodedFormRequest implements AbstractRequest {

        @NonNull
        private final String url;

        @NonNull
        private final String method;

        @NonNull
        private final Map<String, String> headers;

        @NonNull
        private final Map<String, String> fields;

        @Override
        public @Nullable InputStream getBody() {
            if (method.equals("GET")) {
                return null;
            } else {
                return new ByteArrayInputStream(getBodyString().getBytes(StandardCharsets.UTF_8));
            }
        }

        private @NonNull String getBodyString() {
            return fields.entrySet()
                    .stream()
                    .map((entry) -> String.format(
                            "%s=%s",
                            URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8),
                            URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)
                    ))
                    .collect(Collectors.joining("&"));
        }

        @Builder
        public URLEncodedFormRequest(final @NonNull String url,
                                     final @NonNull String method,
                                     final @NonNull @Singular Map<String, String> headers,
                                     final @NonNull @Singular Map<String, String> fields) {
            final Map<String, String> newHeaders = new HashMap<>();
            for (final Map.Entry<String, String> header : headers.entrySet()) {
                newHeaders.put(header.getKey().toLowerCase(Locale.ROOT), header.getValue());
            }
            if (method.equals("GET")) {
                this.url = url + "?" + getBodyString();
            } else {
                newHeaders.put("content-type", "application/x-www-form-urlencoded");
                this.url = url;
            }
            this.method = method;
            this.headers = newHeaders;
            this.fields = fields;
        }

    }

    @Getter
    public static class Response {

        private final int status;

        @NonNull
        private final Map<String, String> headers;

        @NonNull
        private final InputStream body;

        public @Nullable String getHeader(final @NonNull String name) {
            return headers.get(name.toLowerCase(Locale.ROOT));
        }

        @Builder
        public Response(final int status,
                        final @NonNull @Singular Map<String, String> headers,
                        final @NonNull InputStream body) {
            final Map<String, String> newHeaders = new HashMap<>();
            for (final Map.Entry<String, String> header : headers.entrySet()) {
                newHeaders.put(header.getKey().toLowerCase(Locale.ROOT), header.getValue());
            }
            this.status = status;
            this.body = body;
            this.headers = newHeaders;
        }

    }

    public interface ResponseHandler<T> {

        T handle(final @NonNull Response response) throws IOException;

    }

}

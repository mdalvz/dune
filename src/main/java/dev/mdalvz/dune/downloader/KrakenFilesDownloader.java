package dev.mdalvz.dune.downloader;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dev.mdalvz.dune.util.HTTPUtil;
import jakarta.inject.Inject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.jsoup.nodes.Document;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@AllArgsConstructor(onConstructor_ = @__(@Inject))
public class KrakenFilesDownloader implements Downloader {

    private static final Pattern URL_PATTERN =
            Pattern.compile("^https://krakenfiles.com/view/([^/]+)/file.html$");

    @NonNull
    private final HTTPUtil httpUtil;

    @NonNull
    private final HTTPDownloader httpDownloader;

    @Override
    public void download(final @NonNull String url, final @NonNull TarArchiveOutputStream out) throws IOException {
        final String id = getDownloadId(url);
        if (id == null) {
            throw new IllegalStateException(String.format("Invalid url \"%s\"", url));
        }
        final String token = getDownloadToken(url);
        final String force = getForceDownloadUrl(id, token);
        httpDownloader.download(force, out);
    }

    @Override
    public boolean check(@NonNull String url) {
        return getDownloadId(url) != null;
    }

    private @Nullable String getDownloadId(final @NonNull String url) {
        final Matcher matcher = URL_PATTERN.matcher(url);
        return matcher.matches() ? matcher.group(1) : null;
    }

    private @NonNull String getForceDownloadUrl(final @NonNull String id,
                                                final @NonNull String token) throws IOException {
        final GetForceDownloadUrlResponse data = httpUtil.executeForJSON(HTTPUtil.URLEncodedFormRequest.builder()
                .method("POST")
                .url(String.format("https://krakenfiles.com/download/%s", id))
                .field("token", token)
                .build(),
                GetForceDownloadUrlResponse.class);
        if (!data.getStatus().equals("ok")) {
            throw new IllegalStateException(String.format(
                    "Failed to get force download url with status \"%s\"",
                    data.getStatus()));
        }
        final String force = data.getUrl();
        log.info("Resolved force download url \"{}\"", force);
        return force;
    }

    private @NonNull String getDownloadToken(final @NonNull String url) throws IOException {
        log.info("GET \"{}\"", url);
        final Document document = httpUtil.executeForHTML(HTTPUtil.Request.builder()
                .method("GET")
                .url(url)
                .build());
        final String token = document.selectFirst("#dl-token").attr("value");
        log.info("Resolved download token \"{}\"", token);
        return token;
    }

    @Getter
    private static class GetForceDownloadUrlResponse {

        private final String status;

        private final String url;

        @JsonCreator
        public GetForceDownloadUrlResponse(@JsonProperty(value = "status", required = true) final String status,
                                           @JsonProperty(value = "url", required = true) final String url) {
            this.status = status;
            this.url = url;
        }

    }

}

package dev.mdalvz.dune.downloader;

import dev.mdalvz.dune.util.HTTPUtil;
import dev.mdalvz.dune.util.IOUtil;
import jakarta.inject.Inject;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@AllArgsConstructor(onConstructor_ = @__(@Inject))
public class HTTPDownloader implements Downloader {

    private static final Pattern CONTENT_DISPOSITION_PATTERN =
            Pattern.compile("attachment; filename\\*?=\"?([^\"/]+)\"?");

    private static final Pattern CONTENT_TYPE_PATTERN =
            Pattern.compile("^[^/]+/([^\"/;]+);?.*$");

    @NonNull
    private final HTTPUtil httpUtil;

    @NonNull
    private final IOUtil ioUtil;

    @Override
    public void download(final @NonNull String url, final @NonNull TarArchiveOutputStream out) throws IOException {
        log.info("GET \"{}\"", url);
        final HTTPUtil.Request request = HTTPUtil.Request.builder()
                .method("GET")
                .url(url)
                .build();
        httpUtil.execute(request, (response) -> {
            final String name = getFileName(url, response.getHeader("content-disposition"));
            log.info("Resolved filename \"{}\"", name);
            final String rawContentLength = response.getHeader("content-length");
            if (rawContentLength == null) {
                log.info("Response has no Content-Length");
                final ByteArrayOutputStream buf = new ByteArrayOutputStream();
                ioUtil.transfer(response.getBody(), buf, null);
                final TarArchiveEntry entry = new TarArchiveEntry(name);
                entry.setSize(buf.size());
                out.putArchiveEntry(entry);
                out.write(buf.toByteArray());
                out.closeArchiveEntry();
            } else {
                final long contentLength = Long.parseLong(rawContentLength);
                log.info("Response has Content-Length {}", contentLength);
                final TarArchiveEntry entry = new TarArchiveEntry(name);
                entry.setSize(contentLength);
                out.putArchiveEntry(entry);
                ioUtil.transfer(response.getBody(), out, contentLength);
                out.closeArchiveEntry();
            }
            return null;
        });
    }

    @Override
    public boolean check(@NonNull String url) {
        return true;
    }

    private @NonNull String getFileName(final @NonNull String url,
                                        final @Nullable String contentDisposition) {
        try {
            if (contentDisposition != null) {
                final String nameFromContentDisposition = getFileNameFromContentDisposition(contentDisposition);
                if (nameFromContentDisposition != null) {
                    log.info("Resolved filename from Content-Disposition");
                    return nameFromContentDisposition;
                }
            }
            final String fromUrl = new File(new URI(url).getPath()).getName();
            if (!fromUrl.isBlank()) {
                return fromUrl;
            }
            return UUID.randomUUID().toString();
        } catch (final URISyntaxException e) {
            throw new IllegalStateException("Failed to get file name", e);
        }
    }

    private @Nullable String getFileExtensionFromContentType(final @NonNull String contentType) {
        final Matcher matcher = CONTENT_TYPE_PATTERN.matcher(contentType);
        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    private @Nullable String getFileNameFromContentDisposition(final @NonNull String contentDisposition) {
        final Matcher matcher = CONTENT_DISPOSITION_PATTERN.matcher(contentDisposition);
        if (matcher.matches()) {
            final String name = matcher.group(1);
            if (name.startsWith(".")) {
                return null;
            }
            return name;
        } else {
            return null;
        }
    }

}

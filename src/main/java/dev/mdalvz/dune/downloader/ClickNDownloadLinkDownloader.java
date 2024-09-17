package dev.mdalvz.dune.downloader;

import dev.mdalvz.dune.util.HTMLUtil;
import dev.mdalvz.dune.util.HTTPUtil;
import jakarta.inject.Inject;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@AllArgsConstructor(onConstructor_ = @__(@Inject))
public class ClickNDownloadLinkDownloader implements Downloader {

    private static final Pattern URL_PATTERN =
            Pattern.compile("^https://clickndownload.link/[^/]+$");

    @NonNull
    private final HTTPUtil httpUtil;

    @NonNull
    private final HTMLUtil htmlUtil;

    @NonNull
    private final HTTPDownloader httpDownloader;

    @Override
    public void download(final @NonNull String url, final @NonNull TarArchiveOutputStream out) throws IOException {
        if (!check(url)) {
            throw new IllegalStateException(String.format("Invalid url \"%s\"", url));
        }
        final Map<String, String> slowDownloadFormData =
                getSlowDownloadFormData(url);
        log.info("Resolved id as \"{}\"", slowDownloadFormData.get("id"));
        final Map<String, String> createDownloadLinkFormData =
                getCreateDownloadLinkFormData(url, slowDownloadFormData);
        log.info("Resolved token as \"{}\"", createDownloadLinkFormData.get("rand"));
        log.info("Resolved captcha as \"{}\"", createDownloadLinkFormData.get("code"));
        for (int i = 15; i >= 1; --i) {
            log.info("Waiting {} seconds", i);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                //
            }
        }
        final String downloadLink = getDownloadLink(url, createDownloadLinkFormData);
        log.info("Resolved download link as \"{}\"", downloadLink);
        httpDownloader.download(downloadLink, out);
    }

    @Override
    public boolean check(@NonNull String url) {
        return URL_PATTERN.matcher(url).matches();
    }

    private @NonNull Map<String, String> getSlowDownloadFormData(final @NonNull String url) throws IOException {
        log.info("GET \"{}\"", url);
        final Document document = httpUtil.executeForHTML(HTTPUtil.Request.builder()
                .method("GET")
                .url(url)
                .build());
        return htmlUtil.getFormData(document, "input[name=\"op\"]");
    }

    private @NonNull Map<String, String> getCreateDownloadLinkFormData(
            final @NonNull String url,
            final @NonNull Map<String, String> body) throws IOException {
        log.info("POST \"{}\"", url);
        final Document document = httpUtil.executeForHTML(HTTPUtil.URLEncodedFormRequest.builder()
                .method("POST")
                .url(url)
                .fields(body)
                .build());
        final Map<String, String> formData = htmlUtil.getFormData(document, "input[name=\"op\"]");
        formData.put("code", getCode(document));
        return formData;
    }

    private @NonNull String getDownloadLink(final @NonNull String url,
                                            final @NonNull Map<String, String> body) throws IOException {
        log.info("POST \"{}\"", url);
        final Document document = httpUtil.executeForHTML(HTTPUtil.URLEncodedFormRequest.builder()
                .method("POST")
                .url(url)
                .fields(body)
                .build());
        final String downloadLink = htmlUtil.getAttribute(document, "a.downloadbtn", "href");
        if (downloadLink == null) {
            throw new IllegalStateException("Failed to get download link");
        }
        return downloadLink;
    }

    private @NonNull String getCode(final @NonNull Document document) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i <= 80; ++i) {
            final Element current = document.selectFirst(
                    String.format("td[align=\"right\"] > div > span[style*=\"padding-left:%dpx\"]", i));
            if (current != null) {
                builder.append(current.text().trim());
            }
        }
        return builder.toString();
    }

}

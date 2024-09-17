package dev.mdalvz.dune.downloader;

import jakarta.inject.Inject;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import java.io.IOException;
import java.util.List;

@Slf4j
@AllArgsConstructor(onConstructor_ = @__(@Inject))
public class RootDownloader implements Downloader {

    @NonNull
    private final KrakenFilesDownloader krakenFilesDownloader;

    @NonNull
    private final ClickNDownloadLinkDownloader clickNDownloadLinkDownloader;

    @NonNull
    private final HTTPDownloader httpDownloader;

    @Override
    public void download(@NonNull String url, @NonNull TarArchiveOutputStream out) throws IOException {
        for (final Downloader downloader : getDownloaders()) {
            if (downloader.check(url)) {
                log.info("Resolved downloader as {}", downloader.getClass().getSimpleName());
                downloader.download(url, out);
                break;
            }
        }
    }

    @Override
    public boolean check(@NonNull String url) {
        return true;
    }

    private @NonNull List<Downloader> getDownloaders() {
        return List.of(
                krakenFilesDownloader,
                clickNDownloadLinkDownloader,
                httpDownloader);
    }

}

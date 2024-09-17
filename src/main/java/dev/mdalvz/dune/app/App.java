package dev.mdalvz.dune.app;

import dev.mdalvz.dune.downloader.RootDownloader;
import jakarta.inject.Inject;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import java.io.IOException;

@Slf4j
@AllArgsConstructor(onConstructor_ = @__(@Inject))
public class App {

    private final @NonNull RootDownloader rootDownloader;

    public void run(final @NonNull String[] args) {
        try {
            final TarArchiveOutputStream out = new TarArchiveOutputStream(System.out);
            rootDownloader.download(args[0], out);
            out.flush();
            out.close();
        } catch (IOException e) {
            log.error("FAILED", e);
            System.exit(1);
        }
    }

}

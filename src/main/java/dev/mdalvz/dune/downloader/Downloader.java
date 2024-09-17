package dev.mdalvz.dune.downloader;

import lombok.NonNull;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import java.io.IOException;

public interface Downloader {

    void download(final @NonNull String url, final @NonNull TarArchiveOutputStream out) throws IOException;

    boolean check(final @NonNull String url);

}

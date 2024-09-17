package dev.mdalvz.dune.util;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
public class IOUtil {

    public void transfer(final @NonNull InputStream src,
                         final @NonNull OutputStream dst,
                         final @Nullable Long expectedSize) throws IOException {
        final int chunkSize = 1 << 20;
        final byte[] chunk = new byte[chunkSize];
        long transferred = 0;
        int current;
        while (true) {
            current = src.read(chunk, 0, chunkSize);
            if (current == -1) {
                break;
            }
            dst.write(chunk, 0, current);
            transferred += current;
            if (expectedSize != null) {
                log.info(
                        "Transferred {}% ({}/{})",
                        (int)(((float)transferred / (float)expectedSize) * 100.0f),
                        transferred,
                        expectedSize);
            } else {
                log.info(
                        "Transferred {}",
                        transferred);
            }
        }
    }

    public @NonNull String readString(final @NonNull InputStream in) throws IOException {
        return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }

}

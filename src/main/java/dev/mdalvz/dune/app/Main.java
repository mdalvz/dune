package dev.mdalvz.dune.app;

import com.google.inject.Guice;
import lombok.NonNull;

public class Main {

    public static void main(final @NonNull String[] args) {
        Guice.createInjector(new Module())
                .getInstance(App.class)
                .run(args);
    }
}

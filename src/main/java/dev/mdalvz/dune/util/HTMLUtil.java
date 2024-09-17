package dev.mdalvz.dune.util;

import jakarta.inject.Inject;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@AllArgsConstructor(onConstructor_ = @__(@Inject))
public class HTMLUtil {

    public @NonNull Map<String, String> getFormData(final @NonNull Document document,
                                                    final @NonNull String selector) {
        Element parent = document.selectFirst(selector);
        while (parent != null && !parent.tagName().toLowerCase(Locale.ROOT).equals("form")) {
            parent = parent.parent();
        }
        if (parent == null) {
            throw new IllegalStateException("Selector is not inside of a form element");
        }
        final Map<String, String> formData = new HashMap<>();
        for (final Element element : parent.select("input")) {
            if (element.hasAttr("name")) {
                formData.put(element.attr("name"), element.attr("value"));
            }
        }
        return formData;
    }

    public @Nullable String getAttribute(final @NonNull Document document,
                                         final @NonNull String selector,
                                         final @NonNull String name) {
        final Element element = document.selectFirst(selector);
        if (element == null) {
            return null;
        }
        if (element.hasAttr(name)) {
            return element.attr(name);
        } else {
            return null;
        }
    }

    public @NonNull String getInputValue(final @NonNull Document document,
                                         final @NonNull String selector) {
        final String value = getAttribute(document, selector, "value");
        return value != null ? value : "";
    }

}

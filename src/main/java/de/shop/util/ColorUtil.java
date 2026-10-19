package de.shop.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ColorUtil {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    public static Component parseToComponent(String text) {
        if (text == null) return Component.empty();
        return LEGACY.deserialize(text)
                .decoration(TextDecoration.ITALIC, false);
    }

    public static String translate(String text) {
        if (text == null) return "";
        return text.replace('§', '&');
    }

    public static Component parse(String text) {
        return parseToComponent(translate(text));
    }
}

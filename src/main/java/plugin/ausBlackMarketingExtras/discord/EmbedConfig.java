package plugin.ausBlackMarketingExtras.discord;

import org.bukkit.configuration.ConfigurationSection;
import java.util.*;

public record EmbedConfig(
    String title,
    String description,
    int color,
    String authorName,
    String authorIconUrl,
    String thumbnailUrl,
    String imageUrl,
    List<FieldConfig> fields,
    String footerText,
    String footerIconUrl,
    boolean timestamp
) {
    public record FieldConfig(String name, String value, boolean inline) {}

    public static EmbedConfig empty() {
        return new EmbedConfig("", "", 0, "", "", "", "", List.of(), "", "", false);
    }

    public static EmbedConfig fromSection(ConfigurationSection section) {
        List<FieldConfig> fields = new ArrayList<>();
        for (Map<?, ?> map : section.getMapList("fields")) {
            fields.add(new FieldConfig(
                (String) map.get("name"),
                (String) map.get("value"),
                Boolean.TRUE.equals(map.get("inline"))
            ));
        }
        return new EmbedConfig(
            section.getString("title", ""),
            section.getString("description", ""),
            section.getInt("color", 0),
            section.getString("author.name", ""),
            section.getString("author.icon-url", ""),
            section.getString("thumbnail-url", ""),
            section.getString("image-url", ""),
            fields,
            section.getString("footer.text", ""),
            section.getString("footer.icon-url", ""),
            section.getBoolean("timestamp", false)
        );
    }

    public EmbedConfig withPlaceholders(Map<String, String> placeholders) {
        return new EmbedConfig(
            resolve(title, placeholders),
            resolve(description, placeholders),
            color, authorName, authorIconUrl, thumbnailUrl, imageUrl,
            fields.stream()
                .map(f -> new FieldConfig(
                    resolve(f.name(), placeholders),
                    resolve(f.value(), placeholders),
                    f.inline()))
                .toList(),
            footerText, footerIconUrl, timestamp
        );
    }

    private static String resolve(String text, Map<String, String> placeholders) {
        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}

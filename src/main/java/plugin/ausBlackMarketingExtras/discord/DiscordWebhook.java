package plugin.ausBlackMarketingExtras.discord;

import plugin.ausBlackMarketingExtras.logging.AusCycleLogger;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.time.Instant;

public final class DiscordWebhook {

    private DiscordWebhook() {}

    public static void send(String webhookUrl, EmbedConfig embed) {
        String payload = buildPayload(embed);
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(webhookUrl))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .timeout(Duration.ofSeconds(5))
            .build();
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenAccept(response -> {
                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    AusCycleLogger.info("Discord webhook sent (status " + response.statusCode() + ").");
                } else {
                    AusCycleLogger.warn("Discord webhook returned " + response.statusCode() + ": " + response.body());
                }
            })
            .exceptionally(e -> {
                AusCycleLogger.warn("Discord webhook failed: " + e.getMessage());
                return null;
            });
    }

    static String buildPayload(EmbedConfig embed) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"embeds\":[{");
        sb.append("\"title\":\"").append(escape(embed.title())).append("\",");
        sb.append("\"description\":\"").append(escape(embed.description())).append("\",");
        sb.append("\"color\":").append(embed.color());

        if (!embed.authorName().isEmpty()) {
            sb.append(",\"author\":{\"name\":\"").append(escape(embed.authorName())).append("\"");
            if (!embed.authorIconUrl().isEmpty()) {
                sb.append(",\"icon_url\":\"").append(escape(embed.authorIconUrl())).append("\"");
            }
            sb.append("}");
        }
        if (!embed.thumbnailUrl().isEmpty()) {
            sb.append(",\"thumbnail\":{\"url\":\"").append(escape(embed.thumbnailUrl())).append("\"}");
        }
        if (!embed.imageUrl().isEmpty()) {
            sb.append(",\"image\":{\"url\":\"").append(escape(embed.imageUrl())).append("\"}");
        }
        if (!embed.fields().isEmpty()) {
            sb.append(",\"fields\":[");
            for (int i = 0; i < embed.fields().size(); i++) {
                EmbedConfig.FieldConfig f = embed.fields().get(i);
                if (i > 0) sb.append(",");
                sb.append("{\"name\":\"").append(escape(f.name())).append("\"");
                sb.append(",\"value\":\"").append(escape(f.value())).append("\"");
                sb.append(",\"inline\":").append(f.inline()).append("}");
            }
            sb.append("]");
        }
        if (!embed.footerText().isEmpty()) {
            sb.append(",\"footer\":{\"text\":\"").append(escape(embed.footerText())).append("\"");
            if (!embed.footerIconUrl().isEmpty()) {
                sb.append(",\"icon_url\":\"").append(escape(embed.footerIconUrl())).append("\"");
            }
            sb.append("}");
        }
        if (embed.timestamp()) {
            sb.append(",\"timestamp\":\"").append(Instant.now()).append("\"");
        }

        sb.append("}]}");
        return sb.toString();
    }

    private static String escape(String text) {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}

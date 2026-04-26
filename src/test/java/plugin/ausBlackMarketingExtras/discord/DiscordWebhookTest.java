package plugin.ausBlackMarketingExtras.discord;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DiscordWebhookTest {

    @Test
    void buildPayload_includesAllFields() {
        EmbedConfig embed = new EmbedConfig(
            "Titulo", "Descricao", 15158332,
            "AusTV", "https://icon.url",
            "https://thumb.url", "https://image.url",
            List.of(new EmbedConfig.FieldConfig("Campo", "Valor", true)),
            "Footer text", "https://footer-icon.url",
            false
        );

        String payload = DiscordWebhook.buildPayload(embed);

        assertTrue(payload.contains("\"title\":\"Titulo\""));
        assertTrue(payload.contains("\"description\":\"Descricao\""));
        assertTrue(payload.contains("\"color\":15158332"));
        assertTrue(payload.contains("\"name\":\"AusTV\""));
        assertTrue(payload.contains("\"url\":\"https://thumb.url\""));
        assertTrue(payload.contains("\"url\":\"https://image.url\""));
        assertTrue(payload.contains("\"name\":\"Campo\""));
        assertTrue(payload.contains("\"value\":\"Valor\""));
        assertTrue(payload.contains("\"inline\":true"));
        assertTrue(payload.contains("\"text\":\"Footer text\""));
    }

    @Test
    void buildPayload_omitsEmptyOptionalFields() {
        EmbedConfig embed = new EmbedConfig(
            "Titulo", "Descricao", 0,
            "", "", "", "",
            List.of(), "", "", false
        );

        String payload = DiscordWebhook.buildPayload(embed);

        assertFalse(payload.contains("\"author\""));
        assertFalse(payload.contains("\"thumbnail\""));
        assertFalse(payload.contains("\"image\""));
        assertFalse(payload.contains("\"fields\""));
        assertFalse(payload.contains("\"footer\""));
        assertFalse(payload.contains("\"timestamp\""));
    }

    @Test
    void buildPayload_escapesSpecialCharacters() {
        EmbedConfig embed = new EmbedConfig(
            "Titulo \"com aspas\"", "linha1\nlinha2", 0,
            "", "", "", "", List.of(), "", "", false
        );

        String payload = DiscordWebhook.buildPayload(embed);

        assertTrue(payload.contains("\\\"com aspas\\\""));
        assertTrue(payload.contains("linha1\\nlinha2"));
    }
}

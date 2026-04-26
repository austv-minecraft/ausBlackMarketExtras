package plugin.ausBlackMarketingExtras.discord;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class EmbedConfigTest {

    @Test
    void withPlaceholders_replacesAllOccurrences() {
        EmbedConfig embed = new EmbedConfig(
            "Aberto dia {start_day}",
            "Ciclo {cycle} com {auction_count} leiloes",
            15158332,
            "AusTV", "", "", "",
            List.of(new EmbedConfig.FieldConfig("Periodo", "Dias {start_day} ate {end_day}", true)),
            "Footer", "", false
        );

        EmbedConfig resolved = embed.withPlaceholders(Map.of(
            "start_day", "4",
            "end_day", "6",
            "cycle", "1",
            "auction_count", "3"
        ));

        assertEquals("Aberto dia 4", resolved.title());
        assertEquals("Ciclo 1 com 3 leiloes", resolved.description());
        assertEquals("Dias 4 ate 6", resolved.fields().get(0).value());
    }

    @Test
    void withPlaceholders_leavesUnknownPlaceholdersIntact() {
        EmbedConfig embed = new EmbedConfig(
            "Ola {unknown}", "", 0, "", "", "", "",
            List.of(), "", "", false
        );

        EmbedConfig resolved = embed.withPlaceholders(Map.of("start_day", "4"));

        assertEquals("Ola {unknown}", resolved.title());
    }
}

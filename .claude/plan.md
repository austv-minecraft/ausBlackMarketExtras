# ausBlackMarketingExtras Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Construir um plugin Paper bridge que automatiza o ciclo do Mercado Clandestino (AxDarkAuctions), agendando início/fim por dia do mês, colando/restaurando schematics WorldEdit, spawnando/removendo NPCs Citizens e enviando embeds Discord via webhook.

**Architecture:** O plugin escuta `AxDarkAuctionsLoadEvent` para saber quando a API está pronta, então calcula o delay até as 12:00 do mesmo dia e agenda um `BukkitRunnable` one-shot. Ao disparar, para cada `AuctionEntry` do ciclo: cola schematic (salvando backup antes), spawna NPC e chama `auction.start()` ou `auction.stop()`. Todas as ações são registradas em `logs/YYYY-MM-DD.log`.

**Tech Stack:** Java 25, Paper API 26.1.2, AxDarkAuctions 1.8.0 (JAR local), WorldEdit 7.3.0, Citizens 2.0.35, JUnit 5 (testes unitários de lógica pura)

---

## File Map

| Arquivo | Responsabilidade |
|---|---|
| `AusBlackMarketingExtras.java` | Main plugin — wires tudo no onEnable |
| `config/ConfigManager.java` | Carrega e expõe config.yml |
| `model/AuctionEntry.java` | Record: nome do leilão + npcId |
| `model/CycleConfig.java` | Record: id, startDay, endDay, lista de AuctionEntry |
| `logging/AusCycleLogger.java` | Logger estático — arquivo diário + console |
| `discord/EmbedConfig.java` | Record: todos os campos do embed + resolução de placeholders |
| `discord/DiscordWebhook.java` | Constrói JSON e envia HTTP POST assíncrono |
| `schedule/AuctionScheduler.java` | Calcula delay até trigger-time e agenda BukkitRunnable |
| `listener/DarkAuctionsLoadListener.java` | Ouve AxDarkAuctionsLoadEvent e dispara o scheduler |
| `npc/NpcHandler.java` | Spawn/despawn via CitizensAPI |
| `schematic/SchematicHandler.java` | Paste com backup + restore via WorldEdit |
| `auction/AuctionHandler.java` | Orquestra start/stop de cada ciclo |
| `src/main/resources/config.yml` | Config padrão |
| `src/main/resources/paper-plugin.yml` | Metadados e dependências |
| `build.gradle.kts` | Dependências e tarefas de build |
| `src/test/java/.../AuctionSchedulerTest.java` | Testa cálculo de delay |
| `src/test/java/.../DiscordWebhookTest.java` | Testa construção do JSON payload |
| `src/test/java/.../EmbedConfigTest.java` | Testa resolução de placeholders |

---

## Task 1: Setup do Projeto (build.gradle.kts + paper-plugin.yml + config.yml padrão)

**Files:**
- Modify: `build.gradle.kts`
- Modify: `src/main/resources/paper-plugin.yml`
- Create: `src/main/resources/config.yml`
- Create: `src/main/java/plugin/ausBlackMarketingExtras/AusBlackMarketingExtras.java` (stub)
- Create: `src/test/java/plugin/ausBlackMarketingExtras/` (diretório)

- [ ] **Step 1.1: Atualizar `build.gradle.kts` com dependências e repositórios**

```kotlin
plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.citizensnpcs.co/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
    compileOnly(files("src/main/libs/AxDarkAuctions-1.8.0.jar"))
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.0")
    compileOnly("net.citizensnpcs:citizens-main:2.0.35-SNAPSHOT")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    test {
        useJUnitPlatform()
    }

    runServer {
        minecraftVersion("26.1.2")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
}
```

- [ ] **Step 1.2: Atualizar `paper-plugin.yml` com dependências dos plugins**

```yaml
name: ausBlackMarketingExtras
version: '${version}'

main: plugin.ausBlackMarketingExtras.AusBlackMarketingExtras
bootstrapper: plugin.ausBlackMarketingExtras.AusBlackMarketingExtrasBootstrap
loader: plugin.ausBlackMarketingExtras.AusBlackMarketingExtrasLoader
api-version: '1.21'
load: POSTWORLD

authors: [ ZzPowerTechzZ ]

dependencies:
  server:
    AxDarkAuctions:
      load: BEFORE
      required: true
    WorldEdit:
      load: BEFORE
      required: true
    Citizens:
      load: BEFORE
      required: true
```

- [ ] **Step 1.3: Criar `src/main/resources/config.yml` padrão**

```yaml
schematic: "dark_auction.schem"

trigger-hour: 12
trigger-minute: 0

cycles:
  1:
    start-day: 4
    end-day: 6
    auctions:
      - name: "leilaodocas1"
        npc-id: 1
      - name: "leilaodocas2"
        npc-id: 2
      - name: "leilaodocas3"
        npc-id: 3
  2:
    start-day: 15
    end-day: 17
    auctions:
      - name: "leilaocentro1"
        npc-id: 4
      - name: "leilaocentro2"
        npc-id: 5
      - name: "leilaocentro3"
        npc-id: 6
  3:
    start-day: 24
    end-day: 26
    auctions:
      - name: "leilaodc1"
        npc-id: 7
      - name: "leilaodc2"
        npc-id: 8
      - name: "leilaodc3"
        npc-id: 9

discord:
  enabled: false
  webhook-url: ""
  start-embed:
    title: "🏴 Mercado Clandestino Aberto!"
    description: "O mercado clandestino está funcionando!\nCorra antes que acabe o estoque."
    color: 15158332
    author:
      name: "AusTV Network"
      icon-url: ""
    thumbnail-url: ""
    image-url: ""
    fields:
      - name: "📅 Período"
        value: "Dias {start_day} até {end_day}"
        inline: true
      - name: "⏰ Horário"
        value: "A partir das 12:00"
        inline: true
      - name: "📍 Local"
        value: "Coordenadas secretas no servidor"
        inline: false
    footer:
      text: "AusTV Network • Mercado Clandestino"
      icon-url: ""
    timestamp: true
  end-embed:
    title: "🔒 Mercado Clandestino Encerrado"
    description: "O mercado clandestino foi encerrado.\nAté o próximo ciclo!"
    color: 3092790
    author:
      name: "AusTV Network"
      icon-url: ""
    thumbnail-url: ""
    image-url: ""
    fields:
      - name: "📅 Próximo ciclo"
        value: "Em breve..."
        inline: true
    footer:
      text: "AusTV Network • Mercado Clandestino"
      icon-url: ""
    timestamp: true
```

- [ ] **Step 1.4: Verificar que o build compila sem erros**

```bash
./gradlew build
```
Esperado: `BUILD SUCCESSFUL`

- [ ] **Step 1.5: Commit**

```bash
git add build.gradle.kts src/main/resources/paper-plugin.yml src/main/resources/config.yml
git commit -m "chore: setup project dependencies and default config"
```

---

## Task 2: Model Records

**Files:**
- Create: `src/main/java/plugin/ausBlackMarketingExtras/model/AuctionEntry.java`
- Create: `src/main/java/plugin/ausBlackMarketingExtras/model/CycleConfig.java`

- [ ] **Step 2.1: Criar `AuctionEntry.java`**

```java
package plugin.ausBlackMarketingExtras.model;

public record AuctionEntry(String name, int npcId) {}
```

- [ ] **Step 2.2: Criar `CycleConfig.java`**

```java
package plugin.ausBlackMarketingExtras.model;

import java.util.List;

public record CycleConfig(int id, int startDay, int endDay, List<AuctionEntry> auctions) {}
```

- [ ] **Step 2.3: Compilar**

```bash
./gradlew compileJava
```
Esperado: `BUILD SUCCESSFUL`

- [ ] **Step 2.4: Commit**

```bash
git add src/main/java/plugin/ausBlackMarketingExtras/model/
git commit -m "feat: add AuctionEntry and CycleConfig model records"
```

---

## Task 3: AusCycleLogger

**Files:**
- Create: `src/main/java/plugin/ausBlackMarketingExtras/logging/AusCycleLogger.java`

- [ ] **Step 3.1: Criar `AusCycleLogger.java`**

```java
package plugin.ausBlackMarketingExtras.logging;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

public final class AusCycleLogger {

    private static Path logsDir;
    private static Logger pluginLogger;

    private AusCycleLogger() {}

    public static void init(Path dataFolder, Logger logger) {
        logsDir = dataFolder.resolve("logs");
        pluginLogger = logger;
        try {
            Files.createDirectories(logsDir);
        } catch (IOException e) {
            logger.warning("Failed to create logs directory: " + e.getMessage());
        }
    }

    public static void info(String message) {
        log("INFO", message, null);
    }

    public static void warn(String message) {
        log("WARN", message, null);
    }

    public static void error(String message) {
        log("ERROR", message, null);
    }

    public static void error(String message, Throwable throwable) {
        log("ERROR", message, throwable);
    }

    static String formatEntry(String level, String timeStr, String message) {
        return "[" + timeStr + "] [" + level + "] " + message;
    }

    private static void log(String level, String message, Throwable throwable) {
        String timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String entry = formatEntry(level, timeStr, message);

        writeToFile(entry, throwable);

        switch (level) {
            case "INFO" -> pluginLogger.info(message);
            case "WARN" -> pluginLogger.warning(message);
            case "ERROR" -> {
                pluginLogger.severe(message);
                if (throwable != null) pluginLogger.severe(stackTraceToString(throwable));
            }
        }
    }

    private static void writeToFile(String entry, Throwable throwable) {
        if (logsDir == null) return;
        String fileName = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".log";
        Path logFile = logsDir.resolve(fileName);
        try (BufferedWriter writer = Files.newBufferedWriter(
                logFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(entry);
            writer.newLine();
            if (throwable != null) {
                writer.write(stackTraceToString(throwable));
                writer.newLine();
            }
        } catch (IOException e) {
            if (pluginLogger != null) pluginLogger.warning("Failed to write to log file: " + e.getMessage());
        }
    }

    private static String stackTraceToString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        throwable.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
```

- [ ] **Step 3.2: Compilar**

```bash
./gradlew compileJava
```
Esperado: `BUILD SUCCESSFUL`

- [ ] **Step 3.3: Commit**

```bash
git add src/main/java/plugin/ausBlackMarketingExtras/logging/
git commit -m "feat: add AusCycleLogger with daily file output"
```

---

## Task 4: EmbedConfig + DiscordWebhook (com testes)

**Files:**
- Create: `src/main/java/plugin/ausBlackMarketingExtras/discord/EmbedConfig.java`
- Create: `src/main/java/plugin/ausBlackMarketingExtras/discord/DiscordWebhook.java`
- Create: `src/test/java/plugin/ausBlackMarketingExtras/discord/EmbedConfigTest.java`
- Create: `src/test/java/plugin/ausBlackMarketingExtras/discord/DiscordWebhookTest.java`

- [ ] **Step 4.1: Escrever teste de `EmbedConfig.withPlaceholders` — deve falhar**

```java
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
            "Ciclo {cycle} com {auction_count} leilões",
            15158332,
            "AusTV", "", "", "",
            List.of(new EmbedConfig.FieldConfig("Período", "Dias {start_day} até {end_day}", true)),
            "Footer", "", false
        );

        EmbedConfig resolved = embed.withPlaceholders(Map.of(
            "start_day", "4",
            "end_day", "6",
            "cycle", "1",
            "auction_count", "3"
        ));

        assertEquals("Aberto dia 4", resolved.title());
        assertEquals("Ciclo 1 com 3 leilões", resolved.description());
        assertEquals("Dias 4 até 6", resolved.fields().get(0).value());
    }

    @Test
    void withPlaceholders_leavesUnknownPlaceholdersIntact() {
        EmbedConfig embed = new EmbedConfig(
            "Olá {unknown}", "", 0, "", "", "", "",
            List.of(), "", "", false
        );

        EmbedConfig resolved = embed.withPlaceholders(Map.of("start_day", "4"));

        assertEquals("Olá {unknown}", resolved.title());
    }
}
```

- [ ] **Step 4.2: Executar teste para confirmar falha**

```bash
./gradlew test --tests "plugin.ausBlackMarketingExtras.discord.EmbedConfigTest"
```
Esperado: FAIL — `EmbedConfig not found`

- [ ] **Step 4.3: Criar `EmbedConfig.java`**

```java
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
```

- [ ] **Step 4.4: Executar teste — deve passar**

```bash
./gradlew test --tests "plugin.ausBlackMarketingExtras.discord.EmbedConfigTest"
```
Esperado: PASS

- [ ] **Step 4.5: Escrever teste de `DiscordWebhook.buildPayload` — deve falhar**

```java
package plugin.ausBlackMarketingExtras.discord;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DiscordWebhookTest {

    @Test
    void buildPayload_includesAllFields() {
        EmbedConfig embed = new EmbedConfig(
            "Título", "Descrição", 15158332,
            "AusTV", "https://icon.url",
            "https://thumb.url", "https://image.url",
            List.of(new EmbedConfig.FieldConfig("Campo", "Valor", true)),
            "Footer text", "https://footer-icon.url",
            false
        );

        String payload = DiscordWebhook.buildPayload(embed);

        assertTrue(payload.contains("\"title\":\"Título\""));
        assertTrue(payload.contains("\"description\":\"Descrição\""));
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
            "Título", "Descrição", 0,
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
            "Título \"com aspas\"", "linha1\nlinha2", 0,
            "", "", "", "", List.of(), "", "", false
        );

        String payload = DiscordWebhook.buildPayload(embed);

        assertTrue(payload.contains("\\\"com aspas\\\""));
        assertTrue(payload.contains("linha1\\nlinha2"));
    }
}
```

- [ ] **Step 4.6: Executar teste para confirmar falha**

```bash
./gradlew test --tests "plugin.ausBlackMarketingExtras.discord.DiscordWebhookTest"
```
Esperado: FAIL — `DiscordWebhook not found`

- [ ] **Step 4.7: Criar `DiscordWebhook.java`**

```java
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
```

- [ ] **Step 4.8: Executar todos os testes — devem passar**

```bash
./gradlew test
```
Esperado: PASS para `EmbedConfigTest` e `DiscordWebhookTest`

- [ ] **Step 4.9: Commit**

```bash
git add src/main/java/plugin/ausBlackMarketingExtras/discord/ src/test/
git commit -m "feat: add EmbedConfig and DiscordWebhook with unit tests"
```

---

## Task 5: ConfigManager

**Files:**
- Create: `src/main/java/plugin/ausBlackMarketingExtras/config/ConfigManager.java`

- [ ] **Step 5.1: Criar `ConfigManager.java`**

```java
package plugin.ausBlackMarketingExtras.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import plugin.ausBlackMarketingExtras.discord.EmbedConfig;
import plugin.ausBlackMarketingExtras.model.AuctionEntry;
import plugin.ausBlackMarketingExtras.model.CycleConfig;

import java.util.*;

public final class ConfigManager {

    private final List<CycleConfig> cycles = new ArrayList<>();
    private int triggerHour;
    private int triggerMinute;
    private String schematic;
    private boolean discordEnabled;
    private String webhookUrl;
    private EmbedConfig startEmbed;
    private EmbedConfig endEmbed;

    public ConfigManager(FileConfiguration config) {
        schematic = config.getString("schematic", "dark_auction.schem");
        triggerHour = config.getInt("trigger-hour", 12);
        triggerMinute = config.getInt("trigger-minute", 0);

        ConfigurationSection cyclesSection = config.getConfigurationSection("cycles");
        if (cyclesSection != null) {
            for (String key : cyclesSection.getKeys(false)) {
                ConfigurationSection cs = cyclesSection.getConfigurationSection(key);
                if (cs == null) continue;
                int id = Integer.parseInt(key);
                int startDay = cs.getInt("start-day");
                int endDay = cs.getInt("end-day");
                List<AuctionEntry> entries = new ArrayList<>();
                for (Map<?, ?> map : cs.getMapList("auctions")) {
                    entries.add(new AuctionEntry(
                        (String) map.get("name"),
                        (int) map.get("npc-id")
                    ));
                }
                cycles.add(new CycleConfig(id, startDay, endDay, Collections.unmodifiableList(entries)));
            }
        }

        discordEnabled = config.getBoolean("discord.enabled", false);
        webhookUrl = config.getString("discord.webhook-url", "");

        ConfigurationSection startSection = config.getConfigurationSection("discord.start-embed");
        startEmbed = startSection != null ? EmbedConfig.fromSection(startSection) : EmbedConfig.empty();

        ConfigurationSection endSection = config.getConfigurationSection("discord.end-embed");
        endEmbed = endSection != null ? EmbedConfig.fromSection(endSection) : EmbedConfig.empty();
    }

    public List<CycleConfig> getCycles() {
        return Collections.unmodifiableList(cycles);
    }

    public int getTriggerHour() { return triggerHour; }
    public int getTriggerMinute() { return triggerMinute; }
    public String getSchematic() { return schematic; }
    public boolean isDiscordEnabled() { return discordEnabled; }
    public String getWebhookUrl() { return webhookUrl; }
    public EmbedConfig getStartEmbed() { return startEmbed; }
    public EmbedConfig getEndEmbed() { return endEmbed; }

    public Optional<CycleConfig> findCycleByStartDay(int day) {
        return cycles.stream().filter(c -> c.startDay() == day).findFirst();
    }

    public Optional<CycleConfig> findCycleByEndDay(int day) {
        return cycles.stream().filter(c -> c.endDay() == day).findFirst();
    }
}
```

- [ ] **Step 5.2: Compilar**

```bash
./gradlew compileJava
```
Esperado: `BUILD SUCCESSFUL`

- [ ] **Step 5.3: Commit**

```bash
git add src/main/java/plugin/ausBlackMarketingExtras/config/
git commit -m "feat: add ConfigManager with cycle and discord config loading"
```

---

## Task 6: AuctionScheduler (com teste de cálculo de delay)

**Files:**
- Create: `src/main/java/plugin/ausBlackMarketingExtras/schedule/AuctionScheduler.java`
- Create: `src/test/java/plugin/ausBlackMarketingExtras/schedule/AuctionSchedulerTest.java`

- [ ] **Step 6.1: Escrever teste de `calculateDelayTicks` — deve falhar**

```java
package plugin.ausBlackMarketingExtras.schedule;

import org.junit.jupiter.api.Test;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.*;

class AuctionSchedulerTest {

    @Test
    void calculateDelayTicks_thirtyMinutesBefore_returns36000Ticks() {
        LocalTime trigger = LocalTime.of(12, 0);
        LocalTime now = LocalTime.of(11, 30);
        assertEquals(36000L, AuctionScheduler.calculateDelayTicks(trigger, now));
    }

    @Test
    void calculateDelayTicks_oneSecondBefore_returns20Ticks() {
        LocalTime trigger = LocalTime.of(12, 0, 0);
        LocalTime now = LocalTime.of(11, 59, 59);
        assertEquals(20L, AuctionScheduler.calculateDelayTicks(trigger, now));
    }

    @Test
    void calculateDelayTicks_oneMinuteBefore_returns1200Ticks() {
        LocalTime trigger = LocalTime.of(12, 0);
        LocalTime now = LocalTime.of(11, 59);
        assertEquals(1200L, AuctionScheduler.calculateDelayTicks(trigger, now));
    }
}
```

- [ ] **Step 6.2: Executar teste para confirmar falha**

```bash
./gradlew test --tests "plugin.ausBlackMarketingExtras.schedule.AuctionSchedulerTest"
```
Esperado: FAIL — `AuctionScheduler not found`

- [ ] **Step 6.3: Criar `AuctionScheduler.java`**

```java
package plugin.ausBlackMarketingExtras.schedule;

import org.bukkit.scheduler.BukkitRunnable;
import plugin.ausBlackMarketingExtras.AusBlackMarketingExtras;
import plugin.ausBlackMarketingExtras.auction.AuctionHandler;
import plugin.ausBlackMarketingExtras.config.ConfigManager;
import plugin.ausBlackMarketingExtras.logging.AusCycleLogger;
import plugin.ausBlackMarketingExtras.model.CycleConfig;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public final class AuctionScheduler {

    private AuctionScheduler() {}

    public static void schedule(AusBlackMarketingExtras plugin, ConfigManager config) {
        int today = LocalDate.now().getDayOfMonth();
        LocalTime now = LocalTime.now();
        LocalTime triggerTime = LocalTime.of(config.getTriggerHour(), config.getTriggerMinute());

        AusCycleLogger.info("Server started. Day: " + today + ". Checking auction cycles...");

        Optional<CycleConfig> startCycle = config.findCycleByStartDay(today);
        Optional<CycleConfig> endCycle = config.findCycleByEndDay(today);

        if (startCycle.isEmpty() && endCycle.isEmpty()) {
            AusCycleLogger.info("No auction cycle for day " + today + ".");
            return;
        }

        if (!now.isBefore(triggerTime)) {
            AusCycleLogger.warn("Plugin loaded after trigger time " + triggerTime
                + " (now=" + now + "). No tasks scheduled.");
            return;
        }

        long delayTicks = calculateDelayTicks(triggerTime, now);

        startCycle.ifPresent(cycle -> {
            AusCycleLogger.info("Cycle " + cycle.id() + " START scheduled for "
                + triggerTime + " (in " + delayTicks + " ticks).");
            new BukkitRunnable() {
                @Override
                public void run() {
                    AuctionHandler.startCycle(plugin, config, cycle);
                }
            }.runTaskLater(plugin, delayTicks);
        });

        endCycle.ifPresent(cycle -> {
            AusCycleLogger.info("Cycle " + cycle.id() + " STOP scheduled for "
                + triggerTime + " (in " + delayTicks + " ticks).");
            new BukkitRunnable() {
                @Override
                public void run() {
                    AuctionHandler.stopCycle(plugin, config, cycle);
                }
            }.runTaskLater(plugin, delayTicks);
        });
    }

    public static long calculateDelayTicks(LocalTime triggerTime, LocalTime now) {
        long delaySeconds = now.until(triggerTime, ChronoUnit.SECONDS);
        return delaySeconds * 20L;
    }
}
```

- [ ] **Step 6.4: Executar todos os testes — devem passar**

```bash
./gradlew test
```
Esperado: todos PASS

- [ ] **Step 6.5: Commit**

```bash
git add src/main/java/plugin/ausBlackMarketingExtras/schedule/ src/test/java/plugin/ausBlackMarketingExtras/schedule/
git commit -m "feat: add AuctionScheduler with delay calculation and unit tests"
```

---

## Task 7: DarkAuctionsLoadListener

**Files:**
- Create: `src/main/java/plugin/ausBlackMarketingExtras/listener/DarkAuctionsLoadListener.java`

- [ ] **Step 7.1: Criar `DarkAuctionsLoadListener.java`**

```java
package plugin.ausBlackMarketingExtras.listener;

import com.artillexstudios.axdarkauctions.api.AxDarkAuctionsLoadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import plugin.ausBlackMarketingExtras.AusBlackMarketingExtras;
import plugin.ausBlackMarketingExtras.config.ConfigManager;
import plugin.ausBlackMarketingExtras.logging.AusCycleLogger;
import plugin.ausBlackMarketingExtras.schedule.AuctionScheduler;

public final class DarkAuctionsLoadListener implements Listener {

    private final AusBlackMarketingExtras plugin;
    private final ConfigManager config;

    public DarkAuctionsLoadListener(AusBlackMarketingExtras plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    @EventHandler
    public void onAxDarkAuctionsLoad(AxDarkAuctionsLoadEvent event) {
        AusCycleLogger.info("AxDarkAuctions loaded. Initializing auction scheduler...");
        AuctionScheduler.schedule(plugin, config);
    }
}
```

- [ ] **Step 7.2: Compilar**

```bash
./gradlew compileJava
```
Esperado: `BUILD SUCCESSFUL`

- [ ] **Step 7.3: Commit**

```bash
git add src/main/java/plugin/ausBlackMarketingExtras/listener/
git commit -m "feat: add DarkAuctionsLoadListener to trigger scheduler"
```

---

## Task 8: NpcHandler

**Files:**
- Create: `src/main/java/plugin/ausBlackMarketingExtras/npc/NpcHandler.java`

- [ ] **Step 8.1: Criar `NpcHandler.java`**

```java
package plugin.ausBlackMarketingExtras.npc;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.Location;
import plugin.ausBlackMarketingExtras.logging.AusCycleLogger;

public final class NpcHandler {

    private NpcHandler() {}

    public static void spawn(int npcId, Location location) {
        NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);
        if (npc == null) {
            AusCycleLogger.error("NPC id=" + npcId + " not found in Citizens. Skipping spawn.");
            return;
        }
        try {
            npc.spawn(location);
            AusCycleLogger.info("NPC id=" + npcId + " spawned at " + formatLoc(location) + ".");
        } catch (Exception e) {
            AusCycleLogger.error("Failed to spawn NPC id=" + npcId + ": " + e.getMessage(), e);
        }
    }

    public static void despawn(int npcId) {
        NPC npc = CitizensAPI.getNPCRegistry().getById(npcId);
        if (npc == null) {
            AusCycleLogger.error("NPC id=" + npcId + " not found in Citizens. Skipping despawn.");
            return;
        }
        try {
            npc.despawn();
            AusCycleLogger.info("NPC id=" + npcId + " despawned.");
        } catch (Exception e) {
            AusCycleLogger.error("Failed to despawn NPC id=" + npcId + ": " + e.getMessage(), e);
        }
    }

    private static String formatLoc(Location loc) {
        return loc.getWorld().getName()
            + " (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
    }
}
```

- [ ] **Step 8.2: Compilar**

```bash
./gradlew compileJava
```
Esperado: `BUILD SUCCESSFUL`

- [ ] **Step 8.3: Commit**

```bash
git add src/main/java/plugin/ausBlackMarketingExtras/npc/
git commit -m "feat: add NpcHandler for Citizens spawn/despawn"
```

---

## Task 9: SchematicHandler

**Files:**
- Create: `src/main/java/plugin/ausBlackMarketingExtras/schematic/SchematicHandler.java`

- [ ] **Step 9.1: Criar `SchematicHandler.java`**

```java
package plugin.ausBlackMarketingExtras.schematic;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Location;
import plugin.ausBlackMarketingExtras.logging.AusCycleLogger;

import java.io.*;

public final class SchematicHandler {

    private SchematicHandler() {}

    public static void paste(Location pasteLocation, File schematicFile, File backupFile) {
        try {
            Clipboard schematic = loadClipboard(schematicFile);
            saveBackup(pasteLocation, schematic, backupFile);
            pasteClipboard(schematic, pasteLocation);
            AusCycleLogger.info("Schematic pasted at " + formatLoc(pasteLocation) + ".");
        } catch (Exception e) {
            AusCycleLogger.error("Failed to paste schematic at " + formatLoc(pasteLocation), e);
        }
    }

    public static void restore(Location pasteLocation, File backupFile) {
        if (!backupFile.exists()) {
            AusCycleLogger.error("Backup not found: " + backupFile.getPath() + ". Cannot restore.");
            return;
        }
        try {
            Clipboard backup = loadClipboard(backupFile);
            pasteClipboard(backup, pasteLocation);
            boolean deleted = backupFile.delete();
            if (!deleted) AusCycleLogger.warn("Could not delete backup: " + backupFile.getPath());
            AusCycleLogger.info("Area restored at " + formatLoc(pasteLocation) + ".");
        } catch (Exception e) {
            AusCycleLogger.error("Failed to restore area at " + formatLoc(pasteLocation), e);
        }
    }

    private static Clipboard loadClipboard(File file) throws IOException {
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) throw new IOException("Unknown schematic format: " + file.getName());
        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            return reader.read();
        }
    }

    private static void saveBackup(Location origin, Clipboard reference, File backupFile) throws Exception {
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(origin.getWorld());
        BlockVector3 originVec = BlockVector3.at(origin.getBlockX(), origin.getBlockY(), origin.getBlockZ());
        BlockVector3 clipOrigin = reference.getOrigin();

        BlockVector3 min = originVec.add(reference.getMinimumPoint().subtract(clipOrigin));
        BlockVector3 max = originVec.add(reference.getMaximumPoint().subtract(clipOrigin));

        CuboidRegion region = new CuboidRegion(weWorld, min, max);
        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
            ForwardExtentCopy copy = new ForwardExtentCopy(editSession, region, clipboard, region.getMinimumPoint());
            Operations.complete(copy);
        }

        backupFile.getParentFile().mkdirs();
        try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC
                .getWriter(new FileOutputStream(backupFile))) {
            writer.write(clipboard);
        }
    }

    private static void pasteClipboard(Clipboard clipboard, Location location) throws Exception {
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(location.getWorld());
        BlockVector3 target = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
            Operation operation = new ClipboardHolder(clipboard)
                .createPaste(editSession)
                .to(target)
                .ignoreAirBlocks(false)
                .build();
            Operations.complete(operation);
        }
    }

    private static String formatLoc(Location loc) {
        return loc.getWorld().getName()
            + " (" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + ")";
    }
}
```

- [ ] **Step 9.2: Compilar**

```bash
./gradlew compileJava
```
Esperado: `BUILD SUCCESSFUL`

- [ ] **Step 9.3: Commit**

```bash
git add src/main/java/plugin/ausBlackMarketingExtras/schematic/
git commit -m "feat: add SchematicHandler with paste/backup/restore via WorldEdit"
```

---

## Task 10: AuctionHandler

**Files:**
- Create: `src/main/java/plugin/ausBlackMarketingExtras/auction/AuctionHandler.java`

- [ ] **Step 10.1: Criar `AuctionHandler.java`**

```java
package plugin.ausBlackMarketingExtras.auction;

import com.artillexstudios.axdarkauctions.auctions.Auction;
import com.artillexstudios.axdarkauctions.auctions.AuctionManager;
import org.bukkit.Location;
import plugin.ausBlackMarketingExtras.AusBlackMarketingExtras;
import plugin.ausBlackMarketingExtras.config.ConfigManager;
import plugin.ausBlackMarketingExtras.discord.DiscordWebhook;
import plugin.ausBlackMarketingExtras.logging.AusCycleLogger;
import plugin.ausBlackMarketingExtras.model.AuctionEntry;
import plugin.ausBlackMarketingExtras.model.CycleConfig;
import plugin.ausBlackMarketingExtras.npc.NpcHandler;
import plugin.ausBlackMarketingExtras.schematic.SchematicHandler;

import java.io.File;
import java.util.Map;

public final class AuctionHandler {

    private AuctionHandler() {}

    public static void startCycle(AusBlackMarketingExtras plugin, ConfigManager config, CycleConfig cycle) {
        AusCycleLogger.info("=== START cycle " + cycle.id()
            + " (day " + cycle.startDay() + " → " + cycle.endDay() + ") ===");

        File schematicFile = new File(plugin.getDataFolder(), "schematics/" + config.getSchematic());

        for (AuctionEntry entry : cycle.auctions()) {
            Auction auction = AuctionManager.getAuctions().get(entry.name());
            if (auction == null) {
                AusCycleLogger.error("Auction '" + entry.name() + "' not found in AuctionManager. Skipping.");
                continue;
            }
            if (auction.isRunning()) {
                AusCycleLogger.warn("Auction '" + entry.name() + "' is already running. Skipping.");
                continue;
            }

            Location spawnLoc = auction.getSpawn();
            Location pasteLoc = spawnLoc.clone().add(0, -1, 0);
            File backupFile = new File(plugin.getDataFolder(),
                "schematics/backups/backup_" + cycle.id() + "_" + entry.name() + ".schem");

            SchematicHandler.paste(pasteLoc, schematicFile, backupFile);
            NpcHandler.spawn(entry.npcId(), spawnLoc);
            auction.start();
            AusCycleLogger.info("Auction '" + entry.name() + "' started.");
        }

        if (config.isDiscordEnabled() && !config.getWebhookUrl().isBlank()) {
            Map<String, String> ph = Map.of(
                "start_day", String.valueOf(cycle.startDay()),
                "end_day", String.valueOf(cycle.endDay()),
                "cycle", String.valueOf(cycle.id()),
                "auction_count", String.valueOf(cycle.auctions().size())
            );
            DiscordWebhook.send(config.getWebhookUrl(), config.getStartEmbed().withPlaceholders(ph));
        }

        AusCycleLogger.info("=== Cycle " + cycle.id() + " start complete. ===");
    }

    public static void stopCycle(AusBlackMarketingExtras plugin, ConfigManager config, CycleConfig cycle) {
        AusCycleLogger.info("=== STOP cycle " + cycle.id() + " (day " + cycle.endDay() + ") ===");

        for (AuctionEntry entry : cycle.auctions()) {
            Auction auction = AuctionManager.getAuctions().get(entry.name());
            if (auction == null) {
                AusCycleLogger.error("Auction '" + entry.name() + "' not found in AuctionManager. Skipping.");
                continue;
            }

            Location spawnLoc = auction.getSpawn();
            Location pasteLoc = spawnLoc.clone().add(0, -1, 0);
            File backupFile = new File(plugin.getDataFolder(),
                "schematics/backups/backup_" + cycle.id() + "_" + entry.name() + ".schem");

            auction.stop();
            NpcHandler.despawn(entry.npcId());
            SchematicHandler.restore(pasteLoc, backupFile);
            AusCycleLogger.info("Auction '" + entry.name() + "' stopped and area restored.");
        }

        if (config.isDiscordEnabled() && !config.getWebhookUrl().isBlank()) {
            Map<String, String> ph = Map.of(
                "start_day", String.valueOf(cycle.startDay()),
                "end_day", String.valueOf(cycle.endDay()),
                "cycle", String.valueOf(cycle.id()),
                "auction_count", String.valueOf(cycle.auctions().size())
            );
            DiscordWebhook.send(config.getWebhookUrl(), config.getEndEmbed().withPlaceholders(ph));
        }

        AusCycleLogger.info("=== Cycle " + cycle.id() + " stop complete. ===");
    }
}
```

- [ ] **Step 10.2: Compilar**

```bash
./gradlew compileJava
```
Esperado: `BUILD SUCCESSFUL`

- [ ] **Step 10.3: Commit**

```bash
git add src/main/java/plugin/ausBlackMarketingExtras/auction/
git commit -m "feat: add AuctionHandler orchestrating start/stop cycles"
```

---

## Task 11: Main Plugin Class + Build Final

**Files:**
- Modify: `src/main/java/plugin/ausBlackMarketingExtras/AusBlackMarketingExtras.java`

- [ ] **Step 11.1: Atualizar `AusBlackMarketingExtras.java`**

```java
package plugin.ausBlackMarketingExtras;

import org.bukkit.plugin.java.JavaPlugin;
import plugin.ausBlackMarketingExtras.config.ConfigManager;
import plugin.ausBlackMarketingExtras.listener.DarkAuctionsLoadListener;
import plugin.ausBlackMarketingExtras.logging.AusCycleLogger;

public final class AusBlackMarketingExtras extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        AusCycleLogger.init(getDataFolder().toPath(), getLogger());
        ConfigManager config = new ConfigManager(getConfig());
        AusCycleLogger.info("ausBlackMarketingExtras enabled. Awaiting AxDarkAuctions...");
        getServer().getPluginManager().registerEvents(
            new DarkAuctionsLoadListener(this, config), this
        );
    }

    @Override
    public void onDisable() {
        AusCycleLogger.info("ausBlackMarketingExtras disabled.");
    }
}
```

- [ ] **Step 11.2: Executar todos os testes**

```bash
./gradlew test
```
Esperado: PASS

- [ ] **Step 11.3: Gerar o JAR**

```bash
./gradlew build
```
Esperado: `BUILD SUCCESSFUL` — JAR gerado em `build/libs/`

- [ ] **Step 11.4: Commit final**

```bash
git add src/main/java/plugin/ausBlackMarketingExtras/AusBlackMarketingExtras.java
git commit -m "feat: wire main plugin class and complete implementation"
```

---

## Task 12: Checklist de Teste Manual no Servidor

- [ ] **Step 12.1: Copiar JAR para o servidor local**

Copiar `build/libs/ausBlackMarketingExtras-*.jar` para `plugins/` do servidor local.

- [ ] **Step 12.2: Criar estrutura de pastas e copiar schematic**

```
plugins/ausBlackMarketingExtras/
└── schematics/
    └── dark_auction.schem   ← copiar o arquivo aqui
```

- [ ] **Step 12.3: Verificar no `config.yml` gerado**

Confirmar que `plugins/ausBlackMarketingExtras/config.yml` foi criado com os valores padrão após o primeiro boot.

- [ ] **Step 12.4: Alterar `trigger-minute` para testar em 2 minutos**

Editar `config.yml`:
```yaml
trigger-hour: <hora atual>
trigger-minute: <minuto atual + 2>
```
E setar o `start-day` de um ciclo para o dia atual.

- [ ] **Step 12.5: Reiniciar servidor e observar logs**

Verificar:
- Console mostra `"ausBlackMarketingExtras enabled. Awaiting AxDarkAuctions..."`
- Após AxDarkAuctions carregar: `"AxDarkAuctions loaded. Initializing auction scheduler..."`
- `"Cycle X START scheduled for HH:MM (in NNNN ticks)."`
- Arquivo `plugins/ausBlackMarketingExtras/logs/YYYY-MM-DD.log` criado com as mesmas entradas

- [ ] **Step 12.6: Aguardar o horário e verificar execução**

Verificar nos logs:
- `"=== START cycle X (day N → N) ==="`
- `"Schematic pasted at world (X, Y, Z)."`
- `"NPC id=N spawned at world (X, Y, Z)."`
- `"Auction 'nome' started."`
- `"=== Cycle X start complete. ==="`
- Arquivo de backup criado em `schematics/backups/`
- Schematic visível no mundo
- NPC visível no local do leilão
- Leilão iniciado no AxDarkAuctions

- [ ] **Step 12.7: Testar encerramento**

Repetir steps 12.4–12.6 com `end-day` = dia atual e verificar:
- Leilão encerrado
- NPC removido
- Schematic restaurada (backup deletado)
- Discord embed enviado (se `enabled: true`)

- [ ] **Step 12.8: Verificar log de erro com auction name inválido**

Colocar um nome de leilão inexistente em `config.yml`, reiniciar e confirmar:
- Log mostra `[ERROR] Auction 'nome_invalido' not found in AuctionManager. Skipping.`
- Demais leilões do ciclo continuam sendo processados normalmente

- [ ] **Step 12.9: Commit final de documentação**

```bash
git add docs/ .claude/
git commit -m "docs: add implementation plan and sync spec"
```

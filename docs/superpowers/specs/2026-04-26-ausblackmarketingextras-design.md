# ausBlackMarketingExtras — Design Spec
> Data: 2026-04-26 | Autor: Murilo Weiss | Status: Aprovado

---

## 1. Objetivo e Escopo

Plugin bridge para o `AxDarkAuctions` que automatiza o ciclo do Mercado Clandestino no servidor AusTV Network.

**Responsabilidades:**
- Iniciar 3 leilões simultâneos nos dias configurados às 12:00
- Encerrar os 3 leilões simultâneos nos dias configurados às 12:00
- Colar uma schematic do WorldEdit 1 bloco abaixo do spawn de cada leilão ao iniciar
- Restaurar a área (via backup) ao encerrar
- Spawnar NPC do Citizens em cada local ao iniciar; remover ao encerrar
- Enviar embed para Discord via webhook ao iniciar e ao encerrar

**Fora de escopo:**
- Configuração de leilões no AxDarkAuctions (já existem)
- Criação/edição de NPCs no Citizens (já existem)
- Criação da schematic (já existe)

---

## 2. Dependências

| Plugin | Uso | Tipo |
|---|---|---|
| `AxDarkAuctions 1.8.0` | API de leilões | `compileOnly` (JAR local) |
| `WorldEdit 7.3.0` | Paste/restore de schematics | `compileOnly` (Maven) |
| `Citizens 2.0.35` | Spawn/despawn de NPCs | `compileOnly` (Maven) |
| `Paper API 26.1.2` | Base do plugin | `compileOnly` (Maven) |

**`paper-plugin.yml` — server dependencies:**
```yaml
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

**`build.gradle.kts`:**
```kotlin
compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
compileOnly(files("src/main/libs/AxDarkAuctions-1.8.0.jar"))
compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.0")
compileOnly("net.citizensnpcs:citizens-main:2.0.35")
```

---

## 3. Estrutura de Pacotes

```
plugin.ausBlackMarketingExtras
├── AusBlackMarketingExtras.java          ← main plugin
├── config/
│   └── ConfigManager.java                ← carrega config.yml, expõe valores
├── model/
│   ├── CycleConfig.java                  ← dados de um ciclo (startDay, endDay, auctions)
│   └── AuctionEntry.java                 ← par (auctionName, npcId)
├── schedule/
│   └── AuctionScheduler.java             ← calcula delay → agenda BukkitRunnable
├── auction/
│   └── AuctionHandler.java               ← start/stop via AuctionManager API
├── schematic/
│   └── SchematicHandler.java             ← paste + backup/restore via WorldEdit
├── npc/
│   └── NpcHandler.java                   ← spawn/despawn via Citizens API
├── discord/
│   └── DiscordWebhook.java               ← HTTP POST via java.net.http.HttpClient
└── listener/
    └── DarkAuctionsLoadListener.java     ← ouve AxDarkAuctionsLoadEvent
```

---

## 4. Fluxo Principal

### Startup

```
onEnable()
  └─► registra DarkAuctionsLoadListener
        └─► AxDarkAuctionsLoadEvent dispara (AxDarkAuctions pronto)
              └─► AuctionScheduler.schedule()
                    ├─► dia atual == start-day de algum ciclo?
                    │     └─► agenda BukkitRunnable para trigger-hour:trigger-minute
                    ├─► dia atual == end-day de algum ciclo?
                    │     └─► agenda BukkitRunnable para trigger-hour:trigger-minute
                    └─► nenhum match → nenhuma ação
```

**Edge case:** se `AxDarkAuctionsLoadEvent` disparar após `trigger-hour:trigger-minute`, o plugin loga um aviso e não agenda (dia já passou).

### Início do ciclo (start-day às 12:00) — para cada `AuctionEntry`:

```
1. AuctionManager.getAuctions().get(entry.name())     → Auction
   └─► null ou isRunning() == true → log warn + skip entry
2. auction.getSpawn()                                  → Location spawnLoc
3. SchematicHandler.paste(spawnLoc.add(0,-1,0))
   ├─► captura região e salva backup_{cycleId}_{auctionName}.schem
   └─► cola schematic configurada
4. NpcHandler.spawn(entry.npcId(), spawnLoc)
   └─► CitizensAPI.getNPCRegistry().getById(id).spawn(spawnLoc)
5. auction.start()
```
Após processar todos os 3: `DiscordWebhook.send(startEmbed)`

### Encerramento do ciclo (end-day às 12:00) — para cada `AuctionEntry`:

```
1. AuctionManager.getAuctions().get(entry.name())     → Auction
2. auction.getSpawn()                                  → Location spawnLoc
3. auction.stop()
4. NpcHandler.despawn(entry.npcId())
   └─► CitizensAPI.getNPCRegistry().getById(id).despawn()
5. SchematicHandler.restore(spawnLoc.below(), cycleName, entryName)
   ├─► cola backup_{cycleName}_{entryName}.schem
   └─► deleta arquivo de backup
```
Após processar todos os 3: `DiscordWebhook.send(endEmbed)`

---

## 5. SchematicHandler — Detalhes

**Pasta de schematics:** `plugins/ausBlackMarketingExtras/schematics/`

**Pasta de backups (gerada em runtime):** `plugins/ausBlackMarketingExtras/schematics/backups/`

**Paste:**
1. Carrega schematic via `ClipboardFormats.findByFile(file)`
2. Cria `EditSession` com WorldEdit
3. Cola no `BlockVector3` correspondente a `location.below()`
4. Antes do paste, copia a região (bounding box da schematic) para arquivo de backup

**Restore:**
1. Carrega `backups/backup_{cycleName}_{entryName}.schem`
2. Cola sobre a área via `EditSession`
3. Deleta o arquivo de backup

---

## 6. DiscordWebhook — Payload

Envia `POST` com `Content-Type: application/json` para o `webhook-url`.

```json
{
  "embeds": [{
    "title": "...",
    "description": "...",
    "color": 15158332,
    "author": { "name": "...", "icon_url": "..." },
    "thumbnail": { "url": "..." },
    "image": { "url": "..." },
    "fields": [
      { "name": "...", "value": "...", "inline": true }
    ],
    "footer": { "text": "...", "icon_url": "..." },
    "timestamp": "2026-04-26T12:00:00.000Z"
  }]
}
```

Usa `java.net.http.HttpClient` (Java 11+, disponível no Java 25). Chamada assíncrona via `BukkitRunnable` para não bloquear a thread principal.

**Placeholders resolvidos antes do envio:** `{start_day}`, `{end_day}`, `{cycle}`, `{auction_count}`

---

## 7. Estrutura do `config.yml`

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
  enabled: true
  webhook-url: "https://discord.com/api/webhooks/..."
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

---

## 8. Critérios de Aceite

- [ ] Nos dias 4, 15 e 24 às 12:00 — os 3 leilões do ciclo correspondente iniciam simultaneamente
- [ ] Nos dias 6, 17 e 26 às 12:00 — os 3 leilões do ciclo correspondente encerram simultaneamente
- [ ] A schematic é colada 1 bloco abaixo do spawn de cada leilão ao iniciar
- [ ] Um backup da área é salvo antes de cada paste
- [ ] O NPC é spawnado no spawn do leilão ao iniciar
- [ ] Ao encerrar: leilão para, NPC some, schematic é restaurada via backup
- [ ] Embed Discord é enviado ao iniciar e ao encerrar (quando `enabled: true`)
- [ ] Placeholders `{start_day}`, `{end_day}`, `{cycle}`, `{auction_count}` são resolvidos no embed
- [ ] Se `AxDarkAuctionsLoadEvent` disparar após o horário configurado, plugin loga aviso e não agenda
- [ ] Campos vazios no embed (`image-url: ""`) são omitidos do payload JSON

---

## 9. Riscos e Decisões Técnicas

| Risco | Mitigação |
|---|---|
| `AxDarkAuctionsLoadEvent` disparar após 12:00 | Log de aviso; não agenda task; operador deve reiniciar servidor |
| Auction não encontrado pelo nome | Log de erro com nome; skip da entry; continua com as demais |
| NPC ID inválido no Citizens | Log de erro; skip do NPC; leilão inicia normalmente |
| Falha no paste da schematic | Log de erro; skip do paste; leilão inicia normalmente |
| Webhook inativo/URL inválida | HttpClient com timeout de 5s; falha silenciosa com log de warn |
| Servidor reinicia no meio de um ciclo ativo | `AxDarkAuctionsLoadEvent` re-dispara; scheduler verifica se leilão já está rodando via `auction.isRunning()` antes de chamar `start()` |

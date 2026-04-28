# Broadcast Global de Ciclo — Especificação

## Problem Statement

Quando um ciclo do mercado clandestino inicia ou termina, apenas o Discord é notificado.
Jogadores online no servidor não recebem nenhum aviso em jogo, perdendo a janela de participação.
O objetivo é emitir mensagens globais configuráveis (chat broadcast + title na tela) com suporte a cores MiniMessage.

## Goals

- [ ] Mensagem broadcast enviada a todos os jogadores ao iniciar um ciclo
- [ ] Mensagem broadcast enviada a todos os jogadores ao encerrar um ciclo
- [ ] Mensagens configuráveis no `config.yml` com suporte a `&#RRGGBB` e tags MiniMessage
- [ ] Broadcast desativável via flag `broadcast.enabled: false`
- [ ] Title + subtitle exibidos na tela de cada jogador ao iniciar/encerrar ciclo, configuráveis e opcionais
- [ ] Som tocado para cada jogador junto com o title, configurável por nome de Sound e volume/pitch

## Out of Scope

| Feature | Reason |
|---------|--------|
| Mensagens por permissão/grupo | Fora do escopo — broadcast é global |
| Placeholder de tempo restante | Complexidade extra sem pedido explícito |
| Som por jogador individual | Escopo é global — mesmo som para todos |
| Mensagens por ciclo individual | Uma mensagem global cobre todos os ciclos |

---

## User Stories

### P1: Broadcast ao iniciar ciclo ⭐ MVP

**User Story**: Como administrador do servidor, quero que todos os jogadores online recebam uma mensagem no chat quando o mercado clandestino abrir, para que ninguém perca a janela de participação.

**Why P1**: É o caso principal — sem isso a feature não existe.

**Acceptance Criteria**:

1. WHEN `AuctionHandler.startCycle()` é chamado AND `broadcast.enabled: true` THEN sistema SHALL enviar `broadcast.start-message` para todos os jogadores online via `Bukkit.broadcast(Component)`
2. WHEN a mensagem contém `&#RRGGBB` THEN sistema SHALL converter para `<#RRGGBB>` antes do parse MiniMessage
3. WHEN a mensagem contém tags MiniMessage nativas (`<gold>`, `<#FFD700>`, etc.) THEN sistema SHALL renderizá-las corretamente
4. WHEN `broadcast.start-message` está em branco THEN sistema SHALL não enviar nada (sem NPE ou log de erro)

**Independent Test**: Iniciar um ciclo manualmente e verificar que a mensagem aparece no chat para todos os jogadores conectados.

---

### P1: Broadcast ao encerrar ciclo ⭐ MVP

**User Story**: Como administrador do servidor, quero que todos os jogadores online recebam uma mensagem no chat quando o mercado clandestino fechar.

**Why P1**: Simetria com o start — o encerramento também é um evento relevante para jogadores.

**Acceptance Criteria**:

1. WHEN `AuctionHandler.stopCycle()` é chamado AND `broadcast.enabled: true` THEN sistema SHALL enviar `broadcast.end-message` para todos os jogadores online
2. WHEN `broadcast.end-message` está em branco THEN sistema SHALL não enviar nada

**Independent Test**: Aguardar o fim de um ciclo e verificar a mensagem de encerramento no chat.

---

### P2: Title na tela ao iniciar ciclo

**User Story**: Como jogador online, quero ver um title de destaque na tela quando o mercado clandestino abrir, para ser avisado de forma mais visível do que uma mensagem no chat.

**Why P2**: Impacto visual alto para jogadores que não estão olhando o chat, mas a feature funciona bem sem isso — o broadcast de chat já é o MVP.

**Acceptance Criteria**:

1. WHEN `AuctionHandler.startCycle()` é chamado AND `broadcast.enabled: true` AND `broadcast.start-title` não está em branco THEN sistema SHALL enviar title para cada `Player` online via `player.showTitle(Title.title(titleComponent, subtitleComponent, times))`
2. WHEN `broadcast.start-subtitle` está em branco THEN sistema SHALL usar `Component.empty()` como subtitle (sem NPE)
3. WHEN a mensagem de title contém `&#RRGGBB` THEN sistema SHALL aplicar a mesma conversão de hex do broadcast de chat
4. WHEN `broadcast.start-title` está em branco THEN sistema SHALL não exibir nenhum title (guarda)

**Config esperada**:
```yaml
broadcast:
  start-title: "&#FFD700<bold>Mercado Clandestino</bold>"
  start-subtitle: "<white>Está aberto! Corra para aproveitar</white>"
  title-fade-in: 10      # ticks (default 10 = 0.5s)
  title-stay: 60         # ticks (default 60 = 3s)
  title-fade-out: 20     # ticks (default 20 = 1s)
```

**Independent Test**: Iniciar um ciclo e verificar que o title aparece centralizado na tela por ~3 segundos com fade.

---

### P2: Som ao exibir title

**User Story**: Como jogador online, quero ouvir um som quando o title do mercado aparecer na tela, para que o aviso seja ainda mais perceptível (especialmente para quem está de costas ou em outra janela).

**Why P2**: Complementa o title — um par visual+sonoro é muito mais eficaz. Mas o title já cumpre a função sozinho, por isso é P2.

**Acceptance Criteria**:

1. WHEN title é enviado (start ou end) AND `broadcast.sound` não está em branco THEN sistema SHALL tocar o som para cada `Player` online via `player.playSound(player.getLocation(), Sound.valueOf(soundName), volume, pitch)`
2. WHEN `broadcast.sound` contém um nome inválido de Sound THEN sistema SHALL logar aviso (`[ausBlackMarketing] Sound inválido: X`) e não lançar exceção
3. WHEN `broadcast.sound` está em branco THEN sistema SHALL não tocar nenhum som (guarda — funciona silenciosamente)
4. WHEN `broadcast.sound-volume` ou `broadcast.sound-pitch` estão ausentes THEN sistema SHALL usar defaults (`volume=1.0`, `pitch=1.0`)

**Config esperada**:
```yaml
broadcast:
  sound: "ENTITY_ENDER_DRAGON_GROWL"   # nome exato do enum org.bukkit.Sound
  sound-volume: 1.0
  sound-pitch: 1.0
```

**Independent Test**: Iniciar um ciclo com `sound: ENTITY_ENDER_DRAGON_GROWL` e verificar que o som toca para todos os jogadores online junto com o title.

---

### P2: Title na tela ao encerrar ciclo

**User Story**: Como jogador online, quero ver um title de destaque quando o mercado clandestino fechar.

**Why P2**: Simetria com o title de abertura — igualmente útil para jogadores que não veem o chat.

**Acceptance Criteria**:

1. WHEN `AuctionHandler.stopCycle()` é chamado AND `broadcast.enabled: true` AND `broadcast.end-title` não está em branco THEN sistema SHALL enviar title para cada `Player` online
2. WHEN `broadcast.end-subtitle` está em branco THEN sistema SHALL usar `Component.empty()` como subtitle
3. WHEN `broadcast.end-title` está em branco THEN sistema SHALL não exibir nenhum title

**Config esperada**:
```yaml
broadcast:
  end-title: "&#FF4444<bold>Mercado Clandestino</bold>"
  end-subtitle: "<white>Encerrado. Até o próximo ciclo!</white>"
```

**Independent Test**: Aguardar fim de ciclo e verificar title de encerramento na tela.

---

### P1: Controle de ativação via config ⭐ MVP

**User Story**: Como administrador, quero poder desativar os broadcasts sem remover as mensagens do config, para controle operacional.

**Why P1**: Sem essa flag, desativar exigiria apagar as mensagens — má UX de administração.

**Acceptance Criteria**:

1. WHEN `broadcast.enabled: false` no `config.yml` THEN sistema SHALL não enviar nenhum broadcast de chat nem title (start nem end)
2. WHEN `broadcast.enabled: true` (ou ausente, default `true`) THEN sistema SHALL enviar broadcast de chat e title normalmente

**Independent Test**: Setar `enabled: false`, reiniciar o ciclo e confirmar que nenhuma mensagem aparece.

---

## Edge Cases

- WHEN o servidor não tem jogadores online THEN `Bukkit.broadcast` SHALL ser chamado normalmente (sem crash — API já lida com audience vazia)
- WHEN `broadcast` section está ausente do `config.yml` THEN ConfigManager SHALL usar defaults (`enabled=true`, mensagens vazias → sem broadcast)
- WHEN a string `&#RRGGBB` tem casing misto (ex: `&#ffd700`) THEN regex SHALL capturar case-insensitive
- WHEN MiniMessage recebe tag inválida THEN Adventure SHALL ignorá-la silenciosamente (comportamento padrão da lib)
- WHEN `title-stay`, `title-fade-in` ou `title-fade-out` estão ausentes do config THEN ConfigManager SHALL usar defaults (10, 60, 20 ticks respectivamente)
- WHEN um jogador entra no servidor durante um ciclo ativo THEN sistema SHALL NOT reenviar o title (titles são momentâneos — não há estado persistente a replicar)
- WHEN `broadcast.sound` contém nome inválido (ex: `INVALID_SOUND`) THEN sistema SHALL capturar `IllegalArgumentException` do `Sound.valueOf()`, logar aviso e continuar sem travar
- WHEN servidor não tem jogadores online THEN loop de title+som não executa nenhuma iteração (sem crash)

---

## Requirement Traceability

| Requirement ID | Story | Status |
|----------------|-------|--------|
| BCAST-01 | P1: Broadcast start — envio via Bukkit.broadcast | Pending |
| BCAST-02 | P1: Broadcast start — conversão &#RRGGBB → <#RRGGBB> | Pending |
| BCAST-03 | P1: Broadcast start — guarda contra mensagem em branco | Pending |
| BCAST-04 | P1: Broadcast end — envio via Bukkit.broadcast | Pending |
| BCAST-05 | P1: Broadcast end — guarda contra mensagem em branco | Pending |
| BCAST-06 | P1: Flag enabled/disabled suprime chat E title | Pending |
| BCAST-07 | Edge: defaults quando seção broadcast ausente | Pending |
| BCAST-08 | P2: Title start — exibir via player.showTitle para cada Player | Pending |
| BCAST-09 | P2: Title start — subtitle vazio usa Component.empty() | Pending |
| BCAST-10 | P2: Title start — guarda contra title em branco | Pending |
| BCAST-11 | P2: Title end — exibir via player.showTitle para cada Player | Pending |
| BCAST-12 | P2: Title end — guarda contra title em branco | Pending |
| BCAST-13 | Edge: defaults de timing (fade-in 10, stay 60, fade-out 20 ticks) | Pending |
| BCAST-14 | P2: Som — tocar via player.playSound para cada Player quando title é enviado | Pending |
| BCAST-15 | P2: Som — Sound inválido loga aviso sem lançar exceção | Pending |
| BCAST-16 | P2: Som — guarda contra sound em branco (sem execução) | Pending |
| BCAST-17 | Edge: defaults de volume (1.0) e pitch (1.0) quando ausentes | Pending |

**Coverage:** 17 total, 0 mapeados para tasks ⚠️

---

## Arquivos Impactados

| Arquivo | Tipo de mudança |
|---------|----------------|
| `src/main/resources/config.yml` | Adicionar seção `broadcast` |
| `src/main/java/.../config/ConfigManager.java` | 3 campos de chat + 7 campos de title + 3 campos de som + getters |
| `src/main/java/.../auction/AuctionHandler.java` | Método `broadcast()` + método `broadcastTitleAndSound()` + chamadas em start/stop |

## Dependências Técnicas

- **Nenhuma dependência nova** — Paper 1.21.4 já embute Adventure + MiniMessage na API; `org.bukkit.Sound` já está no Paper
- Imports necessários em `AuctionHandler`:
  - `net.kyori.adventure.text.Component`
  - `net.kyori.adventure.text.minimessage.MiniMessage`
  - `net.kyori.adventure.title.Title`
  - `net.kyori.adventure.title.Title.Times`
  - `java.time.Duration`
  - `org.bukkit.Sound`
- `player.showTitle()` e `player.playSound()` iteram sobre `Bukkit.getOnlinePlayers()` — title+som são por jogador, não globais
- `Sound.valueOf(name)` lança `IllegalArgumentException` para nomes inválidos — deve ser capturado

---

## Success Criteria

- [ ] `./gradlew build` passa sem erros após as mudanças
- [ ] Mensagem com `&#FFD700` renderiza em dourado no chat
- [ ] Mensagem com `<white>...</white>` renderiza em branco
- [ ] `broadcast.enabled: false` suprime broadcast de chat e title
- [ ] Nenhum NPE em servidor sem jogadores online
- [ ] Title com `&#FFD700` renderiza em dourado na tela
- [ ] Title sem subtitle configurado não quebra (subtitle vazio aceito)
- [ ] Timing do title segue os ticks configurados (fade-in/stay/fade-out)
- [ ] Som toca para todos os jogadores junto com o title
- [ ] Sound inválido no config loga aviso sem derrubar o servidor
- [ ] `sound` em branco executa silenciosamente (sem erro)

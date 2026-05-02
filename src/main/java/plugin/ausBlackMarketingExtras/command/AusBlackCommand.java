package plugin.ausBlackMarketingExtras.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import plugin.ausBlackMarketingExtras.AusBlackMarketingExtras;
import plugin.ausBlackMarketingExtras.auction.AuctionHandler;
import plugin.ausBlackMarketingExtras.logging.AusCycleLogger;
import plugin.ausBlackMarketingExtras.model.CycleConfig;
import plugin.ausBlackMarketingExtras.model.CycleSnapshot;
import plugin.ausBlackMarketingExtras.persistence.CycleStateRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class AusBlackCommand implements BasicCommand {

  private static final String USAGE =
      "§eUso: /ausblack <ciclo start <id>|ciclo end <id>|reload|resume <clear|status>>";
  private static final String NO_PERMISSION =
      "§cVocê não tem permissão para executar este comando.";

  private final AusBlackMarketingExtras plugin;
  private final CycleStateRepository stateRepository;

  public AusBlackCommand(AusBlackMarketingExtras plugin, CycleStateRepository stateRepository) {
    this.plugin = plugin;
    this.stateRepository = stateRepository;
  }

  @Override
  public void execute(CommandSourceStack stack, String[] args) {
    CommandSender sender = stack.getSender();

    if (args.length == 0) {
      sender.sendMessage(USAGE);
      return;
    }

    switch (args[0].toLowerCase()) {
      case "reload" -> handleReload(sender);
      case "ciclo" -> handleCiclo(sender, args);
      case "resume" -> handleResume(sender, args);
      default -> sender.sendMessage(USAGE);
    }
  }

  @Override
  public Collection<String> suggest(CommandSourceStack stack, String[] args) {
    List<String> suggestions = new ArrayList<>();

    if (args.length == 1) {
      String partial = args[0].toLowerCase();
      for (String opt : List.of("ciclo", "reload", "resume")) {
        if (opt.startsWith(partial)) suggestions.add(opt);
      }
      return suggestions;
    }

    if (args.length == 2 && args[0].equalsIgnoreCase("ciclo")) {
      String partial = args[1].toLowerCase();
      for (String opt : List.of("start", "end")) {
        if (opt.startsWith(partial)) suggestions.add(opt);
      }
      return suggestions;
    }

    if (args.length == 2 && args[0].equalsIgnoreCase("resume")) {
      String partial = args[1].toLowerCase();
      for (String opt : List.of("clear", "status")) {
        if (opt.startsWith(partial)) suggestions.add(opt);
      }
      return suggestions;
    }

    if (args.length == 3 && args[0].equalsIgnoreCase("ciclo")
        && (args[1].equalsIgnoreCase("start") || args[1].equalsIgnoreCase("end"))) {
      String partial = args[2];
      for (CycleConfig cycle : plugin.getConfigManager().getCycles()) {
        String idStr = String.valueOf(cycle.id());
        if (idStr.startsWith(partial)) suggestions.add(idStr);
      }
      return suggestions;
    }

    return suggestions;
  }

  private void handleReload(CommandSender sender) {
    if (!hasPermission(sender, "ausblack.command.reload")) {
      sender.sendMessage(NO_PERMISSION);
      return;
    }
    AusCycleLogger.info("Command: reload executed by " + sender.getName());
    plugin.reloadPlugin();
    sender.sendMessage("§aPlugin recarregado com sucesso.");
  }

  private void handleCiclo(CommandSender sender, String[] args) {
    if (args.length < 3) {
      sender.sendMessage(USAGE);
      return;
    }

    if (!hasPermission(sender, "ausblack.command.ciclo")) {
      sender.sendMessage(NO_PERMISSION);
      return;
    }

    String action = args[1].toLowerCase();
    if (!action.equals("start") && !action.equals("end")) {
      sender.sendMessage(USAGE);
      return;
    }

    int id;
    try {
      id = Integer.parseInt(args[2]);
    } catch (NumberFormatException e) {
      sender.sendMessage("§cID inválido: '" + args[2] + "'. Use um número inteiro.");
      return;
    }

    Optional<CycleConfig> found = plugin.getConfigManager().getCycles()
        .stream()
        .filter(c -> c.id() == id)
        .findFirst();

    if (found.isEmpty()) {
      sender.sendMessage("§cCiclo com ID " + id + " não encontrado.");
      return;
    }

    CycleConfig cycle = found.get();

    if (action.equals("start")) {
      AusCycleLogger.info("Command: ciclo start " + id + " executed by " + sender.getName());
      AuctionHandler.startCycle(plugin, plugin.getConfigManager(), cycle);
      sender.sendMessage("§aCiclo " + id + " iniciado com sucesso.");
    } else {
      AusCycleLogger.info("Command: ciclo end " + id + " executed by " + sender.getName());
      AuctionHandler.stopCycle(plugin, plugin.getConfigManager(), cycle);
      sender.sendMessage("§aCiclo " + id + " encerrado com sucesso.");
    }
  }

  private void handleResume(CommandSender sender, String[] args) {
    if (args.length < 2) {
      sender.sendMessage(USAGE);
      return;
    }

    String subCmd = args[1].toLowerCase();

    switch (subCmd) {
      case "clear" -> {
        if (!hasPermission(sender, "ausblack.admin")) {
          sender.sendMessage(NO_PERMISSION);
          return;
        }
        stateRepository.clear();
        sender.sendMessage("§a[AusBlack] Estado salvo limpo com sucesso.");
        AusCycleLogger.info("Command: resume clear executed by " + sender.getName());
      }
      case "status" -> {
        if (!hasPermission(sender, "ausblack.command.ciclo")) {
          sender.sendMessage(NO_PERMISSION);
          return;
        }
        List<CycleSnapshot> snapshots = stateRepository.load();
        if (snapshots.isEmpty()) {
          sender.sendMessage("§e[AusBlack] Nenhum estado salvo encontrado.");
          return;
        }
        sender.sendMessage("§e[AusBlack] Estado salvo (" + snapshots.size() + " ciclos):");
        for (CycleSnapshot snap : snapshots) {
          long remainingMs = snap.endTimeEpoch() - System.currentTimeMillis();
          String timeFormatted;
          if (remainingMs <= 0) {
            timeFormatted = "expirado";
          } else {
            long totalSecs = remainingMs / 1000L;
            long minutes = totalSecs / 60;
            long seconds = totalSecs % 60;
            timeFormatted = minutes + "m " + seconds + "s";
          }
          sender.sendMessage("§7- Ciclo " + snap.cycleId()
              + " | Leilão: " + snap.auctionName()
              + " | Restante: " + timeFormatted
              + " | Lance: " + snap.bid());
        }
      }
      default -> sender.sendMessage(USAGE);
    }
  }

  private boolean hasPermission(CommandSender sender, String specificPerm) {
    return sender.hasPermission("ausblack.admin") || sender.hasPermission(specificPerm);
  }
}

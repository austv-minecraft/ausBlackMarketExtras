package plugin.ausBlackMarketingExtras.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import plugin.ausBlackMarketingExtras.AusBlackMarketingExtras;
import plugin.ausBlackMarketingExtras.auction.AuctionHandler;
import plugin.ausBlackMarketingExtras.logging.AusCycleLogger;
import plugin.ausBlackMarketingExtras.model.CycleConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AusBlackCommand implements CommandExecutor, TabCompleter {

    private static final String USAGE = "§eUso: /ausblack <ciclo start <id>|ciclo end <id>|reload>";
    private static final String NO_PERMISSION = "§cVocê não tem permissão para executar este comando.";

    private final AusBlackMarketingExtras plugin;

    public AusBlackCommand(AusBlackMarketingExtras plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {

        if (args.length == 0) {
            sender.sendMessage(USAGE);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "reload" -> {
                return handleReload(sender);
            }
            case "ciclo" -> {
                return handleCiclo(sender, args);
            }
            default -> {
                sender.sendMessage(USAGE);
                return true;
            }
        }
    }

    private boolean handleReload(CommandSender sender) {
        if (!hasPermission(sender, "ausblack.command.reload")) {
            sender.sendMessage(NO_PERMISSION);
            return true;
        }

        AusCycleLogger.info("Command: reload executed by " + sender.getName());
        plugin.reloadPlugin();
        sender.sendMessage("§aPlugin recarregado com sucesso.");
        return true;
    }

    private boolean handleCiclo(CommandSender sender, String[] args) {
        // /ausblack ciclo <start|end> <id>
        if (args.length < 3) {
            sender.sendMessage(USAGE);
            return true;
        }

        if (!hasPermission(sender, "ausblack.command.ciclo")) {
            sender.sendMessage(NO_PERMISSION);
            return true;
        }

        String action = args[1].toLowerCase();
        if (!action.equals("start") && !action.equals("end")) {
            sender.sendMessage(USAGE);
            return true;
        }

        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cID inválido: '" + args[2] + "'. Use um número inteiro.");
            return true;
        }

        Optional<CycleConfig> found = plugin.getConfigManager().getCycles()
                .stream()
                .filter(c -> c.id() == id)
                .findFirst();

        if (found.isEmpty()) {
            sender.sendMessage("§cCiclo com ID " + id + " não encontrado.");
            return true;
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

        return true;
    }

    private boolean hasPermission(CommandSender sender, String specificPerm) {
        return sender.hasPermission("ausblack.admin") || sender.hasPermission(specificPerm);
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {

        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            // /ausblack <tab>
            String partial = args[0].toLowerCase();
            for (String opt : List.of("ciclo", "reload")) {
                if (opt.startsWith(partial)) suggestions.add(opt);
            }
            return suggestions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("ciclo")) {
            // /ausblack ciclo <tab>
            String partial = args[1].toLowerCase();
            for (String opt : List.of("start", "end")) {
                if (opt.startsWith(partial)) suggestions.add(opt);
            }
            return suggestions;
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("ciclo")
                && (args[1].equalsIgnoreCase("start") || args[1].equalsIgnoreCase("end"))) {
            // /ausblack ciclo start|end <tab> — suggest existing cycle IDs
            String partial = args[2];
            for (CycleConfig cycle : plugin.getConfigManager().getCycles()) {
                String idStr = String.valueOf(cycle.id());
                if (idStr.startsWith(partial)) suggestions.add(idStr);
            }
            return suggestions;
        }

        return suggestions;
    }
}

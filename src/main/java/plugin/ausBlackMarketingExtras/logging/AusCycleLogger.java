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

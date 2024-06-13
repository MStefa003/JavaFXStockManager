package com.example.util;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for logging application messages.
 */
public class AppLogger {
    private static final Logger logger = Logger.getLogger(AppLogger.class.getName());

    /**
     * Logs an informational message.
     *
     * @param message the message to log
     */
    public static void logInfo(String message) {
        logger.log(Level.INFO, message);
    }

    /**
     * Logs a warning message.
     *
     * @param message the message to log
     */
    public static void logWarning(String message) {
        logger.log(Level.WARNING, message);
    }

    /**
     * Logs an error message along with a throwable.
     *
     * @param message   the message to log
     * @param throwable the throwable to log
     */
    public static void logError(String message, Throwable throwable) {
        logger.log(Level.SEVERE, message, throwable);
    }

    /**
     * Logs a debug message.
     *
     * @param message the message to log
     */
    public static void logDebug(String message) {
        logger.log(Level.FINE, message);
    }
}

package dev.marggx.surventool.utils;

import com.hypixel.hytale.logger.HytaleLogger;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class Logger {
    private static final Logger INSTANCE = new Logger();

    public static Logger get() {
        return INSTANCE;
    }

    private final HytaleLogger parent = HytaleLogger.get("SurvivalEntityTool");

    public Logger() {
    }

    public void info(String var1) {
        parent.atInfo().log(var1);
    }

    public void info(String var1, @NullableDecl Object var2) {
        parent.atInfo().log(var1, var2);
    }

    public void info(String var1, @NullableDecl Object var2, @NullableDecl Object var3) {
        parent.atInfo().log(var1, var2, var3);
    }

    public void warning(String var1) {
        parent.atWarning().log(var1);
    }

    public void warning(String var1, @NullableDecl Object var2) {
        parent.atWarning().log(var1, var2);
    }

    public void warning(String var1, @NullableDecl Object var2, @NullableDecl Object var3) {
        parent.atWarning().log(var1, var2, var3);
    }

    public void severe(String var1) {
        parent.atSevere().log(var1);
    }

    public void severe(String var1, @NullableDecl Object var2) {
        parent.atSevere().log(var1, var2);
    }

    public void severe(String var1, @NullableDecl Object var2, @NullableDecl Object var3) {
        parent.atSevere().log(var1, var2, var3);
    }
}

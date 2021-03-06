package com.mastereric.meta.util;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.FMLLog;
import org.apache.logging.log4j.Level;
/**
 * Created by eric on 2/27/2017.
 */
public class LogUtility {
    private static final String TAG = "M.E.T.A.";

    public static void warn(String format, Object... data) {
        FMLLog.log(TAG, Level.WARN, format, data);
    }

    public static void debug(String format, Object... data) {
        FMLLog.log(TAG, Level.DEBUG, format, data);
    }

    public static void info(String format, Object... data) {
        FMLLog.log(TAG, Level.INFO, format, data);
    }

    public static void error(String format, Object... data) {
        FMLLog.log(TAG, Level.INFO, format, data);
    }

    public static void infoSided(String format, Object... data) {
        if(FMLCommonHandler.instance().getEffectiveSide().isServer())
            LogUtility.info("Server: "+format, data);
        else if (FMLCommonHandler.instance().getEffectiveSide().isClient())
            LogUtility.info("Client: "+format, data);
    }
}

package com.craisinlord.anthology;

import com.craisinlord.anthology.gravity.GravitySync;
import com.craisinlord.anthology.multipart.MultipartFramework;
import com.craisinlord.anthology.portal.PortalEngine;
import com.craisinlord.anthology.shake.CameraShakeSync;
import com.craisinlord.anthology.time.TimeDilationSync;
import net.minecraft.resources.Identifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class Anthology {
    public static final String MODID = "anthology";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    private Anthology() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    public static void init() {
        GravitySync.register();
        TimeDilationSync.register();
        CameraShakeSync.register();
        MultipartFramework.register();
        PortalEngine.register();
    }
}

package com.craisinlord.anthology.portal.client;

public final class SodiumCompat {
    private static final String[] SUPPORTED_API_CLASSES = {
            "me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer",
            "me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager",
            "me.jellysquid.mods.sodium.client.render.chunk.lists.VisibleChunkCollector",
            "me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList",
            "me.jellysquid.mods.sodium.client.render.chunk.ChunkUpdateType"
    };
    private static final String[] RENDERER_CLASSES = {
            "me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer",
            "org.embeddedt.embeddium.impl.render.EmbeddiumWorldRenderer",
            "net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer"
    };
    private static Boolean loaded;
    private static Boolean rendererPresent;

    private SodiumCompat() {
    }

    public static boolean isLoaded() {
        if (loaded == null) {
            loaded = allPresent(SUPPORTED_API_CLASSES);
        }
        return loaded;
    }

    public static boolean isRendererPresent() {
        if (rendererPresent == null) {
            rendererPresent = anyPresent(RENDERER_CLASSES);
        }
        return rendererPresent;
    }

    private static boolean allPresent(String[] classNames) {
        for (String className : classNames) {
            if (!classPresent(className)) {
                return false;
            }
        }
        return true;
    }

    private static boolean anyPresent(String[] classNames) {
        for (String className : classNames) {
            if (classPresent(className)) {
                return true;
            }
        }
        return false;
    }

    private static boolean classPresent(String className) {
        String resource = className.replace('.', '/') + ".class";
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        if (contextLoader != null && contextLoader.getResource(resource) != null) {
            return true;
        }
        ClassLoader ownLoader = SodiumCompat.class.getClassLoader();
        return ownLoader != null && ownLoader.getResource(resource) != null;
    }
}

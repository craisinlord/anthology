package com.craisinlord.anthology.portal.client;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.lwjgl.opengl.GL15;

public final class PortalVisibilityQueries {
    private static final int MAX_ENTRIES = 128;
    private static final String OPENGL_BACKEND = "OpenGL";
    private static final LinkedHashMap<Pair, QueryState> QUERIES = new LinkedHashMap<>(16, 0.75F, true);

    private PortalVisibilityQueries() {
    }

    static boolean supported() {
        return OPENGL_BACKEND.equals(RenderSystem.getDevice().getDeviceInfo().backendName());
    }

    public static boolean shouldRender(UUID source, UUID destination) {
        if (!supported()) {
            return true;
        }
        QueryState state = QUERIES.get(new Pair(source, destination));
        if (state == null) {
            return true;
        }
        if (state.inFlight && GL15.glGetQueryObjecti(state.query, GL15.GL_QUERY_RESULT_AVAILABLE) != 0) {
            state.visible = GL15.glGetQueryObjecti(state.query, GL15.GL_QUERY_RESULT) != 0;
            state.hasResult = true;
            state.inFlight = false;
        }
        return !state.hasResult || state.visible;
    }

    public static boolean begin(UUID source, UUID destination) {
        if (!supported()) {
            return false;
        }
        Pair pair = new Pair(source, destination);
        QueryState state = QUERIES.computeIfAbsent(pair, key -> new QueryState(GL15.glGenQueries()));
        if (state.inFlight || state.active) {
            trim();
            return false;
        }
        state.active = true;
        try {
            GL15.glBeginQuery(GL15.GL_SAMPLES_PASSED, state.query);
        } catch (RuntimeException | Error exception) {
            state.active = false;
            throw exception;
        }
        trim();
        return true;
    }

    public static void end(UUID source, UUID destination, boolean started) {
        if (!started) {
            return;
        }
        QueryState state = QUERIES.get(new Pair(source, destination));
        if (state == null || !state.active) {
            return;
        }
        GL15.glEndQuery(GL15.GL_SAMPLES_PASSED);
        state.active = false;
        state.inFlight = true;
    }

    public static void clear() {
        for (QueryState state : QUERIES.values()) {
            GL15.glDeleteQueries(state.query);
        }
        QUERIES.clear();
    }

    private static void trim() {
        Iterator<Map.Entry<Pair, QueryState>> iterator = QUERIES.entrySet().iterator();
        while (QUERIES.size() > MAX_ENTRIES && iterator.hasNext()) {
            QueryState state = iterator.next().getValue();
            if (!state.active) {
                GL15.glDeleteQueries(state.query);
                iterator.remove();
            }
        }
    }

    private record Pair(UUID source, UUID destination) {
    }

    private static final class QueryState {
        private final int query;
        private boolean active;
        private boolean inFlight;
        private boolean hasResult;
        private boolean visible = true;

        private QueryState(int query) {
            this.query = query;
        }
    }
}

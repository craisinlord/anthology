package com.craisinlord.anthology.portal.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.MeshData;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.lwjgl.opengl.GL15;

public final class PortalSkyQueries {
    private static final int MAX_ENTRIES = 128;
    private static final LinkedHashMap<Key, QueryState> QUERIES = new LinkedHashMap<>(16, 0.75F, true);

    private PortalSkyQueries() {
    }

    public static boolean shouldRenderSky(UUID source, UUID destination, int level) {
        QueryState state = QUERIES.get(new Key(source, destination, level));
        if (state == null || !PortalVisibilityQueries.supported()) {
            return true;
        }
        poll(state);
        return !state.hasResult || state.visible;
    }

    public static void querySkyVisibility(UUID source, UUID destination, int level, RenderTarget target, MeshData farAperture) {
        if (!PortalVisibilityQueries.supported()) {
            if (farAperture != null) {
                farAperture.close();
            }
            return;
        }
        Key key = new Key(source, destination, level);
        QueryState state = QUERIES.computeIfAbsent(key, ignored -> new QueryState(GL15.glGenQueries()));
        poll(state);
        if (state.inFlight || farAperture == null) {
            if (farAperture != null) {
                farAperture.close();
            }
            trim();
            return;
        }
        PortalViewDraw.drawClip(target, PortalViewPipelines.PORTAL_QUERY, farAperture,
                () -> GL15.glBeginQuery(GL15.GL_SAMPLES_PASSED, state.query),
                () -> {
                    GL15.glEndQuery(GL15.GL_SAMPLES_PASSED);
                    state.inFlight = true;
                });
        trim();
    }

    public static void clear() {
        for (QueryState state : QUERIES.values()) {
            GL15.glDeleteQueries(state.query);
        }
        QUERIES.clear();
    }

    private static void poll(QueryState state) {
        if (state.inFlight && GL15.glGetQueryObjecti(state.query, GL15.GL_QUERY_RESULT_AVAILABLE) != 0) {
            state.visible = GL15.glGetQueryObjecti(state.query, GL15.GL_QUERY_RESULT) != 0;
            state.hasResult = true;
            state.inFlight = false;
        }
    }

    private static void trim() {
        Iterator<Map.Entry<Key, QueryState>> iterator = QUERIES.entrySet().iterator();
        while (QUERIES.size() > MAX_ENTRIES && iterator.hasNext()) {
            QueryState state = iterator.next().getValue();
            if (!state.inFlight) {
                GL15.glDeleteQueries(state.query);
                iterator.remove();
            }
        }
    }

    private record Key(UUID source, UUID destination, int level) {
    }

    private static final class QueryState {
        private final int query;
        private boolean inFlight;
        private boolean hasResult;
        private boolean visible = true;

        private QueryState(int query) {
            this.query = query;
        }
    }
}

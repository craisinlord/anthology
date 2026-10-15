package com.craisinlord.anthology.registry;

import java.util.ServiceLoader;

public interface ContentBootstrap {
    void bootstrap();

    static void ensure() {
        Holder.run();
    }

    final class Holder {
        private static boolean done;

        private Holder() {
        }

        private static synchronized void run() {
            if (done) {
                return;
            }
            done = true;
            ServiceLoader.load(ContentBootstrap.class, ContentBootstrap.class.getClassLoader()).forEach(ContentBootstrap::bootstrap);
        }
    }
}

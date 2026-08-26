package io.donbee.jade.examples.devteam;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Lists classpath directory entries whether running from exploded classes
 * (IDE / mvn test) or from inside a jar (production).
 */
final class JarResources {

    private static final Map<String, List<String>> CACHE = new ConcurrentHashMap<>();

    private JarResources() {
    }

    static List<String> list(String dirResource) throws IOException {
        return CACHE.computeIfAbsent(dirResource, JarResources::listUncached);
    }

    private static List<String> listUncached(String dirResource) {
        URI uri;
        try {
            uri = JarResources.class.getResource(dirResource).toURI();
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Bad resource path " + dirResource, e);
        }
        try {
            if ("jar".equals(uri.getScheme())) {
                try (FileSystem fs = FileSystems.newFileSystem(uri, Map.of())) {
                    Path dir = fs.getPath(dirResource);
                    try (Stream<Path> stream = Files.list(dir)) {
                        return toNames(stream);
                    }
                }
            }
            try (Stream<Path> stream = Files.list(Path.of(uri))) {
                return toNames(stream);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot list classpath dir " + dirResource, e);
        }
    }

    private static List<String> toNames(Stream<Path> stream) {
        List<String> names = new ArrayList<>();
        stream.forEach(p -> names.add(p.getFileName().toString()));
        return names;
    }
}

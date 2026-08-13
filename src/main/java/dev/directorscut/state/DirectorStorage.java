package dev.directorscut.state;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

public final class DirectorStorage {
    private final Map<UUID, DirectorState> states = new HashMap<>();
    private final Properties properties = new Properties();
    private Path file;

    public void open(MinecraftServer server) {
        file = server.getWorldPath(LevelResource.ROOT).resolve("data").resolve("directors_cut.properties");
        properties.clear();
        if (Files.isRegularFile(file)) {
            try (InputStream input = Files.newInputStream(file)) {
                properties.load(input);
            } catch (IOException exception) {
                System.err.println("[Director's Cut] Could not load state: " + exception.getMessage());
            }
        }
    }

    public DirectorState state(UUID playerId) {
        return states.computeIfAbsent(playerId, id -> {
            DirectorState state = new DirectorState(id);
            state.load(properties);
            return state;
        });
    }

    public Collection<DirectorState> loadedStates() {
        return states.values();
    }

    public void save() {
        if (file == null) return;
        for (DirectorState state : states.values()) state.save(properties);
        properties.setProperty("format", "1");
        try {
            Files.createDirectories(file.getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            try (OutputStream output = Files.newOutputStream(temporary)) {
                properties.store(output, "Director's Cut persistent world memory");
            }
            try {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException unsupportedAtomicMove) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            System.err.println("[Director's Cut] Could not save state: " + exception.getMessage());
        }
    }

    public void close() {
        save();
        states.clear();
        properties.clear();
        file = null;
    }
}

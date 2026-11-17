package com.aeroreserve.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Centralised Java object serialization helper.
 * Writes are atomic (temporary file + move) and a backup of the previous
 * file is retained so an interrupted write cannot destroy existing data.
 */
public final class FileStorageUtil {

    private static final Logger LOG = Logger.getLogger(FileStorageUtil.class.getName());

    private FileStorageUtil() {
    }

    public static void ensureDirectory(Path directory) throws IOException {
        Files.createDirectories(directory);
    }

    public static void save(Path file, Object object) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        ensureDirectory(parent);
        Path temp = file.resolveSibling(file.getFileName() + ".tmp");
        Path backup = file.resolveSibling(file.getFileName() + ".bak");

        try (OutputStream bos = new BufferedOutputStream(Files.newOutputStream(temp));
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(object);
            oos.flush();
        }

        if (Files.exists(file)) {
            Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
        }
        try {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicFailure) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** Loads a serialized object, returning empty when missing or corrupted. */
    public static <T> Optional<T> load(Path file, Class<T> type) {
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try (InputStream bis = new BufferedInputStream(Files.newInputStream(file));
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            Object value = ois.readObject();
            return type.isInstance(value) ? Optional.of(type.cast(value)) : Optional.empty();
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            LOG.warning("Could not read data file " + file + ": " + e.getMessage());
            try {
                Path corrupt = file.resolveSibling(file.getFileName() + ".corrupt");
                Files.move(file, corrupt, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException moveFailure) {
                LOG.warning("Could not quarantine corrupted file " + file);
            }
            return Optional.empty();
        }
    }

    public static boolean exists(Path file) {
        return Files.exists(file);
    }
}

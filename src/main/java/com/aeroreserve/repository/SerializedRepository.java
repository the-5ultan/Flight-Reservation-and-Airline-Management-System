package com.aeroreserve.repository;

import com.aeroreserve.util.FileStorageUtil;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Generic serialized repository. All objects are held in memory for fast lookup
 * and written back atomically to a single .ser file after every mutation.
 *
 * @param <T> serializable entity type
 */
public class SerializedRepository<T extends Serializable> {

    private final Path file;
    private final LinkedHashMap<String, T> items = new LinkedHashMap<>();

    public SerializedRepository(Path file) {
        this.file = file;
        load();
    }

    @SuppressWarnings("unchecked")
    private void load() {
        Optional<Map> loaded = FileStorageUtil.load(file, Map.class);
        if (loaded.isPresent()) {
            for (Object rawEntry : loaded.get().entrySet()) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) rawEntry;
                if (entry.getKey() instanceof String key && entry.getValue() != null) {
                    try {
                        items.put(key, (T) entry.getValue());
                    } catch (ClassCastException e) {
                        throw new IllegalStateException("Corrupted repository entry in " + file, e);
                    }
                }
            }
        }
    }

    public synchronized void put(String id, T entity) {
        items.put(id, entity);
        flush();
    }

    public synchronized void putAll(Map<String, T> entities) {
        items.putAll(entities);
        flush();
    }

    public synchronized boolean remove(String id) {
        boolean removed = items.remove(id) != null;
        if (removed) {
            flush();
        }
        return removed;
    }

    public synchronized Optional<T> findById(String id) {
        return Optional.ofNullable(items.get(id));
    }

    public synchronized boolean exists(String id) {
        return items.containsKey(id);
    }

    public synchronized List<T> findAll() {
        return new ArrayList<>(items.values());
    }

    public synchronized int size() {
        return items.size();
    }

    public synchronized void flush() {
        try {
            FileStorageUtil.save(file, new LinkedHashMap<>(items));
        } catch (IOException e) {
            throw new IllegalStateException("Could not persist repository: " + file, e);
        }
    }

    public Path getFile() {
        return file;
    }
}

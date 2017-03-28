package com.gdx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.util.Arrays.stream;

/**
 * Features:
 *  - Bucket Strategy: Jagged Array (https://en.wikipedia.org/wiki/Jagged_array)
 *  - Collision Strategy: Separate Chaining with dynamic array
 *  - Resize Strategy: Full copy
 *  - Hash Function: Java.hashcode()
 */
public final class FullCopyResizeMapWithDynamicArrayChaining<K, V> implements Map<K, V>
{
    private static final double MAX_LOAD_FACTOR = 0.75d;
    private static final int INITIAL_SIZE = 16;
    private static final int INITIAL_CHAIN_SIZE = 4;

    private EntryImpl<K, V>[][] table;
    private int size;

    FullCopyResizeMapWithDynamicArrayChaining()
    {
        this(INITIAL_SIZE);
    }

    FullCopyResizeMapWithDynamicArrayChaining(int size)
    {
        this.table = new EntryImpl[size][INITIAL_CHAIN_SIZE];
    }

    public int size()
    {
        return size;
    }

    public boolean isEmpty()
    {
        return size() == 0;
    }

    public boolean containsKey(Object key)
    {
        return get(key) != null;
    }

    public boolean containsValue(Object value)
    {
        for (EntryImpl<K, V>[] entries : table)
        {
            for (EntryImpl<K, V> entry : entries)
            {
                if (entry != null && entry.getValue().equals(value))
                {
                    return true;
                }
            }

        }
        return false;
    }

    public V get(Object key)
    {
        EntryImpl<K, V> entry = search(table[hash(key, table.length)], (K) key);
        return entry != null ? entry.getValue() : null;
    }

    public V put(K key, V value)
    {
        maybeResize();
        return putImpl(key, value, table);
    }

    public V remove(Object key)
    {

        // Check bucket
        EntryImpl<K, V>[] entries = table[hash(key, table.length)];
        for (int i = 0; i < entries.length; i++)
        {
            EntryImpl<K, V> entry = entries[i];
            if (entry != null && entry.getKey().equals(key))
            {
                size--;
                entries[i] = null;
                return entry.getValue();
            }
        }

        // No match
        return null;
    }

    public void putAll(Map<? extends K, ? extends V> map)
    {
        for (Entry<? extends K, ? extends V> entry : map.entrySet())
        {
            put(entry.getKey(), entry.getValue());
        }
    }

    public void clear()
    {
        size = 0;
        table = new EntryImpl[table.length][INITIAL_CHAIN_SIZE];
    }

    public Set<K> keySet()
    {
        Set<K> keys = new HashSet<>(size());
        for (EntryImpl<K, V>[] entries : table)
        {
            for (EntryImpl<K, V> entry : entries)
            {
                if (entry != null)
                {
                    keys.add(entry.getKey());
                }
            }

        }
        return keys;
    }

    public Collection<V> values()
    {
        List<V> values = new ArrayList<>(size());
        for (EntryImpl<K, V>[] entries : table)
        {
            for (EntryImpl<K, V> entry : entries)
            {
                if (entry != null)
                {
                    values.add(entry.getValue());
                }
            }

        }
        return values;
    }

    public Set<Entry<K, V>> entrySet()
    {
        return null;
    }

    private int hash(Object key, int size)
    {
        return Math.abs(key.hashCode() % size);
    }

    private static <K, V> EntryImpl<K, V> search(EntryImpl<K, V>[] entries, K key)
    {
        for (EntryImpl<K, V> entry : entries)
        {
            if(entry != null && entry.getKey().equals(key))
            {
                return entry;
            }
        }
        return null;
    }

    private V putImpl(K key, V value, EntryImpl<K, V>[][] table)
    {
        int i = hash(key, table.length);
        EntryImpl<K, V>[] entries = table[i];

        // Walk buckets to search existing match
        int pos = 0;
        for (; pos < entries.length; pos++)
        {
            EntryImpl<K, V> entry = entries[pos];
            if (entry == null)
            {
                break;
            }

            if (entry.getKey().equals(key))
            {
                // Overwrite
                return entry.setValue(value);
            }
        }

        // No space - need to expand chain
        if (pos == entries.length)
        {
            // Copy
            EntryImpl<K, V>[] newEntries = new EntryImpl[entries.length * 2];
            System.arraycopy(entries, 0, newEntries, 0, entries.length);

            // Update
            table[i] = newEntries;
            entries = newEntries;
        }

        // Add new entry
        entries[pos] = new EntryImpl<K, V>(key, value);
        size++;
        return null;
    }

    private void maybeResize()
    {
        // Load Factor = n/k (n = entries, k = buckets)
        if (((size() + 1) / (double) table.length) >= MAX_LOAD_FACTOR)
        {
//            debug();
            // Create new table & store current size
            int currentSize = size;
            EntryImpl<K, V>[][] newTable = new EntryImpl[table.length * 2][INITIAL_CHAIN_SIZE];

            // Copy all items across
            for (EntryImpl<K, V>[] entries : table)
            {
                for(EntryImpl<K, V> entry : entries)
                {
                    if (entry != null)
                    {
                        putImpl(entry.getKey(), entry.getValue(), newTable);
                    }
                }
            }

            // Set new table & correct size
            this.table = newTable;
            this.size = currentSize;
        }
    }

    private void debug()
    {
        int emptyBuckets = stream(table).mapToInt(entry -> stream(entry).allMatch(Objects::isNull) ? 1 : 0).sum();
        int maxChainDepth = stream(table).mapToInt(entry -> stream(entry).mapToInt(e -> e != null ? 1 : 0).sum()).max().orElse(-1);

        System.out.printf("Info: #elements: %-6d | #buckets: %-6s | #empty buckets: %-6s | #max chain depth: %s%n",
                size, table.length, emptyBuckets, maxChainDepth);
    }

    private static final class EntryImpl<K, V> implements Entry<K, V>
    {
        private final K key;
        private V value;

        EntryImpl(K key, V value)
        {
            this.key = key;
            this.value = value;
        }

        public K getKey()
        {
            return key;
        }

        public V getValue()
        {
            return value;
        }

        public V setValue(V value)
        {
            return value;
        }
    }
}

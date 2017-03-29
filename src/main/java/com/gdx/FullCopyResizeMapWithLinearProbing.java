package com.gdx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Features:
 *  - Bucket Strategy: Array
 *  - Collision Strategy: Open Addressing using linear probing
 *  - Resize Strategy: Full copy
 *  - Hash Function: Java.hashcode()
 */
public final class FullCopyResizeMapWithLinearProbing<K, V> implements Map<K, V>
{
    private static final double MAX_LOAD_FACTOR = 0.75d;
    private static final int INITIAL_SIZE = 16;

    private EntryImpl<K, V>[] table;
    private int size;

    FullCopyResizeMapWithLinearProbing()
    {
        this(INITIAL_SIZE);
    }

    FullCopyResizeMapWithLinearProbing(int size)
    {
        this.table = new EntryImpl[size];
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
        for (EntryImpl<K, V> entry : table)
        {
            if (entry != null && entry.getValue().equals(value))
            {
                return true;
            }
        }
        return false;
    }

    public V get(Object key)
    {
        int i = hash(key, table.length);
        EntryImpl<K, V> entry = table[i];
        for (; entry != null; i = (i + 1) % table.length, entry = table[i])
        {
            if (entry.getKey().equals(key))
            {
                return entry.getValue();
            }
        }
        return null;
    }

    public V put(K key, V value)
    {
        maybeResize();
        return putImpl(new EntryImpl<K, V>(key, value), table);
    }

    public V remove(Object key)
    {
        int i = hash(key, table.length);
        EntryImpl<K, V> entry = table[i];
        for(; entry != null; i = (i + 1) % table.length, entry = table[i])
        {
            if(entry.getKey().equals(key))
            {
                // Remove from table
                table[i] = null;
                size--;

                // Now move forward checking each key hash for a position equal to or less than the item we are removing.
                // We must do this to preserve linear probes for values whose hash is earlier than i but are stored in
                // a position later than i.
                int j = (i + 1) % table.length;
                EntryImpl<K, V> next = table[j];
                for (; next != null; j = (j + 1) % table.length, next = table[j])
                {
                    if (hash(next.getKey(), table.length) <= i)
                    {
                        table[i] = next;
                        table[j] = null;
                        i = j;
                    }
                }

                // We hit an already empty cell. No more moves to consider.
                break;
            }
        }

        return entry != null ? entry.getValue() : null;
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
        table = new EntryImpl[table.length];
    }

    public Set<K> keySet()
    {
        Set<K> keys = new HashSet<>(size());
        for (EntryImpl<K, V> entry : table)
        {
            if (entry != null)
            {
                keys.add(entry.getKey());
            }
        }
        return keys;
    }

    public Collection<V> values()
    {
        List<V> values = new ArrayList<>(size());
        for (EntryImpl<K, V> entry : table)
        {
            if (entry != null)
            {
                values.add(entry.getValue());
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

    private V putImpl(EntryImpl<K, V> entry, EntryImpl<K, V>[] table)
    {
        int i = hash(entry.getKey(), table.length);
        int init = i;

        // Add entry
        if (table[i] == null)
        {
            size++;
            table[i] = entry;
            return null;
        }

        // Walk forward trying to find matching entry or empty slot
        for (; table[i] != null; i = (i + 1) % table.length)
        {
            // Check for existing mapping
            if (table[i].getKey().equals(entry.getKey()))
            {
                EntryImpl<K, V> existing = table[i];
                table[i] = entry;
                return existing.getValue();
            }
        }

        // Found empty slot
        size++;
        table[i] = entry;
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
            EntryImpl<K, V>[] newTable = new EntryImpl[table.length * 2];

            // Copy all items across
            for (int i = 0; i < table.length; i++)
            {
                if (table[i] != null)
                {
                    putImpl(table[i], newTable);
                }
            }

            // Set new table & correct size
            this.table = newTable;
            this.size = currentSize;
        }
    }

    private void debug()
    {
        int emptyBuckets = Arrays.stream(table).mapToInt(entry -> entry == null ? 0 : 1).sum();

        System.out.printf("Info: #elements: %-6d | #buckets: %-6s | #empty buckets: %s%n",
                size, table.length, emptyBuckets);
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

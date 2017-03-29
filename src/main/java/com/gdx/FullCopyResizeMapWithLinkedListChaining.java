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
 *  - Collision Strategy: Separate Chaining with LinkedList
 *  - Resize Strategy: Full copy
 *  - Hash Function: Java.hashcode()
 */
public final class FullCopyResizeMapWithLinkedListChaining<K, V> implements Map<K, V>
{
    private static final double MAX_LOAD_FACTOR = 0.75d;
    private static final int INITIAL_SIZE = 16;

    private EntryImpl<K, V>[] table;
    private int size;

    FullCopyResizeMapWithLinkedListChaining()
    {
        this(INITIAL_SIZE);
    }

    FullCopyResizeMapWithLinkedListChaining(int size)
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
            for (; entry != null; entry = entry.next)
            {
                if (entry.getValue().equals(value))
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
        return putImpl(new EntryImpl<K, V>(key, value), table);
    }

    public V remove(Object key)
    {
        int i = hash(key, table.length);

        // Check bucket
        for (EntryImpl<K, V> prev = null, entry = table[i]; entry != null; prev = entry, entry = entry.next)
        {
            if(entry.getKey().equals(key))
            {
                size--;
                if (prev == null)
                {
                    table[i] = entry.next;
                    return entry.getValue();
                }
                else
                {
                    prev.setNext(entry.next);
                    return entry.getValue();
                }
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
        table = new EntryImpl[table.length];
    }

    public Set<K> keySet()
    {
        Set<K> keys = new HashSet<>(size());
        for (EntryImpl<K, V> entry : table)
        {
            for (; entry != null; entry = entry.next)
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
            for (; entry != null; entry = entry.next)
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

    private static <K, V> EntryImpl<K, V> search(EntryImpl<K, V> entry, K key)
    {
        for (; entry != null; entry = entry.next)
        {
            if(entry.getKey().equals(key))
            {
                return entry;
            }
        }
        return null;
    }

    private V putImpl(EntryImpl<K, V> entry, EntryImpl<K, V>[] table)
    {
        int i = hash(entry.getKey(), table.length);
        EntryImpl<K, V> cur = table[i];

        // Create new bucket
        if (cur == null)
        {
            size++;
            table[i] = entry;
            return null;
        }

        // Walk buckets to search existing match
        EntryImpl<K, V> prev = null;
        for (; cur != null; prev = cur, cur = cur.next)
        {
            if (cur.getKey().equals(entry.getKey()))
            {
                // Overwrite
                return cur.setValue(entry.getValue());
            }
        }

        // No match - append to end
        size++;
        prev.setNext(entry);
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
                for (EntryImpl<K, V> entry = table[i]; entry != null;)
                {
                    // Record next in chain
                    EntryImpl<K, V> next = entry.next;

                    // Move
                    entry.setNext(null);
                    putImpl(entry, newTable);

                    // Move to next element in chain
                    entry = next;
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
        int maxChainDepth = Arrays.stream(table).mapToInt(FullCopyResizeMapWithLinkedListChaining::length).max().orElse(-1);

        System.out.printf("Info: #elements: %-6d | #buckets: %-6s | #empty buckets: %-6s | #max chain depth: %s%n",
                size, table.length, emptyBuckets, maxChainDepth);
    }

    private static <K, V> int length(EntryImpl<K, V> entry)
    {
        int i = 0;
        for (; entry != null; entry = entry.next)
        {
            i++;
        }
        return i;
    }

    private static final class EntryImpl<K, V> implements Entry<K, V>
    {
        private final K key;
        private V value;

        private EntryImpl<K, V> next;

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

        void setNext(EntryImpl<K, V> next)
        {
            this.next = next;
        }
    }


}

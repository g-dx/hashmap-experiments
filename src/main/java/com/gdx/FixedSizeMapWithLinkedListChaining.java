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
 *  - Resize Strategy: None
 *  - Hash Function: Java.hashcode()
 */
public final class FixedSizeMapWithLinkedListChaining<K, V> implements Map<K, V>
{
    private static final int INITIAL_SIZE = 16;

    private EntryImpl<K, V>[] table;

    FixedSizeMapWithLinkedListChaining()
    {
        this.table = new EntryImpl[INITIAL_SIZE];
    }

    public int size()
    {
        return Arrays.stream(table).map(FixedSizeMapWithLinkedListChaining::length).reduce((acc, size) -> acc += size).orElseGet(() -> 0);
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
        EntryImpl<K, V> entry = search(table[hash(key)], (K) key);
        return entry != null ? entry.getValue() : null;
    }

    public V put(K key, V value)
    {
        int i = hash(key);
        EntryImpl<K, V> entry = table[i];

        // Create new bucket
        if (entry == null)
        {
            table[i] = new EntryImpl<K, V>(key, value);
            return null;
        }

        // Walk buckets to search existing match
        EntryImpl<K, V> prev = null;
        for (; entry != null; prev = entry, entry = entry.next)
        {
            if (entry.getKey().equals(key))
            {
                // Overwrite
                return entry.setValue(value);
            }
        }

        // No match - append to end
        prev.setNext(new EntryImpl<K, V>(key, value));
        return null;
    }

    public V remove(Object key)
    {
        int i = hash(key);

        // Check bucket
        for (EntryImpl<K, V> prev = null, entry = table[i]; entry != null; prev = entry, entry = entry.next)
        {
            if(entry.getKey().equals(key))
            {
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
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet())
        {
            put(entry.getKey(), entry.getValue());
        }
    }

    public void clear()
    {
        this.table = new EntryImpl[INITIAL_SIZE];
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

    public Set<Map.Entry<K, V>> entrySet()
    {
        return null;
    }

    private int hash(Object key)
    {
        return Math.abs(key.hashCode() % table.length);
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

    private static <K, V> int length(EntryImpl<K, V> entry)
    {
        int i = 0;
        for (; entry != null; entry = entry.next)
        {
            i++;
        }
        return i;
    }

    private static final class EntryImpl<K, V> implements Map.Entry<K, V>
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

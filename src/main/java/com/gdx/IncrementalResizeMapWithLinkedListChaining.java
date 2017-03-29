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
 *  - Resize Strategy: Incremental
 *  - Hash Function: Java.hashcode()
 */
public final class IncrementalResizeMapWithLinkedListChaining<K, V> implements Map<K, V>
{
    private static final double MAX_LOAD_FACTOR = 0.75d;
    private static final EntryImpl[] EMPTY = new EntryImpl[1];

    private static final int INITIAL_SIZE = 16;
    private static final int BATCH_MOVE_SIZE = 16;

    private EntryImpl<K, V>[] curTable, oldTable;
    private int curSize, oldSize;

    IncrementalResizeMapWithLinkedListChaining()
    {
        this(INITIAL_SIZE);
    }

    IncrementalResizeMapWithLinkedListChaining(int size)
    {
        this.curTable = new EntryImpl[size];
        this.oldTable = EMPTY;
    }

    public int size()
    {
        return curSize + oldSize;
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
        return containsValueImpl(value, curTable) || containsValueImpl(value, oldTable);
    }

    public V get(Object key)
    {
        // Check current
        EntryImpl<K, V> entry = search(curTable[hash(key, curTable.length)], (K) key);
        if (entry != null)
        {
            return entry.getValue();
        }
        // Check old
        entry = entry = search(oldTable[hash(key, oldTable.length)], (K) key);
        return entry != null ?  entry.getValue(): null;
    }

    public V put(K key, V value)
    {
        maybeResize();
        V oldValue = putImpl(key, value);
        maybeMoveOldEntries();
        return oldValue;
    }

    public V remove(Object key)
    {
        V value = removeImpl(key, curTable);
        if (value != null)
        {
            curSize--;
            return value;
        }
        value = removeImpl(key, oldTable);
        if (value != null)
        {
            oldSize--;
            // TODO: Check for zero and clear array...
        }
        return value;
    }

    private V removeImpl(Object key, EntryImpl<K, V>[] table)
    {
        int i = hash(key, table.length);

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
        for (Entry<? extends K, ? extends V> entry : map.entrySet())
        {
            put(entry.getKey(), entry.getValue());
        }
    }

    public void clear()
    {
        curSize = 0;
        curTable = new EntryImpl[INITIAL_SIZE];
        oldTable = EMPTY;
        oldSize = 0;
    }

    public Set<K> keySet()
    {
        Set<K> keys = new HashSet<>(size());
        keySetImpl(keys, curTable);
        keySetImpl(keys, oldTable);
        return keys;
    }

    public Collection<V> values()
    {
        List<V> values = new ArrayList<>(size());
        valuesImpl(values, curTable);
        valuesImpl(values, oldTable);
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

    private static <K, V> void keySetImpl(Set<K> keys, EntryImpl<K, V>[] table)
    {
        for (EntryImpl<K, V> entry : table)
        {
            for (; entry != null; entry = entry.next)
            {
                keys.add(entry.getKey());
            }
        }
    }

    private static <K, V> void valuesImpl(List<V> values, EntryImpl<K, V>[] table)
    {
        for (EntryImpl<K, V> entry : table)
        {
            for (; entry != null; entry = entry.next)
            {
                values.add(entry.getValue());
            }
        }
    }

    private static <K, V> boolean containsValueImpl(Object value, EntryImpl<K, V>[] table)
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

    private void maybeMoveOldEntries()
    {
        // Already moved old table -> current table
        if (oldSize == 0)
        {
            return;
        }

        for (int i = 0, moved = 0; i < oldTable.length && moved <= BATCH_MOVE_SIZE; i++)
        {
            EntryImpl<K, V> entry = oldTable[i];
            for (; entry != null && moved <= BATCH_MOVE_SIZE; entry = entry.next, moved++, oldSize--)
            {
                // TODO: Consider memory churn here - can we simply copy the reference?
                putImpl(entry.getKey(), entry.getValue());
            }
            oldTable[i] = entry;
        }

        // Old table move is complete
        if (oldSize == 0)
        {
            oldTable = EMPTY;
        }
    }

    private V putImpl(K key, V value)
    {
        int i = hash(key, curTable.length);
        EntryImpl<K, V> entry = curTable[i];

        // Create new bucket
        if (entry == null)
        {
            curSize++;
            curTable[i] = new EntryImpl<K, V>(key, value);
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
        curSize++;
        prev.setNext(new EntryImpl<K, V>(key, value));
        return null;
    }

    private void maybeResize()
    {
        // Load Factor = n/k (n = entries, k = buckets)
        if (((size() + 1) / (double) curTable.length) >= MAX_LOAD_FACTOR)
        {
//            debug();

            // Set tables & correct size
            this.oldTable = this.curTable;
            this.oldSize = this.curSize;
            this.curTable = new EntryImpl[curTable.length * 2];
            this.curSize = 0;
        }
    }

    private void debug()
    {
        int emptyBuckets = Arrays.stream(curTable).mapToInt(entry -> entry == null ? 0 : 1).sum();
        int maxChainDepth = Arrays.stream(curTable).mapToInt(IncrementalResizeMapWithLinkedListChaining::length).max().orElse(-1);

        System.out.printf("Info: #elements: %-6d | #buckets: %-6s | #empty buckets: %-6s | #max chain depth: %s%n",
                size(), curTable.length, emptyBuckets, maxChainDepth);
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

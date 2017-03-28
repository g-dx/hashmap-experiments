package com.gdx;

import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public final class HashMapTest
{
    private static String[] DICTIONARY = Utils.load("/words.txt");

    @Test
    public void testFixedSizeMapWithLinkedListChaining()
    {
        assertMainOperations(new FixedSizeMapWithLinkedListChaining<>());
    }

    private void assertMainOperations(Map<String, Integer> map)
    {
        // Put
        for (int i = 0; i < DICTIONARY.length; i++)
        {
            Integer prev = map.put(DICTIONARY[i], i);
            assertEquals(format("Expected: no existing mapping for key = %s, Actual: previous value = %s", DICTIONARY[i], prev), null, prev);
        }

        // Arbitrary values to check
        for (Integer i : asList(1, 4, 45, 78, 431, 1023, 12333, 16314, 19022, 34978, 39001, 45401))
        {
            // ContainsKey
            if (!map.containsKey(DICTIONARY[i]))
            {
                fail(format("Expected: contains key = %s, Actual: <none>", DICTIONARY[i]));
            }

            // ContainsValue
            if (!map.containsValue(i))
            {
                fail(format("Expected: contains value = %s, Actual: <none>", i));
            }
        }

        // Size & isEmpty
        assertEquals(format("Expected: map.size() == %s. Actual: map.size() == %s", DICTIONARY.length, map.size()), DICTIONARY.length, map.size());
        assertEquals("Expected: map.isEmpty() == false. Actual: map.isEmpty() == true", false, map.isEmpty());

        // Get & remove
        for (int expected = 0; expected < DICTIONARY.length; expected++)
        {
            // Get
            String key = DICTIONARY[expected];
            Integer actual = map.get(key);
            if (!actual.equals(expected))
            {
                fail(format("Expected: map.get(%s) == %s, Actual: %s", key, expected, actual));
            }

            // Remove
            actual = map.remove(key);
            if (!actual.equals(expected))
            {
                fail(format("Expected: map.remove(%s) == %s, Actual: %s", key, expected, actual));
            }

            // Get null entry
            actual = map.get(key);
            if (actual != null)
            {
                fail(format("Expected: map.get(%s) == null, Actual: %s", key, actual));
            }
        }

        // Size & isEmpty
        assertEquals(format("Expected: map.size() == 0. Actual: map.size() == %s", map.size()), 0, map.size());
        assertEquals("Expected: map.isEmpty() == true. Actual: map.isEmpty() == false", true, map.isEmpty());
    }
}
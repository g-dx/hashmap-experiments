package com.gdx;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

final class Utils
{
    private Utils() { /* ... */ }

    static String[] load(String resource)
    {
        try
        {
            File file = new File(HashMapTest.class.getResource(resource).toURI());
            String[] lines = new String[countLines(file)];

            try(BufferedReader reader = new BufferedReader(new FileReader(file)))
            {
                int i = 0;
                String line = "";
                while ((line = reader.readLine()) != null)
                {
                    lines[i] = line;
                    i++;
                }
            }
            return lines;
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Failed to load resource: " + resource);
        }
    }

    private static int countLines(File f) throws IOException
    {
        int i = 0;
        for(BufferedReader r = new BufferedReader(new FileReader(f)); r.readLine() != null; i++) { /* ... */ }
        return i;
    }
}

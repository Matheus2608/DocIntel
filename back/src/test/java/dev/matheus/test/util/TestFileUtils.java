package dev.matheus.test.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class for loading test files.
 */
public class TestFileUtils {

    private TestFileUtils() {
        // Utility class
    }

    /**
     * Reads a test file from the test resources folder.
     */
    public static byte[] readTestFile(String filename) throws IOException {
        try (InputStream inputStream = TestFileUtils.class.getClassLoader()
                .getResourceAsStream("files/" + filename)) {
            if (inputStream == null) {
                throw new IOException("Test file not found: " + filename);
            }
            return inputStream.readAllBytes();
        }
    }
}


package org.example.util;

import org.example.server.config.ThreadConfigContext;

import java.io.File;
import java.io.IOException;

public class FileUtils {

    public static File find(File directory, String fileName) {
        return new File(directory, fileName);
    }


}

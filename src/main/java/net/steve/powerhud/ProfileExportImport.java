package net.steve.powerhud;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class ProfileExportImport {
    public static boolean exportProfile(String profileName, Path exportPath) {
        String sanitized = PowerHudConfig.sanitizeProfileName(profileName);
        Path profileFile = PowerHudConfig.PROFILES_DIR.resolve(sanitized + ".json");
        if (!profileFile.toFile().exists()) return false;
        try {
            Files.copy(profileFile, exportPath);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean importProfile(Path importPath) {
        try {
            String fileName = importPath.getFileName().toString();
            if (!fileName.endsWith(".json")) return false;
            Path dest = PowerHudConfig.PROFILES_DIR.resolve(fileName);
            Files.copy(importPath, dest);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}

package server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads config.properties (DB credentials, API keys) safely for BOTH the
 * IDE-run case and the packaged-JAR case, without ever needing the secret
 * baked into a build artifact:
 *
 *  1. First looks for a config.properties file sitting NEXT TO wherever the
 *     app is actually running from (the current working directory) - this
 *     is the file you place beside Server.jar/Client.jar when running the
 *     packaged JAR. It is NEVER bundled into the JAR itself.
 *  2. Falls back to the classpath resource (src/main/resources/config.properties)
 *     for convenience when running directly from the IDE during development.
 *
 * This is the fix for the JAR/GitHub API-key leak: previously both
 * DatabaseManager and BotApiClient only checked the classpath resource,
 * which meant Maven would happily bundle whatever config.properties
 * existed locally straight into the packaged JAR - including real secrets,
 * if the JAR was built on a machine that had them. With this loader, the
 * JAR never needs to contain the file at all.
 */
public final class ConfigLoader {

    private ConfigLoader() {
    }

    public static Properties load() {
        Properties props = new Properties();

        File externalFile = new File("config.properties");
        if (externalFile.exists()) {
            try (InputStream in = new FileInputStream(externalFile)) {
                props.load(in);
                System.out.println("[CONFIG] Loaded config.properties from: " + externalFile.getAbsolutePath());
                return props;
            } catch (IOException e) {
                System.err.println("[CONFIG] Found external config.properties but couldn't read it: " + e.getMessage());
            }
        }

        try (InputStream in = ConfigLoader.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                props.load(in);
                System.out.println("[CONFIG] Loaded config.properties from the classpath (IDE/dev mode).");
                return props;
            }
        } catch (IOException e) {
            System.err.println("[CONFIG] Failed to read classpath config.properties: " + e.getMessage());
        }

        System.err.println("[CONFIG] No config.properties found (checked external file and classpath).");
        return props;
    }
}
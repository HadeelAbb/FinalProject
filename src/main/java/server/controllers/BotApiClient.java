package server.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Properties;

/**
 * SUC-14: Study Bot - sends a student's question to a real external AI API
 * (Groq, free tier - broadly available, OpenAI-compatible format) and
 * returns the answer, per the spec's requirement to use an existing bot/API
 * rather than build one. Reads the key from config.properties (same
 * gitignored file used for the DB password) under "groq.api.key".
 */
public class BotApiClient {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final String apiKey;

    public BotApiClient() {
        this.apiKey = loadApiKey();
    }

    private String loadApiKey() {
        Properties props = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (in != null) {
                props.load(in);
                String key = props.getProperty("groq.api.key");
                if (key != null && !key.isBlank()) {
                    return key.trim();
                }
            }
        } catch (IOException e) {
            System.err.println("[BOT] Could not read config.properties: " + e.getMessage());
        }
        System.err.println("[BOT] No groq.api.key found in config.properties - bot will use fallback answers.");
        return null;
    }

    /**
     * Sends the question to the real API and returns the answer, or null if
     * the key is missing, the call fails, or no suitable answer came back -
     * callers should show the spec's "no suitable answer" message in that case.
     */
    public String ask(String studentQuestion, String courseContext) {
        if (apiKey == null) {
            return null;
        }
        try {
            String systemPrompt = "You are a study assistant helping a high school student with their "
                    + "coursework" + (courseContext != null ? " for course " + courseContext : "")
                    + ". Answer clearly and concisely, in a way appropriate for a student. "
                    + "If the question is unrelated to schoolwork, politely redirect them to ask something study-related.";

            String body = "{"
                    + "\"model\":\"" + MODEL + "\","
                    + "\"messages\":["
                    + "{\"role\":\"system\",\"content\":" + jsonString(systemPrompt) + "},"
                    + "{\"role\":\"user\",\"content\":" + jsonString(studentQuestion) + "}"
                    + "],"
                    + "\"max_tokens\":500"
                    + "}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(20))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.err.println("[BOT] API returned status " + response.statusCode() + ": " + response.body());
                return null;
            }

            return extractContent(response.body());
        } catch (Exception e) {
            System.err.println(
                    "[BOT] API call failed: "
                            + e.getClass().getName()
                            + ": "
                            + e.getMessage()
            );
            e.printStackTrace();
            return null;
        }
    }

    /** Minimal hand-rolled JSON string escaping - avoids pulling in a JSON library for one field. */
    private String jsonString(String text) {
        StringBuilder sb = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.append("\"").toString();
    }

    /** Minimal hand-rolled extraction of choices[0].message.content - avoids a JSON library for one field. */
    private String extractContent(String responseBody) {
        try {
            int contentKey = responseBody.indexOf("\"content\"");
            if (contentKey == -1) return null;
            int colon = responseBody.indexOf(':', contentKey);
            int firstQuote = responseBody.indexOf('"', colon + 1);
            StringBuilder result = new StringBuilder();
            int i = firstQuote + 1;
            while (i < responseBody.length() && responseBody.charAt(i) != '"') {
                char c = responseBody.charAt(i);
                if (c == '\\' && i + 1 < responseBody.length()) {
                    char next = responseBody.charAt(i + 1);
                    switch (next) {
                        case 'n' -> result.append('\n');
                        case 't' -> result.append('\t');
                        case '"' -> result.append('"');
                        case '\\' -> result.append('\\');
                        default -> result.append(next);
                    }
                    i += 2;
                } else {
                    result.append(c);
                    i++;
                }
            }
            String content = result.toString().trim();
            return content.isEmpty() ? null : content;
        } catch (Exception e) {
            System.err.println("[BOT] Could not parse API response: " + e.getMessage());
            return null;
        }
    }
}
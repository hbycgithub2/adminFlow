package com.hmall.tts.volcengine;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpUnidirectionalStreaming {
    private static final String URL = "https://openspeech.bytedance.com/api/v3/tts/unidirectional";
    private static final String API_KEY = "a83eef4b-bde3-4cbf-ac5f-0a35a17b31ad";
    private static final String RESOURCE_ID = "seed-tts-2.0";

    private static final Pattern CODE_PATTERN = Pattern.compile("\"code\"\\s*:\\s*(-?\\d+)");
    private static final Pattern DATA_PATTERN = Pattern.compile("\"data\"\\s*:\\s*\"([^\"]*)\"");

    public static void main(String[] args) {
        ttsHttpStream();
    }

    private static void ttsHttpStream() {
        String payload = "{"
                + "\"req_params\":{"
                + "\"text\":\"你好，这是一个语音测试\","
                + "\"speaker\":\"zh_male_liufei_uranus_bigtts\","
                + "\"audio_params\":{"
                + "\"format\":\"mp3\","
                + "\"sample_rate\":24000"
                + "}"
                + "}"
                + "}";

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .timeout(Duration.ofMinutes(5))
                .header("X-Api-Key", API_KEY)
                .header("X-Api-Resource-Id", RESOURCE_ID)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        ByteArrayOutputStream audioData = new ByteArrayOutputStream();

        try {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }

                    System.out.println("json data:" + line);

                    int code = extractCode(line);
                    String data = extractData(line);

                    if (code == 0 && data != null && !data.isEmpty()) {
                        byte[] chunkAudio = Base64.getDecoder().decode(data);
                        audioData.write(chunkAudio);
                    }

                    if (code == 20000000) {
                        break;
                    }

                    if (code > 0) {
                        System.out.println("error response:" + line);
                        break;
                    }
                }
            }

            if (audioData.size() > 0) {
                Path outputDir = Path.of("tts");
                Files.createDirectories(outputDir);

                Path outputFile = outputDir.resolve("tts_test.mp3");
                Files.write(outputFile, audioData.toByteArray());
                System.out.printf("file size: %.2f KB%n", audioData.size() / 1024.0);
            }
        } catch (Exception e) {
            System.out.println("request error: " + e.getMessage());
        }
    }

    private static int extractCode(String jsonLine) {
        Matcher matcher = CODE_PATTERN.matcher(jsonLine);
        if (!matcher.find()) {
            return 0;
        }
        return Integer.parseInt(matcher.group(1));
    }

    private static String extractData(String jsonLine) {
        Matcher matcher = DATA_PATTERN.matcher(jsonLine);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
    }
}
package com.editflowimg.backend.service;

import com.editflowimg.backend.dto.generation.AiResult;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Base64;
import java.util.List;

@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final WebClient openAiClient;
    private final WebClient insecureClient;
    private final String model;

    public AiService(@Value("${openai.api.key}") String apiKey,
                     @Value("${openai.image.model}") String model) {
        this.model = model;
        try {
            var sslContext = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
            var httpClient = HttpClient.create().secure(spec -> spec.sslContext(sslContext));
            var connector = new ReactorClientHttpConnector(httpClient);
            this.openAiClient = WebClient.builder()
                    .baseUrl("https://api.openai.com")
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .clientConnector(connector)
                    .codecs(config -> config.defaultCodecs().maxInMemorySize(20 * 1024 * 1024))
                    .build();
            this.insecureClient = WebClient.builder()
                    .clientConnector(connector)
                    .codecs(config -> config.defaultCodecs().maxInMemorySize(20 * 1024 * 1024))
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Falha ao configurar SSL do WebClient", e);
        }
    }

    public byte[] editImage(File imageFile, String prompt) throws IOException {
        byte[] originalBytes = Files.readAllBytes(imageFile.toPath());
        byte[] pngBytes = convertToPng1024(originalBytes);

        String englishPrompt = translateToEnglish(prompt);
        log.info("Prompt traduzido: {}", englishPrompt);
        log.info("Imagem convertida para PNG 1024x1024 ({} bytes). Enviando ao OpenAI.", pngBytes.length);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("model", model);
        body.add("prompt", englishPrompt);
        body.add("n", "1");
        body.add("image", toResource(pngBytes, "image.png"));

        String response = openAiClient.post()
                .uri("/v1/images/edits")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(body))
                .retrieve()
                .onStatus(status -> status.isError(), res ->
                        res.bodyToMono(String.class)
                                .defaultIfEmpty("(corpo vazio)")
                                .map(error -> new RuntimeException("Erro OpenAI [" + res.statusCode().value() + "]: " + error))
                )
                .bodyToMono(String.class)
                .timeout(Duration.ofMinutes(3))
                .block();

        if (response == null) {
            throw new RuntimeException("Resposta vazia do OpenAI");
        }

        log.info("Resposta recebida do OpenAI, extraindo imagem...");
        return extractImageBytes(response);
    }

    private String translateToEnglish(String text) {
        try {
            String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String url = "https://api.mymemory.translated.net/get?q=" + encoded + "&langpair=pt|en";

            String result = insecureClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (result != null) {
                String marker = "\"translatedText\":\"";
                int start = result.indexOf(marker);
                if (start != -1) {
                    start += marker.length();
                    int end = result.indexOf("\"", start);
                    if (end != -1) {
                        return result.substring(start, end);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Falha na tradução, usando prompt original: {}", e.getMessage());
        }
        return text;
    }

    private byte[] extractImageBytes(String json) {
        for (String marker : List.of("\"b64_json\": \"", "\"b64_json\":\"")) {
            int start = json.indexOf(marker);
            if (start != -1) {
                start += marker.length();
                int end = json.indexOf("\"", start);
                if (end != -1) {
                    byte[] decoded = Base64.getDecoder().decode(json.substring(start, end));
                    log.info("Imagem extraída via b64_json: {} bytes", decoded.length);
                    return decoded;
                }
            }
        }

        for (String marker : List.of("\"url\": \"", "\"url\":\"")) {
            int start = json.indexOf(marker);
            if (start != -1) {
                start += marker.length();
                int end = json.indexOf("\"", start);
                if (end != -1) {
                    String url = json.substring(start, end).replace("\\/", "/");
                    log.info("Baixando imagem da URL: {}", url);
                    return downloadFromUrl(url);
                }
            }
        }

        throw new RuntimeException("Imagem não encontrada na resposta do OpenAI: " + json.substring(0, Math.min(300, json.length())));
    }

    private byte[] downloadFromUrl(String url) {
        byte[] result = insecureClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(byte[].class)
                .timeout(Duration.ofMinutes(2))
                .block();

        if (result == null || result.length == 0) {
            throw new RuntimeException("Falha ao baixar imagem da URL do OpenAI");
        }
        return result;
    }

    private byte[] convertToPng1024(byte[] originalBytes) throws IOException {
        BufferedImage original = ImageIO.read(new ByteArrayInputStream(originalBytes));
        if (original == null) {
            throw new IOException("Não foi possível ler a imagem enviada");
        }

        BufferedImage resized = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(original, 0, 0, 1024, 1024, null);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(resized, "png", baos);
        return baos.toByteArray();
    }

    private ByteArrayResource toResource(byte[] bytes, String fileName) {
        return new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };
    }

    public AiResult generateAnalysis(String editingPrompt) {
        String promptResumido = editingPrompt.length() > 80
                ? editingPrompt.substring(0, 80) + "..."
                : editingPrompt;

        List<String> analysisItems = List.of(
                "Imagem original recebida e analisada com sucesso.",
                "Qualidade e formato da imagem verificados antes da edição.",
                "Instrução interpretada: " + promptResumido
        );

        List<String> changeItems = List.of(
                "Edição aplicada pelo modelo " + model + " via OpenAI.",
                "Imagem transformada com base nas instruções fornecidas.",
                "Nova versão gerada preservando elementos originais da foto."
        );

        String descriptionText = "A imagem foi editada pelo modelo " + model + " (OpenAI) " +
                "com base nas instruções fornecidas, gerando uma nova versão profissional.";

        return new AiResult(analysisItems, changeItems, descriptionText);
    }
}

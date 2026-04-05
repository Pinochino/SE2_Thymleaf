package com.example.SE2.services.embedding;

import ai.djl.ModelException;
import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory;
import ai.djl.inference.Predictor;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import com.example.SE2.models.Novel;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    private Predictor<String, float[]> predictor;

    @PostConstruct
    public void init() {
        try {
            // Try classpath first, fall back to file system path
            URL resource = getClass().getClassLoader()
                    .getResource("model/all-MiniLM-L6-v2");

            Path modelDir;
            if (resource != null) {
                modelDir = Paths.get(resource.toURI());
            } else {
                modelDir = Paths.get("src/main/resources/model/all-MiniLM-L6-v2")
                        .toAbsolutePath();
            }

            if (!modelDir.toFile().exists()) {
                throw new RuntimeException(
                        "Model directory not found at: " + modelDir +
                                "\nModel should be at src/main/resources/model/all-MiniLM-L6-v2/"
                );
            }

            Criteria<String, float[]> criteria = Criteria.builder()
                    .setTypes(String.class, float[].class)
                    .optModelPath(modelDir)
                    .optModelName("model")
                    .optEngine("OnnxRuntime")
                    .optTranslatorFactory(new TextEmbeddingTranslatorFactory())
                    .optArgument("includeTokenTypes", true)   // ← key fix
                    .optArgument("pooling", "mean")
                    .optArgument("normalize", true)
                    .build();

            ZooModel<String, float[]> model = criteria.loadModel();
            predictor = model.newPredictor();

        } catch (IOException | ModelException | URISyntaxException e) {
            throw new RuntimeException("Failed to load embedding model", e);
        }
    }

    @Override
    public float[] embed(String text) {
        try {
            text = preprocess(text);
            return predictor.predict(text);
        } catch (Exception e) {
            throw new RuntimeException("Embedding failed", e);
        }
    }

    @Override
    public float[] embedNovel(Novel novel) {
        String genres = novel.getGenres().stream()
                .map(g -> g.getName().name())
                .collect(Collectors.joining(", "));

        String text = String.format(
                "Title: %s. Author: %s. Description: %s. Genres: %s.",
                safe(novel.getTitle()),
                safe(novel.getAuthor()),
                safe(novel.getDescription()),
                genres
        );
        return embed(text);
    }

    private String preprocess(String text) {
        if (text == null) return "";
        return text.toLowerCase().trim().replaceAll("\\s+", " ");
    }

    private String safe(String s) { return s == null ? "" : s; }

}

package com.example.SE2.services.embedding;

import com.example.SE2.models.Novel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class EmbeddingServiceImpl implements EmbeddingService {
    private final OllamaEmbeddingModel embeddingModel;

    public EmbeddingServiceImpl(OllamaEmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public Float[] embed(String text) {
        float[] raw = embeddingModel.embed(text);
        Float[] result = new Float[raw.length];
        for (int i = 0; i < raw.length; i++) result[i] = raw[i];
        return result;
    }

    public Float[] embedNovel(Novel novel) {
        String genres = novel.getGenres().stream()
                .map(genre -> genre.getName().name())
                .collect(Collectors.joining(", "));

        String text = novel.getTitle() + " " +
                novel.getAuthor() + " " +
                novel.getDescription() + " " +
                genres;

        return embed(text);
    }

}

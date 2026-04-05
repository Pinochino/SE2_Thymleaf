package com.example.SE2.services.embedding;

import com.example.SE2.models.Novel;

public interface EmbeddingService{

    float[] embed(String text);

    float[] embedNovel(Novel novel);
}

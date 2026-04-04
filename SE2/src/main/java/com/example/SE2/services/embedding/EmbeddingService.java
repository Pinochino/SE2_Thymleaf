package com.example.SE2.services.embedding;

import com.example.SE2.models.Novel;

public interface EmbeddingService{

    Float[] embed(String text);

    Float[] embedNovel(Novel novel);
}

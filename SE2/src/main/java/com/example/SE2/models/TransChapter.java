package com.example.SE2.models;

import jakarta.persistence.*;

@Entity
@Table(name = "trans_chapter")
public class TransChapter extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "novel_id", nullable = false)
    private Novel novel;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    private Integer paragraphs;

    public TransChapter() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Novel getNovel() {
        return novel;
    }

    public void setNovel(Novel novel) {
        this.novel = novel;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getParagraphs() {
        return paragraphs;
    }

    public void setParagraphs(Integer paragraphs) {
        this.paragraphs = paragraphs;
    }
}

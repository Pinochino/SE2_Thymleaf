package com.example.SE2.models;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
public class ParagraphComment extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    private Integer paragraphIndex;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private ParagraphComment parentComment;

    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL)
    private List<ParagraphComment> replies = new ArrayList<>();

    public ParagraphComment() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Chapter getChapter() {
        return chapter;
    }

    public void setChapter(Chapter chapter) {
        this.chapter = chapter;
    }

    public Integer getParagraphIndex() {
        return paragraphIndex;
    }

    public void setParagraphIndex(Integer paragraphIndex) {
        this.paragraphIndex = paragraphIndex;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public ParagraphComment getParentComment() {
        return parentComment;
    }

    public void setParentComment(ParagraphComment parentComment) {
        this.parentComment = parentComment;
    }

    public List<ParagraphComment> getReplies() {
        return replies;
    }

    public void setReplies(List<ParagraphComment> replies) {
        this.replies = replies;
    }
}

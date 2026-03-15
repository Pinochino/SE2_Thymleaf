package com.example.SE2.models;

import com.example.SE2.constants.FontFamily;
import com.example.SE2.constants.FontSize;
import com.example.SE2.constants.LineSpacing;
import com.example.SE2.constants.Theme;
import jakarta.persistence.*;

@Entity
public class ReadingSetting extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    private FontSize fontSize;

    @Enumerated(EnumType.STRING)
    private FontFamily fontFamily;

    @Enumerated(EnumType.STRING)
    private Theme theme;

    @Enumerated(EnumType.STRING)
    private LineSpacing lineSpacing;

    public ReadingSetting() {
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

    public FontSize getFontSize() {
        return fontSize;
    }

    public void setFontSize(FontSize fontSize) {
        this.fontSize = fontSize;
    }

    public FontFamily getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(FontFamily fontFamily) {
        this.fontFamily = fontFamily;
    }

    public Theme getTheme() {
        return theme;
    }

    public void setTheme(Theme theme) {
        this.theme = theme;
    }

    public LineSpacing getLineSpacing() {
        return lineSpacing;
    }

    public void setLineSpacing(LineSpacing lineSpacing) {
        this.lineSpacing = lineSpacing;
    }
}

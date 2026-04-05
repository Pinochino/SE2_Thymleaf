package com.example.SE2.dtos.request;

public class ProgressRequest {
    private Long paragraphIndex;  // ← was "lastPosition", renamed for clarity
    private Long lastPosition;       // keep for backward-compat if needed

    public Long getParagraphIndex() { return paragraphIndex; }
    public void setParagraphIndex(Long paragraphIndex) { this.paragraphIndex = paragraphIndex; }

    public Long getLastPosition() {
        // Support both field names from JS
        return lastPosition != null ? lastPosition
                : (paragraphIndex != null ? paragraphIndex.longValue() : 0L);
    }
    public void setLastPosition(Long lastPosition) { this.lastPosition = lastPosition; }
}

package com.example.SE2.services.notification;

import com.example.SE2.models.*;
import com.example.SE2.repositories.BookmarkRepository;
import com.example.SE2.repositories.NotificationRepository;
import com.example.SE2.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Notify the parent comment author that someone replied to their novel comment.
     */
    public void notifyNovelCommentReply(NovelComment reply, NovelComment parentComment) {
        User parentAuthor = parentComment.getUser();
        User replier = reply.getUser();

        // Don't notify yourself
        if (parentAuthor.getId().equals(replier.getId())) return;

        String novelTitle = reply.getNovel().getTitle();
        String replierName = formatUserName(replier);
        String content = replierName + " replied to your comment on \"" + novelTitle + "\"";

        createNotification(parentAuthor, content);
    }

    /**
     * Notify the parent comment author that someone replied to their paragraph comment.
     */
    public void notifyParagraphCommentReply(ParagraphComment reply, ParagraphComment parentComment) {
        User parentAuthor = parentComment.getUser();
        User replier = reply.getUser();

        if (parentAuthor.getId().equals(replier.getId())) return;

        String chapterTitle = reply.getChapter().getTitle();
        String replierName = formatUserName(replier);
        String content = replierName + " replied to your comment on \"" + chapterTitle + "\"";

        createNotification(parentAuthor, content);
    }

    /**
     * Notify users who bookmarked this paragraph that someone commented on it.
     */
    public void notifyBookmarkHolders(ParagraphComment comment) {
        Long chapterId = comment.getChapter().getId();
        int paragraphIndex = comment.getParagraphIndex();
        String commenterId = comment.getUser().getId();

        List<String> userIds = bookmarkRepository.findUserIdsByChapterIdAndParagraphIndex(chapterId, paragraphIndex);

        String chapterTitle = comment.getChapter().getTitle() != null ? comment.getChapter().getTitle() : "Chapter " + comment.getChapter().getChapterNumber();
        String commenterName = formatUserName(comment.getUser());

        for (String userId : userIds) {
            // Don't notify the commenter themselves
            if (userId.equals(commenterId)) continue;

            User user = userRepository.findById(userId).orElse(null);
            if (user == null) continue;

            String content = commenterName + " commented on a paragraph you bookmarked in \"" + chapterTitle + "\"";
            createNotification(user, content);
        }
    }

    private void createNotification(User user, String content) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setContent(content);
        notification.setReaded(false);
        notificationRepository.save(notification);
    }

    private String formatUserName(User user) {
        if (user.getFirstName() != null && user.getLastName() != null) {
            return user.getFirstName() + " " + user.getLastName();
        }
        if (user.getFirstName() != null) return user.getFirstName();
        if (user.getUsername() != null) return user.getUsername();
        return "Someone";
    }
}

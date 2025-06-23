package com.content_storage_service.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

@Data // Generates getters, setters, equals, hashCode, toString
@NoArgsConstructor // Generates a no-args constructor
@AllArgsConstructor // Generates a constructor with all fields
@Builder // For creating instances easily with a builder pattern
public class RedditStoryTemplateParams {

    // Backgrounds & Music
    private String backgroundVideoId; // video_id
    private String backgroundMusicId; // music_id

    // User & Post Details
    private String avatarImageUrl; // URL to the avatar image
    private String username;       // Username to display
    private String subreddit;      // Subreddit name (e.g., "AskReddit")
    private String postTitle;      // Title of the Reddit post
    private String postDescription; // Description/body of the Reddit post

    // Comments
    private List<String> comments; // List of individual comment texts

    // Video/Rendering Settings
    private String aspectRatio; // e.g., "9:16"
    private Boolean showSubtitles; // bool
    private String subtitlesColor; // e.g., "#FFFFFF"
    //private String subtitlesFont;  // e.g., "Arial"
    //private String subtitlesPosition; // e.g., "bottom", "center", "top"

    // Voice Selection
    private String voiceSelection; // e.g., "elevenlabs_voice_id_X", "openai_voice_id_Y"
}
package com.content_generation_service.generation.service.reddit.visual;

import com.content_generation_service.generation.service.visual.ImageUtilitiesService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Orchestrates the creation of a simplified, static image for a Reddit post.
 * This service programmatically generates a header, body, and footer,
 * then combines them into a single PNG image.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedditImageService {

    private final ImageUtilitiesService imageUtils;

    // --- Design Constants ---
    private static final int TARGET_WIDTH = 750; // Width of the final image
    private static final int HORIZONTAL_PADDING = 25;
    private static final int VERTICAL_PADDING = 10;
    private static final int AVATAR_SIZE = 90;

    // Fonts
    private static final Font SUBREDDIT_FONT = new Font("Arial", Font.BOLD, 32);
    private static final Font USERNAME_FONT = new Font("Arial", Font.PLAIN, 28);
    private static final Font TITLE_FONT = new Font("Arial", Font.BOLD, 36);
    
    // Dark Theme Colors
    private static final Color DARK_BACKGROUND = new Color(22, 22, 22);
    private static final Color DARK_TEXT_PRIMARY = Color.WHITE;
    private static final Color DARK_TEXT_SECONDARY = Color.GRAY;

    // Light Theme Colors
    private static final Color LIGHT_BACKGROUND = new Color(245, 245, 245);
    private static final Color LIGHT_TEXT_PRIMARY = Color.BLACK;
    private static final Color LIGHT_TEXT_SECONDARY = Color.DARK_GRAY;


    /**
     * Creates the final Reddit post image by composing multiple assets.
     *
     * @param params A JsonNode containing parameters like 'postTitle', 'subreddit', 'username', 'avatarUrl', and 'theme'.
     * @return The path to the final generated PNG image.
     */
    public Path createRedditPostImage(JsonNode params) {
        log.info("Starting simplified Reddit post image creation process.");

        final String theme = params.get("theme").asText("dark");
        final String avatarUrl = params.get("avatarImageUrl").asText();
        final String subreddit = params.get("subreddit").asText();
        final String username = params.get("username").asText();
        final String postTitle = params.get("postTitle").asText();
        
        final Color bgColor = "dark".equals(theme) ? DARK_BACKGROUND : LIGHT_BACKGROUND;
        final Color primaryColor = "dark".equals(theme) ? DARK_TEXT_PRIMARY : LIGHT_TEXT_PRIMARY;
        final Color secondaryColor = "dark".equals(theme) ? DARK_TEXT_SECONDARY : LIGHT_TEXT_SECONDARY;

        try {
            // 2. Generate each section of the image
            BufferedImage header = createHeader(avatarUrl, subreddit, username, bgColor, primaryColor, secondaryColor);
            BufferedImage body = createTitleBody(postTitle, bgColor, primaryColor);
            BufferedImage footer = createFooter(bgColor, secondaryColor);

            // 3. Combine all sections vertically
            BufferedImage finalImage = imageUtils.combineVertically(header, body, footer);

            // 4. FIX: Apply rounded corners to the final composite image
            int cornerRadius = 50;
            BufferedImage roundedImage = imageUtils.makeRoundedCorner(finalImage, cornerRadius);

            // 5. Save the final rounded image to a temp file
            Path outputPath = Files.createTempFile("reddit-post-" + UUID.randomUUID(), ".png");
            imageUtils.saveImage(roundedImage, outputPath, "png");

            return outputPath;

        } catch (IOException e) {
            log.error("Failed to create Reddit post image.", e);
            throw new RuntimeException("Image generation failed due to an I/O error.", e);
        }
    }

    private BufferedImage createHeader(String avatarUrl, String subreddit, String username, Color bgColor, Color primaryColor, Color secondaryColor) throws IOException {
        //BufferedImage avatar = imageUtils.loadImageFromUrl(avatarUrl);
        BufferedImage avatar = imageUtils.loadImageFromResources(avatarUrl);
        BufferedImage resizedAvatar = imageUtils.resizeImage(avatar, AVATAR_SIZE, AVATAR_SIZE);

        int headerHeight = AVATAR_SIZE + (2 * VERTICAL_PADDING);
        BufferedImage headerCanvas = new BufferedImage(TARGET_WIDTH, headerHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = headerCanvas.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(bgColor);
        g.fillRect(0, 0, TARGET_WIDTH, headerHeight);
        g.drawImage(resizedAvatar, HORIZONTAL_PADDING, VERTICAL_PADDING, null);

        int textX = HORIZONTAL_PADDING + AVATAR_SIZE + 20;
        int textY = VERTICAL_PADDING + 35;

        g.setColor(primaryColor);
        g.setFont(SUBREDDIT_FONT);
        g.drawString(subreddit, textX, textY);
        
        g.setColor(secondaryColor);
        g.setFont(USERNAME_FONT);
        g.drawString("u/" + username, textX, textY + 40);

        g.dispose();
        log.info("Header section created successfully.");
        return headerCanvas;
    }
    
    private BufferedImage createTitleBody(String postTitle, Color bgColor, Color textColor) {
        log.info("Creating title body section.");
        // Reduced vertical padding to decrease spacing, while keeping horizontal padding at 40px.
        return imageUtils.renderTextToImage(postTitle, TITLE_FONT, textColor, bgColor, TARGET_WIDTH, HORIZONTAL_PADDING, 0);
    }
    
    private BufferedImage createFooter(Color bgColor, Color textColor) {
        int footerHeight = 50;
        BufferedImage footerCanvas = new BufferedImage(TARGET_WIDTH, footerHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = footerCanvas.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(bgColor);
        g.fillRect(0, 0, TARGET_WIDTH, footerHeight);

        // Use a logical font like "SansSerif" which is has emoji glyphs.
        g.setColor(textColor);
        g.setFont(new Font("SansSerif", Font.BOLD, 30));

        String upvoteText = "❤️  999+";
        String commentText = "💬  999+";
        
        int textY = footerHeight / 2 + 10;
        
        g.drawString(upvoteText, HORIZONTAL_PADDING, textY);
        g.drawString(commentText, HORIZONTAL_PADDING + 150, textY);

        g.dispose();
        log.info("Footer section created successfully.");
        return footerCanvas;
    }
}

package com.content_generation_service.generation.service.visual;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.awt.Dimension;

/**
 * Provides generic, reusable image manipulation utilities using Java's AWT.
 * This service is responsible for low-level tasks like combining images,
 * drawing text, and loading/saving image files.
 */
@Slf4j
@Service
public class ImageUtilitiesService {

    /**
     * Reads an image file and returns its dimensions.
     * @param imagePath The path to the image file.
     * @return A Dimension object containing the width and height.
     * @throws IOException if the file cannot be read or is not a valid image.
     */
    public Dimension getImageDimensions(Path imagePath) throws IOException {
        try {
            BufferedImage image = ImageIO.read(imagePath.toFile());
            if (image == null) {
                throw new IOException("Could not read image file or format is not supported: " + imagePath);
            }
            return new Dimension(image.getWidth(), image.getHeight());
        } catch (IOException e) {
            log.error("Failed to get dimensions for image: {}", imagePath, e);
            throw e;
        }
    }

    /**
     * Loads an image from the application's resources folder.
     *
     * @param resourcePath The path to the image within the resources folder (e.g., "assets/image.png").
     * @return The loaded image as a BufferedImage.
     * @throws IOException If the resource cannot be found or read.
     */
    public BufferedImage loadImageFromResources(String resourcePath) throws IOException {
        log.debug("Loading image from resource path: {}", resourcePath);
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            return ImageIO.read(is);
        }
    }

    public BufferedImage loadImageFromUrl(String urlString) throws IOException {
        log.debug("Loading image from URL: {}", urlString);
        try {
            return ImageIO.read(new URI(urlString).toURL());
        } catch (java.net.URISyntaxException e) {
            throw new IOException("Invalid URL syntax: " + urlString, e);
        }
    }

    public BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        float originalRatio = (float) originalWidth / originalHeight;
        float targetRatio = (float) targetWidth / targetHeight;

        int newWidth;
        int newHeight;

        if (originalRatio > targetRatio) {
            newWidth = targetWidth;
            newHeight = Math.round(targetWidth / originalRatio);
        } else {
            newHeight = targetHeight;
            newWidth = Math.round(targetHeight * originalRatio);
        }

        BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resizedImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(originalImage, 0, 0, newWidth, newHeight, null);
        g.dispose();
        return resizedImage;
    }

    public BufferedImage combineVertically(BufferedImage... images) {
        int maxWidth = 0;
        int totalHeight = 0;
        for (BufferedImage img : images) {
            if (img.getWidth() > maxWidth) {
                maxWidth = img.getWidth();
            }
            totalHeight += img.getHeight();
        }

        BufferedImage combined = new BufferedImage(maxWidth, totalHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = combined.createGraphics();

        int currentY = 0;
        for (BufferedImage img : images) {
            int xOffset = (maxWidth - img.getWidth()) / 2;
            g.drawImage(img, xOffset, currentY, null);
            currentY += img.getHeight();
        }
        g.dispose();
        return combined;
    }

    /**
     * Renders a block of text onto a new image with word-wrapping.
     * @param horizontalPadding The padding on the left and right.
     * @param verticalPadding   The padding on the top and bottom.
     * @return A new BufferedImage containing the rendered text.
     */
    public BufferedImage renderTextToImage(String text, Font font, Color textColor, Color bgColor, int width, int horizontalPadding, int verticalPadding) {
        BufferedImage tempImg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = tempImg.createGraphics();
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();

        List<String> lines = new ArrayList<>();
        int availableWidth = width - (2 * horizontalPadding);
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder(words[0]);

        for (int i = 1; i < words.length; i++) {
            if (fm.stringWidth(currentLine + " " + words[i]) < availableWidth) {
                currentLine.append(" ").append(words[i]);
            } else {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder(words[i]);
            }
        }
        lines.add(currentLine.toString());
        g2d.dispose();

        int lineHeight = fm.getHeight();
        int height = (lines.size() * lineHeight) + (2 * verticalPadding);
        BufferedImage textImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        g2d = textImage.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2d.setColor(bgColor);
        g2d.fillRect(0, 0, width, height);

        g2d.setColor(textColor);
        g2d.setFont(font);
        int y = verticalPadding + fm.getAscent();
        for (String line : lines) {
            g2d.drawString(line, horizontalPadding, y);
            y += lineHeight;
        }

        g2d.dispose();
        return textImage;
    }
    
    /**
     * Applies rounded corners to an image.
     * @param image The image to round.
     * @param cornerRadius The radius of the corners.
     * @return A new image with rounded corners and a transparent background.
     */
    public BufferedImage makeRoundedCorner(BufferedImage image, int cornerRadius) {
        if (image == null) {
            throw new IllegalArgumentException("Source image cannot be null.");
        }
        if (cornerRadius < 0) {
            cornerRadius = 0;
        }

        // Create a new image with an alpha channel to support transparency.
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Get the graphics object for the output image.
        Graphics2D g2 = output.createGraphics();

        try {
            // 1. Turn on anti-aliasing to get smooth curves.
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 2. Create a clipping shape (a rectangle with rounded corners).
            // The clip defines the area where drawing will be allowed.
            RoundRectangle2D.Float roundRect = new RoundRectangle2D.Float(0, 0, width, height, cornerRadius, cornerRadius);
            
            // 3. Set the clip on the graphics context.
            g2.setClip(roundRect);

            // 4. Draw the original image. Because the clip is set, only the parts
            // of the image within the rounded rectangle will be drawn.
            g2.drawImage(image, 0, 0, null);

        } finally {
            // 5. Dispose of the graphics context to release system resources.
            g2.dispose();
        }

        return output;
    }

    public void saveImage(BufferedImage image, Path outputPath, String format) throws IOException {
        ImageIO.write(image, format, outputPath.toFile());
        log.info("Successfully saved image to {}", outputPath.toAbsolutePath());
    }
}

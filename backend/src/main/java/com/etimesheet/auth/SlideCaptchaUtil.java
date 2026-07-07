package com.etimesheet.auth;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Random;

/**
 * 滑块验证码生成工具 - 不规则拼图形状
 */
public class SlideCaptchaUtil {

    static final int BG_WIDTH = 280;
    static final int BG_HEIGHT = 160;
    private static final int PIECE_SIZE = 44;
    private static final Random RANDOM = new Random();

    public static SlideCaptchaResult generate() {
        BufferedImage bg = new BufferedImage(BG_WIDTH, BG_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = bg.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. 绘制渐变背景
        Color color1 = randomColor();
        Color color2 = randomColor();
        GradientPaint gp = new GradientPaint(0, 0, color1, BG_WIDTH, BG_HEIGHT, color2);
        g2d.setPaint(gp);
        g2d.fillRect(0, 0, BG_WIDTH, BG_HEIGHT);

        // 2. 绘制随机形状
        drawRandomShapes(g2d);

        // 3. 随机拼图位置（水平居中范围，垂直居中）
        int pieceX = RANDOM.nextInt(BG_WIDTH - PIECE_SIZE * 2 - 20) + PIECE_SIZE + 10;
        int pieceY = (BG_HEIGHT - PIECE_SIZE) / 2;

        // 4. 创建不规则拼图形状（在 PIECE_SIZE 边界内）
        Shape puzzleShape = createPuzzleShape(0, 0, PIECE_SIZE);

        // 5. 生成拼图块图片（尺寸 = PIECE_SIZE × PIECE_SIZE）
        BufferedImage piece = new BufferedImage(PIECE_SIZE, PIECE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D pg2d = piece.createGraphics();
        pg2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 清空为透明
        pg2d.setComposite(AlphaComposite.Clear);
        pg2d.fillRect(0, 0, PIECE_SIZE, PIECE_SIZE);
        pg2d.setComposite(AlphaComposite.SrcOver);

        // 用不规则形状裁剪 + 复制背景像素
        pg2d.setClip(puzzleShape);
        pg2d.drawImage(bg, 0, 0, PIECE_SIZE, PIECE_SIZE, pieceX, pieceY, pieceX + PIECE_SIZE, pieceY + PIECE_SIZE, null);
        pg2d.dispose();

        // 6. 从背景中挖空拼图形状
        g2d.setComposite(AlphaComposite.Clear);
        Shape bgHole = createPuzzleShape(pieceX, pieceY, PIECE_SIZE);
        g2d.fill(bgHole);
        g2d.setComposite(AlphaComposite.SrcOver);

        // 7. 绘制槽位边框
        g2d.setColor(new Color(255, 255, 255, 90));
        g2d.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.draw(bgHole);

        // 8. 槽位内浅色阴影
        g2d.setColor(new Color(0, 0, 0, 30));
        Shape innerHole = createPuzzleShape(pieceX + 2, pieceY + 2, PIECE_SIZE - 4);
        g2d.fill(innerHole);

        g2d.dispose();

        // 9. 给拼图块描边 + 阴影
        pg2d = piece.createGraphics();
        pg2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 阴影
        pg2d.setColor(new Color(0, 0, 0, 35));
        pg2d.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Shape shadowShape = createPuzzleShape(0.5f, 0.5f, PIECE_SIZE);
        pg2d.draw(shadowShape);

        // 白色描边
        pg2d.setColor(new Color(255, 255, 255, 190));
        pg2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Shape pieceBorder = createPuzzleShape(0, 0, PIECE_SIZE);
        pg2d.draw(pieceBorder);

        pg2d.dispose();

        return new SlideCaptchaResult(
                toBase64(bg),
                toBase64(piece),
                pieceX,
                pieceY,
                PIECE_SIZE
        );
    }

    /**
     * 创建不规则拼图形状路径
     * 顶部有平滑凸起弧线，底部有匹配凹入弧线，四个角圆角过渡
     * 形状完全在 (x, y) 到 (x+size, y+size) 范围内
     */
    private static Shape createPuzzleShape(float x, float y, float size) {
        Path2D.Float path = new Path2D.Float();
        float mid = size / 2f;
        float wave = 5f; // 波浪幅度

        // 从左上角附近开始（预留圆角空间）
        path.moveTo(x + 3, y);

        // ── 顶部边缘：中间有一个平滑凸起 ──
        path.lineTo(x + mid - wave, y);
        // 凸起弧线（向上凸，最高点 y - wave*0.5）
        path.quadTo(x + mid, y - wave * 0.55f, x + mid + wave, y);
        path.lineTo(x + size - 3, y);

        // 右上圆角
        path.quadTo(x + size, y, x + size, y + 3);

        // ── 右侧边缘 ──
        path.lineTo(x + size, y + size - 3);
        path.quadTo(x + size, y + size, x + size - 3, y + size);

        // ── 底部边缘：中间有一个平滑凹入（和顶部凸起匹配） ──
        path.lineTo(x + mid + wave, y + size);
        // 凹入弧线（向上凹入，最深点 y + size - wave*0.5）
        path.quadTo(x + mid, y + size - wave * 0.4f, x + mid - wave, y + size);
        path.lineTo(x + 3, y + size);

        // 左下圆角
        path.quadTo(x, y + size, x, y + size - 3);

        // ── 左侧边缘 ──
        path.lineTo(x, y + 3);
        path.quadTo(x, y, x + 3, y);

        path.closePath();
        return path;
    }

    private static void drawRandomShapes(Graphics2D g2d) {
        for (int i = 0; i < 10; i++) {
            int alpha = RANDOM.nextInt(60) + 20;
            g2d.setColor(new Color(
                    RANDOM.nextInt(100) + 100,
                    RANDOM.nextInt(100) + 100,
                    RANDOM.nextInt(100) + 100,
                    alpha
            ));
            int x = RANDOM.nextInt(BG_WIDTH + 40) - 20;
            int y = RANDOM.nextInt(BG_HEIGHT + 40) - 20;
            int s = RANDOM.nextInt(50) + 15;
            g2d.fillOval(x, y, s, s);
        }

        g2d.setStroke(new BasicStroke(1.2f));
        for (int i = 0; i < 6; i++) {
            g2d.setColor(new Color(
                    RANDOM.nextInt(60) + 180,
                    RANDOM.nextInt(60) + 180,
                    RANDOM.nextInt(60) + 180,
                    50
            ));
            g2d.drawLine(
                    RANDOM.nextInt(BG_WIDTH), RANDOM.nextInt(BG_HEIGHT),
                    RANDOM.nextInt(BG_WIDTH), RANDOM.nextInt(BG_HEIGHT)
            );
        }
    }

    private static Color randomColor() {
        float hue = RANDOM.nextFloat();
        float sat = 0.45f + RANDOM.nextFloat() * 0.35f;
        float bri = 0.55f + RANDOM.nextFloat() * 0.3f;
        return Color.getHSBColor(hue, sat, bri);
    }

    private static String toBase64(BufferedImage image) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("滑块验证码生成失败", e);
        }
    }

    public static class SlideCaptchaResult {
        private final String bgImage;
        private final String pieceImage;
        private final int pieceX;
        private final int pieceY;
        private final int pieceSize;

        public SlideCaptchaResult(String bgImage, String pieceImage, int pieceX, int pieceY, int pieceSize) {
            this.bgImage = bgImage;
            this.pieceImage = pieceImage;
            this.pieceX = pieceX;
            this.pieceY = pieceY;
            this.pieceSize = pieceSize;
        }

        public String getBgImage() { return bgImage; }
        public String getPieceImage() { return pieceImage; }
        public int getPieceX() { return pieceX; }
        public int getPieceY() { return pieceY; }
        public int getPieceSize() { return pieceSize; }
    }
}

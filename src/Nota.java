import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Nota.java
 * Oggetto raccoglibile che spawna randomicamente nella stanza Casa.
 * Quando il giocatore ci passa sopra, appare un popup con il codice di debug.
 */
public class Nota {

    public float x, y;
    private final int size = 32;
    private final BufferedImage img;
    private boolean raccolta = false;

    // Animazione fluttuante
    private int tick = 0;

    public Nota(int tileX, int tileY, int tileSize, BufferedImage img) {
        this.x   = tileX * tileSize + (tileSize - size) / 2f;
        this.y   = tileY * tileSize + (tileSize - size) / 2f;
        this.img = img;
    }

    public void update() {
        if (!raccolta) tick++;
    }

    public boolean controllaRaccolta(float pgX, float pgY, int pgSize) {
        if (raccolta) return false;
        Rectangle hbPG   = new Rectangle((int) pgX, (int) pgY, pgSize, pgSize);
        Rectangle hbNota = new Rectangle((int) x, (int) y, size, size);
        return hbPG.intersects(hbNota);
    }

    public void raccogli() { raccolta = true; }
    public boolean isRaccolta() { return raccolta; }

    public void draw(Graphics2D g2) {
        if (raccolta) return;

        // Fluttuazione verticale
        float offset = (float) Math.sin(tick * 0.07f) * 4f;
        int dx = (int) x, dy = (int) (y + offset);

        // Alone giallo pulsante
        float pulse = 0.6f + 0.4f * (float) Math.sin(tick * 0.09f);
        g2.setColor(new Color(255, 230, 80, (int)(60 * pulse)));
        g2.fillOval(dx - 6, dy - 6, size + 12, size + 12);

        if (img != null) {
            g2.drawImage(img, dx, dy, size, size, null);
        } else {
            // Fallback: foglio bianco con scritte
            g2.setColor(new Color(240, 235, 200));
            g2.fillRoundRect(dx, dy, size, size, 4, 4);
            g2.setColor(new Color(100, 90, 60));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(dx, dy, size, size, 4, 4);
            g2.setStroke(new BasicStroke(1f));
            // Righine simulate
            g2.setColor(new Color(160, 150, 110));
            for (int i = 1; i <= 4; i++) {
                g2.drawLine(dx + 5, dy + i * 6, dx + size - 5, dy + i * 6);
            }
        }

        // Punto esclamativo lampeggiante sopra
        boolean blink = (System.currentTimeMillis() / 400) % 2 == 0;
        if (blink) {
            g2.setFont(new Font("Consolas", Font.BOLD, 14));
            g2.setColor(new Color(255, 220, 0));
            g2.drawString("!", dx + size / 2 - 3, dy - 4);
        }
    }
}
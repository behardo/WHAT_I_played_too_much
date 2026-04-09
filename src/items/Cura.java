package items;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Oggetto raccoglibile: cura un punto vita al giocatore.
 */
public class Cura {

    private final float x, y;
    private final int   size;
    private final BufferedImage img;
    private long spawnMs = System.currentTimeMillis();

    public Cura(int tileX, int tileY, int tileSize, BufferedImage img) {
        this.x    = tileX * tileSize;
        this.y    = tileY * tileSize;
        this.size = tileSize / 2;
        this.img  = img;
    }

    /** Ritorna true se il giocatore ha raccolto questo oggetto. */
    public boolean controllaRaccolta(float pgX, float pgY, int pgSize) {
        return pgX < x + size && pgX + pgSize > x
                && pgY < y + size && pgY + pgSize > y;
    }

    public void draw(Graphics2D g2) {
        float bob = (float) Math.sin((System.currentTimeMillis() - spawnMs) * 0.005) * 4f;
        int dy = (int) bob;
        if (img != null) {
            g2.drawImage(img, (int) x, (int) y + dy, size, size, null);
        } else {
            g2.setColor(new Color(220, 50, 50));
            g2.fillOval((int) x, (int) y + dy, size, size);
            g2.setColor(Color.WHITE);
            g2.drawString("+", (int) x + size / 4, (int) y + dy + size * 3 / 4);
        }
    }
}
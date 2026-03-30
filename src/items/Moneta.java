package items;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Oggetto raccoglibile: aggiunge monete al contatore del giocatore.
 */
public class Moneta {

    private final float x, y;
    private final int   size;
    private final BufferedImage img;
    private final int   valore;
    private long spawnMs = System.currentTimeMillis();

    public Moneta(int tileX, int tileY, int tileSize, BufferedImage img) {
        this.x      = tileX;
        this.y      = tileY;
        this.size   = tileSize / 2;
        this.img    = img;
        this.valore = 1;
    }

    public int getValore() { return valore; }

    /** Ritorna true se il giocatore ha raccolto questa moneta. */
    public boolean controllaRaccolta(float pgX, float pgY, int pgSize) {
        return pgX < x + size && pgX + pgSize > x
            && pgY < y + size && pgY + pgSize > y;
    }

    public void draw(Graphics2D g2) {
        float bob = (float) Math.sin((System.currentTimeMillis() - spawnMs) * 0.006) * 3f;
        int dy = (int) bob;
        if (img != null) {
            g2.drawImage(img, (int) x, (int) y + dy, size, size, null);
        } else {
            g2.setColor(new Color(255, 215, 0));
            g2.fillOval((int) x, (int) y + dy, size, size);
        }
    }
}

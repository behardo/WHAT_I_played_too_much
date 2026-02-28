import java.awt.*;
import java.awt.image.BufferedImage;

// --- GOIN DROP: Nuova classe per l'oggetto Moneta ---
public class Moneta {
    public float x, y;
    private int size = 35; // Leggermente più piccola della cura
    private int valore = 1;
    private BufferedImage imgMoneta;

    public Moneta(int grigliaX, int grigliaY, int tileSize, BufferedImage img) {
        // Centriamo l'oggetto nella tile
        this.x = grigliaX * tileSize + (tileSize/2 - size/2);
        this.y = grigliaY * tileSize + (tileSize/2 - size/2);
        this.imgMoneta = img;
    }

    public void draw(Graphics2D g2) {
        if (imgMoneta != null) {
            g2.drawImage(imgMoneta, (int)x, (int)y, size, size, null);
        } else {
            // Backup colorato (Giallo Oro)
            g2.setColor(Color.ORANGE);
            g2.fillOval((int)x, (int)y, size, size);
            g2.setColor(Color.WHITE);
            g2.drawOval((int)x, (int)y, size, size);
        }
    }

    // Hitbox circolare per la raccolta
    public boolean controllaRaccolta(float px, float py, int pSize) {
        float dx = (x + size/2) - (px + pSize/2);
        float dy = (y + size/2) - (py + pSize/2);
        float distanzaSq = dx*dx + dy*dy;
        return distanzaSq < (size/2 + pSize/2)*(size/2 + pSize/2);
    }

    public int getValore() {
        return valore;
    }
}

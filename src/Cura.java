import java.awt.*;
import java.awt.image.BufferedImage;

// --- CURA / GAME OVER: Nuova classe per l'oggetto di cura ---
public class Cura {
    public float x, y;
    private int size = 40;
    public boolean daRimuovere = false;
    private BufferedImage imgCura;

    public Cura(int grigliaX, int grigliaY, int tileSize, BufferedImage img) {
        // Centriamo l'oggetto nella tile
        this.x = grigliaX * tileSize + (tileSize/2 - size/2);
        this.y = grigliaY * tileSize + (tileSize/2 - size/2);
        this.imgCura = img;
    }

    public void draw(Graphics2D g2) {
        if (imgCura != null) {
            g2.drawImage(imgCura, (int)x, (int)y, size, size, null);
        } else {
            // Backup colorato (verde brillante)
            g2.setColor(Color.GREEN);
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
        // Collisione se la distanza è minore della somma dei raggi
        return distanzaSq < (size/2 + pSize/2)*(size/2 + pSize/2);
    }
}
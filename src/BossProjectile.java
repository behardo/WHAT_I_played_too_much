import java.awt.*;
import java.awt.image.BufferedImage;

// --- BOSS POTENZIATO: Nuova classe per i proiettili del Boss ---
public class BossProjectile {
    public float x, y;
    private float velocitaX, velocitaY;
    private int size = 25; // Più piccolo del pugno
    public boolean daRimuovere = false;
    private BufferedImage img;

    public BossProjectile(float startX, float startY, float targetX, float targetY, BufferedImage img) {
        this.x = startX;
        this.y = startY;
        this.img = img;

        // Calcola la direzione verso il giocatore
        float dx = targetX - startX;
        float dy = targetY - startY;
        float distanza = (float) Math.sqrt(dx * dx + dy * dy);

        // Imposta velocità (più lento dei pugni)
        float velocitaMax = 6.0f;
        this.velocitaX = (dx / distanza) * velocitaMax;
        this.velocitaY = (dy / distanza) * velocitaMax;
    }

    public void update() {
        x += velocitaX;
        y += velocitaY;
    }

    public void draw(Graphics2D g2) {
        if (img != null) {
            g2.drawImage(img, (int)x, (int)y, size, size, null);
        } else {
            // Backup colorato
            g2.setColor(Color.ORANGE);
            g2.fillRect((int)x, (int)y, size, size);
        }
    }

    // Hitbox per la collisione con il giocatore
    public Rectangle getHitbox() {
        return new Rectangle((int)x, (int)y, size, size);
    }
}
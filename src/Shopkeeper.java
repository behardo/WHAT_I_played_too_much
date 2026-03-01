import java.awt.*;
import java.awt.image.BufferedImage;

// --- SHOP: Classe per il venditore e la sua battuta ---
public class Shopkeeper {
    public float x, y;
    private int size = 80; // Più grande dei nemici
    private BufferedImage imgShopkeeper;
    private String battuta = "Non spenderli tutti figliolo!!";
    private boolean mostraBattuta = false;
    private int timerBattuta = 0;

    public Shopkeeper(int grigliaX, int grigliaY, int tileSize, BufferedImage img) {
        // Centriamo l'NPC nella tile
        this.x = grigliaX * tileSize + (tileSize/2 - size/2);
        this.y = grigliaY * tileSize + (tileSize/2 - size/2);
        this.imgShopkeeper = img;
    }

    public void draw(Graphics2D g2) {
        if (imgShopkeeper != null) {
            g2.drawImage(imgShopkeeper, (int)x, (int)y, size, size, null);
        } else {
            // Backup colorato
            g2.setColor(Color.LIGHT_GRAY);
            g2.fillRect((int)x, (int)y, size, size);
        }

        // --- SHOP: Disegno della battuta ---
        if (mostraBattuta) {
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 14));
            // Disegna il testo in un baloon (semplificato)
            g2.drawString(battuta, (int)x - 30, (int)y - 15);

            timerBattuta++;
            if (timerBattuta > 120) { // Mostra per 2 secondi
                mostraBattuta = false;
                timerBattuta = 0;
            }
        }
    }

    // Hitbox per il rilevamento della vicinanza (per la battuta)
    public Rectangle getHitbox() {
        return new Rectangle((int)x - 20, (int)y - 20, size + 40, size + 40);
    }

    public void attivaBattuta() {
        mostraBattuta = true;
        timerBattuta = 0;
    }
    public float getX() { return x; } //
    public float getY() { return y; } //
}

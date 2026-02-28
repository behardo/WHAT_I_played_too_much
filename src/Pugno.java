import java.awt.*;
import java.awt.image.BufferedImage;

public class Pugno {
    public float x, y;
    private float velocitaX, velocitaY;
    private float velocitaMax = 10.0f;
    private int size = 30; // Più piccolo del personaggio

    // --- SHOP: Il danno non è più fisso, viene passato dal giocatore ---
    private int danno;
    // ------------------------------------------------------------------

    private int durataFrame = 15; // Quanti frame vive il pugno
    private int frameVissuti = 0;
    public boolean daRimuovere = false;

    private BufferedImage imgPugno;

    // --- SHOP: Costruttore AGGIORNATO (accetta 6 parametri, incluso il danno) ---
    public Pugno(float startX, float startY, int direzioneX, int direzioneY, BufferedImage img, int dannoPassato) {
        // Centriamo il pugno sul personaggio
        this.x = startX + 10;
        this.y = startY + 10;
        this.imgPugno = img;

        // --- SHOP: Assegna il danno passato dal giocatore ---
        this.danno = dannoPassato;
        // ------------------------------------------------------

        // Impostiamo la velocità in base alla direzione
        this.velocitaX = direzioneX * velocitaMax;
        this.velocitaY = direzioneY * velocitaMax;

        // Se si spara in diagonale, normalizziamo la velocità
        if (direzioneX != 0 && direzioneY != 0) {
            this.velocitaX /= 1.414f;
            this.velocitaY /= 1.414f;
        }
    }

    public void update() {
        // Muovi il pugno
        x += velocitaX;
        y += velocitaY;

        // Gestisci la durata
        frameVissuti++;
        if (frameVissuti >= durataFrame) {
            daRimuovere = true;
        }
    }

    public void draw(Graphics2D g2) {
        if (imgPugno != null) {
            g2.drawImage(imgPugno, (int)x, (int)y, size, size, null);
        } else {
            // Backup colorato
            g2.setColor(Color.WHITE);
            g2.fillOval((int)x, (int)y, size, size);
        }
    }

    // Hitbox per la collisione con i nemici
    public Rectangle getHitbox() {
        return new Rectangle((int)x, (int)y, size, size);
    }

    public int getDanno() {
        return danno;
    }
}
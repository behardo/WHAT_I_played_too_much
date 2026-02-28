import java.awt.*;
import java.awt.image.BufferedImage;

public class Nemico {
    // Rendiamo x e y protette così Boss può accedervi
    protected float x, y;
    protected float velocita = 1.8f;
    protected int size = 50;

    // Sistema Salute Nemico
    private int vita;
    private int vitaMax;

    // Costruttore aggiornato per accettare la vita
    public Nemico(int grigliaX, int grigliaY, int tileSize, int vitaIniziale) {
        this.x = grigliaX * tileSize;
        this.y = grigliaY * tileSize;
        this.vitaMax = vitaIniziale;
        this.vita = vitaIniziale;
    }

    public void update(float playerX, float playerY) {
        // Logica inseguimento semplice verso il giocatore
        // (Aggiungiamo un piccolo offset per non sovrapporsi perfettamente)
        float targetX = playerX + 10;
        float targetY = playerY + 10;

        if (x < targetX) x += velocita;
        if (x > targetX) x -= velocita;
        if (y < targetY) y += velocita;
        if (y > targetY) y -= velocita;
    }

    public void draw(Graphics2D g2, BufferedImage img) {
        if (img != null) {
            g2.drawImage(img, (int)x, (int)y, size, size, null);
        } else {
            g2.setColor(Color.ORANGE); // Colore backup nemici
            g2.fillRect((int)x, (int)y, size, size);
        }

        // Barra della vita sopra il nemico (visibile solo se danneggiato)
        if (vita < vitaMax) {
            g2.setColor(Color.RED);
            g2.fillRect((int)x, (int)y - 7, size, 4); // Sfondo
            g2.setColor(Color.GREEN);
            int larghezzaVita = (int)(((float)vita / vitaMax) * size);
            g2.fillRect((int)x, (int)y - 7, larghezzaVita, 4); // Vita
        }
    }

    public boolean toccaGiocatore(float px, float py, int pSize) {
        // Collisione circolare approssimativa per i nemici
        float dx = (x + size/2) - (px + pSize/2);
        float dy = (y + size/2) - (py + pSize/2);
        float distanzaSq = dx*dx + dy*dy;
        return distanzaSq < (size/2 + pSize/2)*(size/2 + pSize/2);
    }

    // Hitbox quadra per la collisione con i pugni
    public Rectangle getHitbox() {
        return new Rectangle((int)x, (int)y, size, size);
    }

    public void subisciDanno(int danno) {
        vita -= danno;
    }

    public boolean isMorto() {
        return vita <= 0;
    }
}
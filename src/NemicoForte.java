import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * NemicoForte eredita da Nemico.
 * Ha una logica di movimento leggermente diversa (più veloce)
 * e ora accetta la vita dinamica per scalare con la difficoltà.
 */
public class NemicoForte extends Nemico {

    private float velocitaForte = 2.5f; // Più veloce del nemico base
    private int tagliaForte = 55;       // Leggermente più grande

    // --- CORREZIONE: Il nome del costruttore DEVE essere NemicoForte ---
    public NemicoForte(int grigliaX, int grigliaY, int tileSize, int vitaPassata) {
        // Passa i parametri al costruttore di Nemico
        super(grigliaX, grigliaY, tileSize, vitaPassata);

        // Possiamo personalizzare la velocità o altre statistiche qui se vogliamo
    }
    // ------------------------------------------------------------------

    @Override
    public void update(float playerX, float playerY) {
        // Inseguimento più aggressivo
        if (x < playerX) x += velocitaForte;
        else if (x > playerX) x -= velocitaForte;

        if (y < playerY) y += velocitaForte;
        else if (y > playerY) y -= velocitaForte;
    }

    @Override
    public void draw(Graphics2D g2, BufferedImage img) {
        if (img != null) {
            g2.drawImage(img, (int)x, (int)y, tagliaForte, tagliaForte, null);
        } else {
            // Backup visivo: un teschio o un colore diverso
            g2.setColor(Color.ORANGE);
            g2.fillOval((int)x, (int)y, tagliaForte, tagliaForte);
            g2.setColor(Color.BLACK);
            g2.drawOval((int)x, (int)y, tagliaForte, tagliaForte);
        }
    }

    @Override
    public Rectangle getHitbox() {
        // Hitbox leggermente più grande per riflettere la taglia
        return new Rectangle((int)x, (int)y, tagliaForte, tagliaForte);
    }
}
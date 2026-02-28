import java.awt.*;
import java.awt.image.BufferedImage;

// --- MONDO 2 / BOSS: Nuova classe per il Boss ---
public class Boss extends Nemico {
    private int sizeBoss = 120; // Molto più grande del nemico normale
    private float velocitaBoss = 1.3f; // Leggermente più lento ma inarrestabile
    private int vitaMaxBoss = 30; // Ci vogliono 30 colpi per sconfiggerlo
    private int vitaBoss;

    public Boss(int grigliaX, int grigliaY, int tileSize) {
        // Inizializziamo il Nemico base, ma sovrascriveremo la logica
        super(grigliaX, grigliaY, tileSize, 30);
        this.vitaBoss = vitaMaxBoss;
        // Posizioniamo la hitbox corretta per la dimensione del boss
        this.x = grigliaX * tileSize - (sizeBoss / 4);
        this.y = grigliaY * tileSize - (sizeBoss / 4);
    }

    @Override
    public void update(float playerX, float playerY) {
        // Inseguimento lento ma costante verso il giocatore
        if (x + (sizeBoss/2) < playerX + 25) x += velocitaBoss;
        if (x + (sizeBoss/2) > playerX + 25) x -= velocitaBoss;
        if (y + (sizeBoss/2) < playerY + 25) y += velocitaBoss;
        if (y + (sizeBoss/2) > playerY + 25) y -= velocitaBoss;
    }

    @Override
    public void draw(Graphics2D g2, BufferedImage img) {
        // Disegno del Boss (se img è null, usa un backup)
        if (img != null) {
            g2.drawImage(img, (int)x, (int)y, sizeBoss, sizeBoss, null);
        } else {
            g2.setColor(Color.MAGENTA); // Colore di backup per il Boss
            g2.fillRect((int)x, (int)y, sizeBoss, sizeBoss);
        }

        // Barra della vita del Boss (grande, in alto)
        int barW = sizeBoss;
        int barH = 12;
        int barY = (int)y - 20;

        g2.setColor(Color.GRAY);
        g2.fillRect((int)x, barY, barW, barH);

        // La vita diventa gialla/rossa man mano che diminuisce
        if (vitaBoss > vitaMaxBoss / 2) g2.setColor(Color.GREEN);
        else if (vitaBoss > vitaMaxBoss / 4) g2.setColor(Color.YELLOW);
        else g2.setColor(Color.RED);

        int larghezzaVita = (int)(((float)vitaBoss / vitaMaxBoss) * barW);
        g2.fillRect((int)x, barY, larghezzaVita, barH);

        // Contorno barra vita
        g2.setColor(Color.BLACK);
        g2.drawRect((int)x, barY, barW, barH);
    }

    @Override
    public Rectangle getHitbox() {
        // Hitbox generosa per il Boss
        return new Rectangle((int)x, (int)y, sizeBoss, sizeBoss);
    }

    @Override
    public void subisciDanno(int danno) {
        vitaBoss -= danno;
        // Knockback leggero quando colpito (opzionale)
    }

    @Override
    public boolean isMorto() {
        return vitaBoss <= 0;
    }
}

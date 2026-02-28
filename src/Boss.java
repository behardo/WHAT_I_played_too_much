import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

// --- MODALITA: Classe Boss aggiornata con vita dinamica ---
public class Boss extends Nemico {
    private int sizeBoss = 120; // Molto più grande del nemico normale
    private float velocitaBoss = 1.3f; // Lento ma inarrestabile

    // --- MODALITA: Vita dinamica passata dal costruttore ---
    private int vitaMaxBoss;
    private int vitaBoss;
    // --------------------------------------------------------

    // Logica per attacco a distanza
    private int cooldownSparo = 0;
    private final int SPARO_DELAY = 90; // Spara ogni 1.5 secondi
    private List<BossProjectile> proiettiliBoss = new ArrayList<>();
    private BufferedImage imgProiettile;

    // --- MODALITA: Costruttore AGGIORNATO (accetta 4 parametri, inclusa la vita) ---
    public Boss(int grigliaX, int grigliaY, int tileSize, int vitaPassata) {
        // Inizializziamo il Nemico base, ma sovrascriveremo la logica
        super(grigliaX, grigliaY, tileSize, vitaPassata);

        // --- MODALITA: Assegna la vita passata dal codice principale ---
        this.vitaMaxBoss = vitaPassata;
        this.vitaBoss = vitaMaxBoss;
        // --------------------------------------------------------------

        // Posizioniamo la hitbox corretta per la dimensione del boss
        this.x = grigliaX * tileSize - (sizeBoss / 4);
        this.y = grigliaY * tileSize - (sizeBoss / 4);
    }

    public void caricaProiettile(BufferedImage img) {
        this.imgProiettile = img;
    }

    @Override
    public void update(float playerX, float playerY) {
        // Inseguimento lento verso il giocatore
        if (x + (sizeBoss/2) < playerX + 25) x += velocitaBoss;
        if (x + (sizeBoss/2) > playerX + 25) x -= velocitaBoss;
        if (y + (sizeBoss/2) < playerY + 25) y += velocitaBoss;
        if (y + (sizeBoss/2) > playerY + 25) y -= velocitaBoss;

        // Logica Sparo
        if (cooldownSparo > 0) cooldownSparo--;
        if (cooldownSparo <= 0) {
            spara(playerX, playerY);
            cooldownSparo = SPARO_DELAY;
        }

        // Update proiettili
        for (int i = 0; i < proiettiliBoss.size(); i++) {
            BossProjectile p = proiettiliBoss.get(i);
            p.update();
            // Rimuovi se fuori dai limiti della stanza (semplificato)
            if (p.x < 64 || p.x > 1024 || p.y < 64 || p.y > 448) {
                proiettiliBoss.remove(i);
                i--;
            }
        }
    }

    private void spara(float targetX, float targetY) {
        // Crea il proiettile che parte dal centro del boss
        proiettiliBoss.add(new BossProjectile(x + sizeBoss/2, y + sizeBoss/2, targetX, targetY, imgProiettile));
    }

    @Override
    public void draw(Graphics2D g2, BufferedImage img) {
        if (img != null) {
            g2.drawImage(img, (int)x, (int)y, sizeBoss, sizeBoss, null);
        } else {
            g2.setColor(Color.MAGENTA); // Backup colorato
            g2.fillRect((int)x, (int)y, sizeBoss, sizeBoss);
        }

        // Disegno proiettili
        for (BossProjectile p : proiettiliBoss) {
            p.draw(g2);
        }
    }

    @Override
    public Rectangle getHitbox() {
        return new Rectangle((int)x, (int)y, sizeBoss, sizeBoss);
    }

    public boolean controllaCollisioneProiettili(float px, float py, int pSize) {
        Rectangle hbGiocatore = new Rectangle((int)px, (int)py, pSize, pSize);
        for (int i = 0; i < proiettiliBoss.size(); i++) {
            BossProjectile p = proiettiliBoss.get(i);
            if (p.getHitbox().intersects(hbGiocatore)) {
                proiettiliBoss.remove(i);
                return true;
            }
        }
        return false;
    }

    @Override
    public void subisciDanno(int danno) {
        vitaBoss -= danno;
    }

    @Override
    public boolean isMorto() {
        return vitaBoss <= 0;
    }

    // Getters per l'UI (ORA CORRETTI PER USARE LE VARIABILI DINAMICHE)
    public int getVita() { return vitaBoss; }
    public int getVitaMax() { return vitaMaxBoss; }
}
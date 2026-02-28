import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

// --- BOSS POTENZIATO: Classe Boss aggiornata ---
public class Boss extends Nemico {
    private int sizeBoss = 120; // Molto più grande del nemico normale
    private float velocitaBoss = 1.3f; // Lento ma inarrestabile

    // --- BOSS POTENZIATO: Vita notevolmente aumentata ---
    private static final int VITA_MAX_BOSS = 100; // Da 30 a 100 colpi
    private int vitaBoss;

    // Logica per attacco a distanza
    private int cooldownSparo = 0;
    private final int SPARO_DELAY = 90; // Spara ogni 1.5 secondi
    private List<BossProjectile> proiettiliBoss = new ArrayList<>();
    private BufferedImage imgProiettile;

    public Boss(int grigliaX, int grigliaY, int tileSize) {
        // Inizializziamo il Nemico base, sovrascriveremo la logica
        super(grigliaX, grigliaY, tileSize, VITA_MAX_BOSS);
        this.vitaBoss = VITA_MAX_BOSS;
        // Posizioniamo la hitbox corretta per la dimensione del boss
        this.x = grigliaX * tileSize - (sizeBoss / 4);
        this.y = grigliaY * tileSize - (sizeBoss / 4);
    }

    // --- BOSS POTENZIATO: Caricamento immagine proiettile ---
    public void caricaProiettile(BufferedImage img) {
        this.imgProiettile = img;
    }
    // ---------------------------------------------------------

    @Override
    public void update(float playerX, float playerY) {
        // Inseguimento lento verso il giocatore (invariato)
        if (x + (sizeBoss/2) < playerX + 25) x += velocitaBoss;
        if (x + (sizeBoss/2) > playerX + 25) x -= velocitaBoss;
        if (y + (sizeBoss/2) < playerY + 25) y += velocitaBoss;
        if (y + (sizeBoss/2) > playerY + 25) y -= velocitaBoss;

        // --- BOSS POTENZIATO: Logica Sparo ---
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
        // ------------------------------------
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

        // --- BOSS POTENZIATO: Disegno proiettili ---
        for (BossProjectile p : proiettiliBoss) {
            p.draw(g2);
        }
        // -------------------------------------------

        // Rimossa la barra della vita "sopra la testa", sarà nell'UI
    }

    @Override
    public Rectangle getHitbox() {
        return new Rectangle((int)x, (int)y, sizeBoss, sizeBoss);
    }

    // --- BOSS POTENZIATO: Controllo collisione proiettili con giocatore ---
    public boolean controllaCollisioneProiettili(float px, float py, int pSize) {
        Rectangle hbGiocatore = new Rectangle((int)px, (int)py, pSize, pSize);
        for (int i = 0; i < proiettiliBoss.size(); i++) {
            BossProjectile p = proiettiliBoss.get(i);
            if (p.getHitbox().intersects(hbGiocatore)) {
                proiettiliBoss.remove(i);
                return true; // Ha colpito!
            }
        }
        return false;
    }
    // ------------------------------------------------------------------------

    @Override
    public void subisciDanno(int danno) {
        vitaBoss -= danno;
    }

    @Override
    public boolean isMorto() {
        return vitaBoss <= 0;
    }

    // Getters per l'UI
    public int getVita() { return vitaBoss; }
    public int getVitaMax() { return VITA_MAX_BOSS; }
}
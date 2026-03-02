import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Boss.java — estende Nemico.
 *
 * Boss diversi per mondo (parametro mondoAttuale):
 *  Mondo 1 — Boss LENTO: insegue diretto, spara a intervalli lunghi
 *  Mondo 2 — Boss MULTIPLO: spara 4 proiettili a croce
 *  Mondo 3 — Boss CARICA: ogni tot frame scatta in avanti velocemente
 *  Mondo 4 — Boss FINALE: spara spirale + velocità crescente
 *
 * Tutti i boss scalano vita/danno col mondo.
 */
public class Boss extends Nemico {

    // ── Dimensione e tipo ─────────────────────────────────────────────────────
    private static final int TAGLIA_BOSS = 96;
    private final int tipo;   // = mondoAttuale % 4  (1-4)

    // ── Proiettili ────────────────────────────────────────────────────────────
    private final List<BossProjectile> proiettili = new ArrayList<>();
    private BufferedImage[] imgProiettiliPerTipo = new BufferedImage[4]; // indice 0-3
    private int cooldownSparo = 0;

    // Delay sparo per tipo
    private final int DELAY_SPARO;

    // ── Carica (tipo 3) ───────────────────────────────────────────────────────
    private int timerCarica = 0;
    private boolean inCarica = false;
    private float caricaDx = 0, caricaDy = 0;
    private static final int INTERVALLO_CARICA = 150;
    private static final int DURATA_CARICA     = 20;

    // ── Spirale (tipo 4) ──────────────────────────────────────────────────────
    private float angoloproiettile = 0f;

    // ── Costruttore ───────────────────────────────────────────────────────────
    public Boss(int tileX, int tileY, int tileSize, int vita, int mondoAttuale) {
        super(tileX, tileY, tileSize, vita);
        this.size       = TAGLIA_BOSS;
        this.dimensione = TAGLIA_BOSS;
        this.tipo       = ((mondoAttuale - 1) % 4) + 1;
        // Velocità crescente per mondo
        this.velocita   = StatNemico.velocitaBoss(mondoAttuale);
        // Delay sparo: diminuisce col mondo
        this.DELAY_SPARO = Math.max(40, 90 - (mondoAttuale - 1) * 12);
        // Centra nella tile
        this.x -= (TAGLIA_BOSS - tileSize) / 2f;
        this.y -= (TAGLIA_BOSS - tileSize) / 2f;
    }

    /** Carica lo stesso proiettile per tutti i tipi (compatibilità). */
    public void caricaProiettile(BufferedImage img) {
        java.util.Arrays.fill(imgProiettiliPerTipo, img);
    }
    /** Carica proiettili specifici per tipo (1-4). */
    public void caricaProiettiliPerTipo(java.awt.image.BufferedImage[] imgs) {
        for (int i = 0; i < 4 && i < imgs.length; i++)
            imgProiettiliPerTipo[i] = imgs[i];
    }
    private BufferedImage getImgProiettile() {
        BufferedImage img = imgProiettiliPerTipo[tipo - 1];
        if (img == null) img = imgProiettiliPerTipo[0]; // fallback al tipo 1
        return img;
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Override
    public void update(float pgX, float pgY, List<Nemico> altri) {
        if (morto) return;

        switch (tipo) {
            case 1 -> updateTipo1(pgX, pgY);
            case 2 -> updateTipo2(pgX, pgY);
            case 3 -> updateTipo3(pgX, pgY);
            case 4 -> updateTipo4(pgX, pgY);
        }

        // Aggiorna proiettili
        for (int i = 0; i < proiettili.size(); i++) {
            BossProjectile p = proiettili.get(i);
            p.update();
            if (p.x < 64 || p.x > GameState.LARGHEZZA_GIOCO - 64
                    || p.y < 64 || p.y > GameState.ALTEZZA_GIOCO - 64) {
                proiettili.remove(i--);
            }
        }

        clampBordi();
    }

    @Override
    public void update(float pgX, float pgY) { update(pgX, pgY, java.util.Collections.emptyList()); }

    // Tipo 1: lento, spara diretto
    private void updateTipo1(float pgX, float pgY) {
        muoviVerso(pgX, pgY, velocita);
        if (--cooldownSparo <= 0) {
            spara(pgX, pgY, 1);
            cooldownSparo = DELAY_SPARO;
        }
    }

    // Tipo 2: medio, spara a croce (4 direzioni)
    private void updateTipo2(float pgX, float pgY) {
        muoviVerso(pgX, pgY, velocita);
        if (--cooldownSparo <= 0) {
            sparaCroce();
            cooldownSparo = DELAY_SPARO;
        }
    }

    // Tipo 3: si carica verso il giocatore ogni tot frame
    private void updateTipo3(float pgX, float pgY) {
        if (inCarica) {
            x += caricaDx * 9f;
            y += caricaDy * 9f;
            if (--timerCarica <= 0) inCarica = false;
        } else {
            muoviVerso(pgX, pgY, velocita * 0.6f);
            timerCarica++;
            if (timerCarica >= INTERVALLO_CARICA) {
                // Prepara la carica
                float dx   = pgX - x, dy = pgY - y;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist > 1f) { caricaDx = dx / dist; caricaDy = dy / dist; }
                inCarica    = true;
                timerCarica = DURATA_CARICA;
            }
        }
        if (--cooldownSparo <= 0) {
            spara(pgX, pgY, 1);
            cooldownSparo = DELAY_SPARO;
        }
    }

    // Tipo 4: spara spirale + accelera
    private void updateTipo4(float pgX, float pgY) {
        // Velocità crescente con vita bassa
        float multVel = 1f + (1f - (float) vitaAttuale / vitaMax) * 1.5f;
        muoviVerso(pgX, pgY, velocita * multVel);
        if (--cooldownSparo <= 0) {
            sparaSpirale();
            cooldownSparo = DELAY_SPARO;
        }
    }

    // ── Movimento ─────────────────────────────────────────────────────────────

    private void muoviVerso(float tx, float ty, float vel) {
        float dx = tx - x, dy = ty - y;
        float d  = (float) Math.sqrt(dx * dx + dy * dy);
        if (d > 1f) { x += (dx / d) * vel; y += (dy / d) * vel; }
    }

    // ── Sparo ─────────────────────────────────────────────────────────────────

    private void spara(float tx, float ty, int n) {
        float cx = x + TAGLIA_BOSS / 2f, cy = y + TAGLIA_BOSS / 2f;
        proiettili.add(new BossProjectile(cx, cy, tx, ty, getImgProiettile()));
    }

    private void sparaCroce() {
        float cx = x + TAGLIA_BOSS / 2f, cy = y + TAGLIA_BOSS / 2f;
        float[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        for (float[] d : dirs)
            proiettili.add(new BossProjectile(cx, cy, cx + d[0]*100, cy + d[1]*100, getImgProiettile()));
    }

    private void sparaSpirale() {
        float cx = x + TAGLIA_BOSS / 2f, cy = y + TAGLIA_BOSS / 2f;
        for (int i = 0; i < 3; i++) {
            float a = angoloproiettile + (float)(i * Math.PI * 2 / 3);
            float tx = cx + (float) Math.cos(a) * 100;
            float ty = cy + (float) Math.sin(a) * 100;
            proiettili.add(new BossProjectile(cx, cy, tx, ty, getImgProiettile()));
        }
        angoloproiettile += 0.3f;
    }

    // ── Collisione proiettili col giocatore ───────────────────────────────────

    public boolean controllaCollisioneProiettili(float pgX, float pgY, int pgSize) {
        Rectangle hbPG = new Rectangle((int) pgX, (int) pgY, pgSize, pgSize);
        for (int i = 0; i < proiettili.size(); i++) {
            if (proiettili.get(i).getHitbox().intersects(hbPG)) {
                proiettili.remove(i);
                return true;
            }
        }
        return false;
    }

    // ── Draw ──────────────────────────────────────────────────────────────────

    @Override
    public void draw(Graphics2D g2, BufferedImage img) {
        if (morto) return;

        // Effetto lampeggio rosso se vita bassa
        boolean lampeggia = vitaAttuale < vitaMax * 0.25f
                && (System.currentTimeMillis() / 150) % 2 == 0;

        if (img != null) {
            if (lampeggia) {
                // Tinta rossa sopra lo sprite
                g2.drawImage(img, (int) x, (int) y, TAGLIA_BOSS, TAGLIA_BOSS, null);
                g2.setColor(new Color(255, 0, 0, 80));
                g2.fillRect((int) x, (int) y, TAGLIA_BOSS, TAGLIA_BOSS);
            } else {
                g2.drawImage(img, (int) x, (int) y, TAGLIA_BOSS, TAGLIA_BOSS, null);
            }
        } else {
            // Colore diverso per tipo
            Color[] colori = {Color.MAGENTA, new Color(0,150,200), new Color(200,80,0), new Color(150,0,150)};
            g2.setColor(lampeggia ? Color.RED : colori[tipo - 1]);
            g2.fillRect((int) x, (int) y, TAGLIA_BOSS, TAGLIA_BOSS);
        }

        // Disegna proiettili
        for (BossProjectile p : proiettili) p.draw(g2);
    }

    @Override
    public Rectangle getHitbox() { return new Rectangle((int) x, (int) y, TAGLIA_BOSS, TAGLIA_BOSS); }

    // ── Barra vita boss (offset più grande per taglia) ────────────────────────
    public void disegnaBarraVitaBoss(Graphics2D g2) {
        disegnaBarraVita(g2, -14);
    }
}
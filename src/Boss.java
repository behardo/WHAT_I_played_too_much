import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Boss.java — Quattro boss con comportamenti completamente distinti.
 *
 *  Tipo 1 — BRUTALE  : insegue diretto, spara 3 proiettili a ventaglio
 *  Tipo 2 — OMBRA    : si muove a scatti, spara a 8 direzioni
 *  Tipo 3 — CARICA   : carica verso il giocatore + bruciatura al contatto
 *                       + proiettili di fuoco
 *  Tipo 4 — FINALE   : si muove SPECULARMENTE al giocatore,
 *                       può schivare proiettili in arrivo (prob. bassa),
 *                       alterna spirale e burst mirato
 */
public class Boss extends Nemico {

    private static final int TAGLIA_BOSS = 96;
    private final int tipo;

    // ── Proiettili ────────────────────────────────────────────────────────────
    private final List<BossProjectile> proiettili = new ArrayList<>();
    private BufferedImage[] imgProiettiliPerTipo   = new BufferedImage[4];
    private int cooldownSparo = 0;
    private final int DELAY_SPARO;
    private float angoloSpirale = 0f;

    // ── Tipo 2 — scatti ───────────────────────────────────────────────────────
    private int timerScatto = 0;
    private boolean inScatto = false;
    private float scattoDx = 0, scattoDy = 0;
    private static final int INTERVALLO_SCATTO = 90;
    private static final int DURATA_SCATTO     = 8;

    // ── Tipo 3 — carica + burn ────────────────────────────────────────────────
    private int timerCarica = 0;
    private boolean inCarica = false;
    private float caricaDx = 0, caricaDy = 0;
    private static final int INTERVALLO_CARICA = 140;
    private static final int DURATA_CARICA     = 22;
    private Runnable onBurnPlayer = null;
    private int cooldownBurn = 0;

    // ── Tipo 4 — specchio + schivata ─────────────────────────────────────────
    private float dodgeDx = 0, dodgeDy = 0;
    private int   timerDodge = 0;
    private int   timerBurst = 0;
    private boolean modoBurst = false;
    private static final int   CICLO_BURST   = 200;
    private List<Pugno> pugniAttivi = null;
    private boolean primoSparoLog = false; // debug

    // ── Costruttore ───────────────────────────────────────────────────────────
    public Boss(int tileX, int tileY, int tileSize, int vita, int mondoAttuale) {
        super(tileX, tileY, tileSize, vita);
        this.size       = TAGLIA_BOSS;
        this.dimensione = TAGLIA_BOSS;
        this.tipo       = ((mondoAttuale - 1) % 4) + 1;
        this.velocita   = StatNemico.velocitaBoss(mondoAttuale);
        this.DELAY_SPARO = Math.max(35, 85 - (mondoAttuale - 1) * 12);
        this.x -= (TAGLIA_BOSS - tileSize) / 2f;
        this.y -= (TAGLIA_BOSS - tileSize) / 2f;
    }

    // ── Setters esterni ───────────────────────────────────────────────────────
    public void setOnBurnPlayer(Runnable cb)     { this.onBurnPlayer = cb; }
    public void setPugniAttiviRef(List<Pugno> l) { this.pugniAttivi  = l; }

    // ── Proiettili ────────────────────────────────────────────────────────────
    public void caricaProiettile(BufferedImage img) {
        java.util.Arrays.fill(imgProiettiliPerTipo, img);
    }
    public void caricaProiettiliPerTipo(BufferedImage[] imgs) {
        for (int i = 0; i < 4 && i < imgs.length; i++)
            imgProiettiliPerTipo[i] = imgs[i];
    }
    private BufferedImage getImgProiettile() {
        BufferedImage img = imgProiettiliPerTipo[tipo - 1];
        return img != null ? img : imgProiettiliPerTipo[0];
    }

    // ── Update ────────────────────────────────────────────────────────────────
    @Override
    public void update(float pgX, float pgY, List<Nemico> altri) {
        if (morto) return;
        switch (tipo) {
            case 1 -> updateBrutale(pgX, pgY);
            case 2 -> updateOmbra(pgX, pgY);
            case 3 -> updateCarica(pgX, pgY);
            case 4 -> updateFinale(pgX, pgY);
        }
        for (int i = proiettili.size() - 1; i >= 0; i--) {
            BossProjectile p = proiettili.get(i);
            if (tipo == 4) p.aggiornaTarget(pgX, pgY);
            p.update();
            if (p.x < 32 || p.x > GameState.LARGHEZZA_GIOCO - 32
                    || p.y < 32 || p.y > GameState.ALTEZZA_GIOCO - 32)
                proiettili.remove(i);
        }
        if (cooldownBurn > 0) cooldownBurn--;
        clampBordi();
    }

    /** Override a 4 parametri — chiamato da GameLoop con lista ostacoli.
     *  Il Boss ignora gli ostacoli (troppo grande) ma deve eseguire la sua IA. */
    @Override
    public void update(float pgX, float pgY, List<Nemico> altri, int[][] ostacoli) {
        update(pgX, pgY, altri);
    }

    @Override
    public void update(float pgX, float pgY) {
        update(pgX, pgY, java.util.Collections.emptyList());
    }

    // ── TIPO 1 — BRUTALE: ventaglio a 3 proiettili ───────────────────────────
    private void updateBrutale(float pgX, float pgY) {
        muoviVerso(pgX, pgY, velocita);
        if (--cooldownSparo <= 0) {
            sparaVentaglio(pgX, pgY, 3, 25f, BossProjectile.Tipo.NORMAL);
            cooldownSparo = DELAY_SPARO;
        }
    }

    // ── TIPO 2 — OMBRA: scatti + 8 direzioni ─────────────────────────────────
    private void updateOmbra(float pgX, float pgY) {
        if (inScatto) {
            x += scattoDx * 14f;
            y += scattoDy * 14f;
            if (--timerScatto <= 0) { inScatto = false; timerScatto = 0; }
        } else {
            muoviVerso(pgX, pgY, velocita * 0.5f);
            if (++timerScatto >= INTERVALLO_SCATTO) {
                float dx = pgX - x, dy = pgY - y;
                float d  = (float) Math.sqrt(dx * dx + dy * dy);
                if (d > 1f) { scattoDx = dx / d; scattoDy = dy / d; }
                inScatto    = true;
                timerScatto = DURATA_SCATTO;
                spara8Direzioni();
            }
        }
        if (--cooldownSparo <= 0) {
            spara8Direzioni();
            cooldownSparo = DELAY_SPARO + 20;
        }
    }

    // ── TIPO 3 — CARICA + BURN ────────────────────────────────────────────────
    private void updateCarica(float pgX, float pgY) {
        if (inCarica) {
            x += caricaDx * 10f;
            y += caricaDy * 10f;
            if (--timerCarica <= 0) inCarica = false;
            controllaContattoBurn(pgX, pgY);
        } else {
            muoviVerso(pgX, pgY, velocita * 0.55f);
            if (++timerCarica >= INTERVALLO_CARICA) {
                float dx = pgX - x, dy = pgY - y;
                float d  = (float) Math.sqrt(dx * dx + dy * dy);
                if (d > 1f) { caricaDx = dx / d; caricaDy = dy / d; }
                inCarica    = true;
                timerCarica = DURATA_CARICA;
            }
        }
        if (--cooldownSparo <= 0) {
            sparaVentaglio(pgX, pgY, 3, 18f, BossProjectile.Tipo.FUOCO);
            cooldownSparo = DELAY_SPARO;
        }
    }

    private void controllaContattoBurn(float pgX, float pgY) {
        if (cooldownBurn > 0) return;
        float cx   = x + TAGLIA_BOSS / 2f, cy = y + TAGLIA_BOSS / 2f;
        float dist = (float) Math.sqrt((cx - pgX) * (cx - pgX) + (cy - pgY) * (cy - pgY));
        if (dist < TAGLIA_BOSS * 0.85f) {
            if (onBurnPlayer != null) onBurnPlayer.run();
            cooldownBurn = 90;
        }
    }

    // ── Tipo 4 — FINALE ──────────────────────────────────────────────────────
    // Fase 1 (vita > 66%): movimento specchio + spirale
    // Fase 2 (vita 33-66%): specchio più aggressivo + burst + schivata frequente
    // Fase 3 (vita < 33%): FURIA — insegue direttamente + schivata quasi certa
    //                       + spirale veloce + burst simultaneo
    private static final float PROB_SCHIVATA_F1 = 0.08f;  // ~1 ogni 12 frame con pugno vicino
    private static final float PROB_SCHIVATA_F2 = 0.22f;  // ~1 ogni 5 frame
    private static final float PROB_SCHIVATA_F3 = 0.55f;  // più di 1 su 2
    private static final float DIST_DODGE_TRIGGER = 200f; // px — distanza pugno che triggera dodge
    private static final int   DURATA_DODGE_F1 = 14;
    private static final int   DURATA_DODGE_F2 = 20;
    private static final int   DURATA_DODGE_F3 = 26;
    private static final float VEL_DODGE_BASE  = 8f;

    private int fase4 = 1;  // 1, 2 o 3
    private int cooldownDodge = 0; // cooldown minimo tra schivate successive

    private void updateFinale(float pgX, float pgY) {
        // Calcola fase in base alla vita rimanente
        float vitaPerc = (float) vitaAttuale / vitaMax;
        fase4 = vitaPerc > 0.66f ? 1 : vitaPerc > 0.33f ? 2 : 3;

        float multVel = switch (fase4) {
            case 2 -> 1.3f;
            case 3 -> 1.7f;
            default -> 1.0f;
        };

        if (cooldownDodge > 0) cooldownDodge--;

        // ── Schivata ──────────────────────────────────────────────────────────
        if (timerDodge > 0) {
            x += dodgeDx * VEL_DODGE_BASE * (fase4 == 3 ? 1.4f : 1f);
            y += dodgeDy * VEL_DODGE_BASE * (fase4 == 3 ? 1.4f : 1f);
            timerDodge--;
        } else if (cooldownDodge <= 0 && pugniAttivi != null && !pugniAttivi.isEmpty()) {
            float bossCx = x + TAGLIA_BOSS / 2f;
            float bossCy = y + TAGLIA_BOSS / 2f;
            float prob = switch (fase4) {
                case 2 -> PROB_SCHIVATA_F2;
                case 3 -> PROB_SCHIVATA_F3;
                default -> PROB_SCHIVATA_F1;
            };

            // Cerca il pugno più vicino (in avvicinamento non verificabile senza getter,
            // ma la distanza < soglia è già un buon trigger)
            Pugno pugnoTarget = null;
            float distMin = DIST_DODGE_TRIGGER;
            for (Pugno p : pugniAttivi) {
                float dx = bossCx - p.x, dy = bossCy - p.y;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist < distMin) {
                    distMin = dist;
                    pugnoTarget = p;
                }
            }

            if (pugnoTarget != null && Math.random() < prob) {
                float dx = bossCx - pugnoTarget.x, dy = bossCy - pugnoTarget.y;
                float norm = (float) Math.sqrt(dx * dx + dy * dy);
                if (norm > 0.1f) {
                    // Schiva perpendicolarmente alla traiettoria del pugno
                    dodgeDx = -dy / norm;
                    dodgeDy =  dx / norm;
                    if (Math.random() < 0.5) { dodgeDx = -dodgeDx; dodgeDy = -dodgeDy; }
                }
                timerDodge = switch (fase4) {
                    case 2 -> DURATA_DODGE_F2;
                    case 3 -> DURATA_DODGE_F3;
                    default -> DURATA_DODGE_F1;
                };
                cooldownDodge = timerDodge + 8; // non schivare di nuovo subito
            }

            // Movimento base (anche quando non schiva)
            muoviFinale(pgX, pgY, multVel);
        } else {
            muoviFinale(pgX, pgY, multVel);
        }

        // ── Sparo ─────────────────────────────────────────────────────────────
        if (++timerBurst > CICLO_BURST) { modoBurst = !modoBurst; timerBurst = 0; }
        if (--cooldownSparo <= 0) {
            switch (fase4) {
                case 1 -> {
                    sparaSpirale();
                    cooldownSparo = DELAY_SPARO;
                }
                case 2 -> {
                    if (modoBurst) sparaBurst(pgX, pgY, 6, 12f);
                    else           sparaSpirale();
                    cooldownSparo = modoBurst ? DELAY_SPARO * 2 / 3 : DELAY_SPARO;
                }
                case 3 -> {
                    // Furia: spara entrambi simultaneamente
                    sparaSpirale();
                    sparaBurst(pgX, pgY, 8, 20f);
                    cooldownSparo = Math.max(20, DELAY_SPARO / 2);
                }
            }
        }
    }

    /** Logica di movimento del boss finale in base alla fase. */
    private void muoviFinale(float pgX, float pgY, float multVel) {
        float cx = GameState.LARGHEZZA_GIOCO / 2f;
        float cy = GameState.ALTEZZA_GIOCO   / 2f;

        if (fase4 == 3) {
            // Furia: insegue direttamente il giocatore invece di muoversi specchio
            muoviVerso(pgX, pgY, velocita * multVel);
        } else {
            // Specchio rispetto al centro stanza, con deriva laterale in fase 2
            float targetX = 2 * cx - pgX - TAGLIA_BOSS / 2f;
            float targetY = 2 * cy - pgY - TAGLIA_BOSS / 2f;

            if (fase4 == 2) {
                // Aggiunge un offset oscillante che rende il movimento imprevedibile
                float t = (float)(System.currentTimeMillis() % 3000) / 3000f;
                float wave = (float) Math.sin(t * Math.PI * 2) * 80f;
                targetX += wave;
                targetY += (float) Math.cos(t * Math.PI * 2) * 40f;
            }
            muoviVerso(targetX, targetY, velocita * multVel);
        }
    }

    // ── Metodi di sparo ───────────────────────────────────────────────────────

    private float cx() { return x + TAGLIA_BOSS / 2f; }
    private float cy() { return y + TAGLIA_BOSS / 2f; }

    private void sparaVentaglio(float pgX, float pgY, int n, float semiAngleDeg, BossProjectile.Tipo t) {
        float base  = (float) Math.atan2(pgY - cy(), pgX - cx());
        float step  = n > 1 ? (float) Math.toRadians(semiAngleDeg * 2 / (n - 1)) : 0;
        float start = base - (float) Math.toRadians(semiAngleDeg);
        for (int i = 0; i < n; i++) {
            float a = start + step * i;
            proiettili.add(new BossProjectile(cx(), cy(),
                    cx() + (float) Math.cos(a) * 200,
                    cy() + (float) Math.sin(a) * 200,
                    getImgProiettile(), t));
        }
        if (!primoSparoLog) {
            primoSparoLog = true;
            System.out.println("[Boss" + tipo + "] Primo sparo! pos=(" + (int)x + "," + (int)y
                    + ") proiettili=" + proiettili.size() + " img=" + getImgProiettile());
        }
    }

    private void spara8Direzioni() {
        for (int i = 0; i < 8; i++) {
            float a = (float)(i * Math.PI / 4);
            proiettili.add(new BossProjectile(cx(), cy(),
                    cx() + (float) Math.cos(a) * 200,
                    cy() + (float) Math.sin(a) * 200,
                    getImgProiettile(), BossProjectile.Tipo.CROCE));
        }
    }

    private void sparaSpirale() {
        for (int i = 0; i < 3; i++) {
            float a = angoloSpirale + (float)(i * Math.PI * 2 / 3);
            proiettili.add(new BossProjectile(cx(), cy(),
                    cx() + (float) Math.cos(a) * 200,
                    cy() + (float) Math.sin(a) * 200,
                    getImgProiettile(), BossProjectile.Tipo.FINALE));
        }
        angoloSpirale += 0.28f;
    }

    private void sparaBurst(float pgX, float pgY, int n, float spreadDeg) {
        float base = (float) Math.atan2(pgY - cy(), pgX - cx());
        for (int i = 0; i < n; i++) {
            float jitter = (float)((Math.random() - 0.5) * Math.toRadians(spreadDeg));
            float a = base + jitter;
            proiettili.add(new BossProjectile(cx(), cy(),
                    cx() + (float) Math.cos(a) * 200,
                    cy() + (float) Math.sin(a) * 200,
                    getImgProiettile(), BossProjectile.Tipo.FINALE));
        }
    }

    // ── Movimento ────────────────────────────────────────────────────────────
    private void muoviVerso(float tx, float ty, float vel) {
        float dx = tx - x, dy = ty - y;
        float d  = (float) Math.sqrt(dx * dx + dy * dy);
        if (d > 1f) { x += (dx / d) * vel; y += (dy / d) * vel; }
    }

    // ── Collisioni ────────────────────────────────────────────────────────────
    public BossProjectile.Tipo controllaCollisioneProiettiliConTipo(float pgX, float pgY, int pgSize) {
        Rectangle hbPG = new Rectangle((int) pgX, (int) pgY, pgSize, pgSize);
        for (int i = proiettili.size() - 1; i >= 0; i--) {
            if (proiettili.get(i).getHitbox().intersects(hbPG)) {
                BossProjectile.Tipo t = proiettili.get(i).getTipo();
                proiettili.remove(i);
                return t;
            }
        }
        return null;
    }

    public boolean controllaCollisioneProiettili(float pgX, float pgY, int pgSize) {
        return controllaCollisioneProiettiliConTipo(pgX, pgY, pgSize) != null;
    }

    // ── Draw ──────────────────────────────────────────────────────────────────
    @Override
    public void draw(Graphics2D g2, BufferedImage img) {
        if (morto) return;
        boolean lampeggia = vitaAttuale < vitaMax * 0.25f
                && (System.currentTimeMillis() / 150) % 2 == 0;

        if (tipo == 3 && inCarica) {
            g2.setColor(new Color(255, 120, 0, 100));
            g2.fillOval((int) x - 8, (int) y - 8, TAGLIA_BOSS + 16, TAGLIA_BOSS + 16);
        }
        if (tipo == 4 && timerDodge > 0) {
            g2.setColor(new Color(160, 0, 255, 80));
            g2.fillOval((int) x - 10, (int) y - 10, TAGLIA_BOSS + 20, TAGLIA_BOSS + 20);
        }
        // Fase 3: alone rosso pulsante
        if (tipo == 4 && fase4 == 3) {
            long t = System.currentTimeMillis();
            int alpha = 60 + (int)(50 * Math.sin(t * 0.008));
            g2.setColor(new Color(255, 30, 30, alpha));
            g2.fillOval((int) x - 14, (int) y - 14, TAGLIA_BOSS + 28, TAGLIA_BOSS + 28);
        }

        if (img != null) {
            g2.drawImage(img, (int) x, (int) y, TAGLIA_BOSS, TAGLIA_BOSS, null);
            if (lampeggia) {
                g2.setColor(new Color(255, 0, 0, 80));
                g2.fillRect((int) x, (int) y, TAGLIA_BOSS, TAGLIA_BOSS);
            }
        } else {
            Color[] colori = { Color.MAGENTA, new Color(0,150,200), new Color(220,80,0), new Color(150,0,200) };
            g2.setColor(lampeggia ? Color.RED : colori[tipo - 1]);
            g2.fillRect((int) x, (int) y, TAGLIA_BOSS, TAGLIA_BOSS);
        }
        for (BossProjectile p : proiettili) p.draw(g2);
    }

    @Override
    public Rectangle getHitbox() { return new Rectangle((int) x, (int) y, TAGLIA_BOSS, TAGLIA_BOSS); }

    public void disegnaBarraVitaBoss(Graphics2D g2) { disegnaBarraVita(g2, -14); }
}
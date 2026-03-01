import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Entita.java
 * Classe base astratta per tutti gli enti di gioco con vita:
 * Nemico, NemicoForte, Boss, Shopkeeper.
 *
 * Gestisce:
 *  - Posizione in pixel (x, y)
 *  - Vita attuale e massima
 *  - Barra della vita visibile solo dopo aver ricevuto danno
 *  - Timer barra vita (scompare dopo N frame senza danno)
 *  - Hitbox rettangolare
 *  - Flag morto
 */
public abstract class Entita {

    // ── Posizione e dimensione ────────────────────────────────────────────────
    public float x, y;
    public int   dimensione;

    // ── Vita ──────────────────────────────────────────────────────────────────
    protected int vitaMax;
    protected int vitaAttuale;

    // ── Barra vita ────────────────────────────────────────────────────────────
    /** Quanti frame mostrare la barra vita dopo un danno */
    private static final int DURATA_BARRA = 120;
    private int timerBarra = 0;   // countdown, 0 = non visibile

    // ── Stato ─────────────────────────────────────────────────────────────────
    protected boolean morto = false;

    // ── Costruttore ───────────────────────────────────────────────────────────
    public Entita(int tileX, int tileY, int tileSize, int vitaMax) {
        this.x          = tileX * tileSize;
        this.y          = tileY * tileSize;
        this.dimensione = tileSize;
        this.vitaMax    = vitaMax;
        this.vitaAttuale = vitaMax;
    }

    // ── Vita e danno ──────────────────────────────────────────────────────────

    public void subisciDanno(int danno) {
        if (morto) return;
        vitaAttuale  = Math.max(0, vitaAttuale - danno);
        timerBarra   = DURATA_BARRA;   // mostra la barra
        if (vitaAttuale == 0) morto = true;
    }

    public boolean isMorto()    { return morto; }
    public int     getVita()    { return vitaAttuale; }
    public int     getVitaMax() { return vitaMax; }

    // ── Hitbox ────────────────────────────────────────────────────────────────

    public Rectangle getHitbox() {
        return new Rectangle((int) x, (int) y, dimensione, dimensione);
    }

    public boolean toccaGiocatore(float pgX, float pgY, int pgSize) {
        return getHitbox().intersects(new Rectangle((int) pgX, (int) pgY, pgSize, pgSize));
    }

    // ── Tick ──────────────────────────────────────────────────────────────────

    /**
     * Aggiorna il timer della barra vita. Da chiamare ogni frame.
     */
    public void tickBarra() {
        if (timerBarra > 0) timerBarra--;
    }

    /**
     * True se la barra vita deve essere disegnata (ha ricevuto danno di recente).
     */
    public boolean barraDaMostrare() {
        return timerBarra > 0;
    }

    // ── Disegno barra vita ────────────────────────────────────────────────────

    /**
     * Disegna la barra vita sopra l'entità.
     * Colore: verde → giallo → rosso in base alla percentuale vita.
     * Chiamare DOPO aver chiamato tickBarra() e solo se barraDaMostrare() == true.
     *
     * @param g2      Graphics2D corrente
     * @param offsetY pixel sopra l'entità dove disegnare la barra (default -10)
     */
    public void disegnaBarraVita(Graphics2D g2, int offsetY) {
        if (!barraDaMostrare()) return;

        int barW = dimensione;
        int barH = 6;
        int bx   = (int) x;
        int by   = (int) y + offsetY;

        // Sfondo barra
        g2.setColor(new Color(30, 30, 30, 200));
        g2.fillRect(bx, by, barW, barH);

        // Riempimento proporzionale alla vita
        float perc = (float) vitaAttuale / vitaMax;
        int riempito = (int)(barW * perc);

        // Colore: verde > 60%, giallo > 30%, rosso altrimenti
        Color colore;
        if      (perc > 0.6f) colore = new Color(50,  200, 50);
        else if (perc > 0.3f) colore = new Color(220, 180, 0);
        else                  colore = new Color(220, 50,  50);

        g2.setColor(colore);
        g2.fillRect(bx, by, riempito, barH);

        // Bordo
        g2.setColor(new Color(0, 0, 0, 150));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRect(bx, by, barW, barH);

        // Dissolvenza: la barra diventa trasparente negli ultimi 30 frame
        if (timerBarra < 30) {
            int alpha = (int)(180 * (timerBarra / 30.0));
            g2.setColor(new Color(0, 0, 0, alpha));
            g2.fillRect(bx, by, barW, barH + 1);
        }
    }

    /** Overload con offset default -10px */
    public void disegnaBarraVita(Graphics2D g2) {
        disegnaBarraVita(g2, -10);
    }

    // ── Metodo astratto ───────────────────────────────────────────────────────

    /**
     * Aggiorna la logica dell'entità (movimento, AI, proiettili...).
     * Implementato da ogni sottoclasse.
     */
    public abstract void update(float pgX, float pgY);

    /**
     * Disegna l'entità (sprite o fallback colore).
     */
    public abstract void draw(Graphics2D g2, BufferedImage sprite);
}
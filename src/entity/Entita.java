package entity;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Entita.java
 * Classe base astratta per tutti gli enti di gioco con vita.
 */
public abstract class Entita {

    public float x, y;
    public int   dimensione;

    protected int vitaMax;
    protected int vitaAttuale;

    private static final int DURATA_BARRA = 120;
    private int timerBarra = 0;

    protected boolean morto = false;

    public Entita(int tileX, int tileY, int tileSize, int vitaMax) {
        this.x          = tileX * tileSize;
        this.y          = tileY * tileSize;
        this.dimensione = tileSize;
        this.vitaMax    = vitaMax;
        this.vitaAttuale = vitaMax;
    }

    public void subisciDanno(int danno) {
        if (morto) return;
        vitaAttuale  = Math.max(0, vitaAttuale - danno);
        timerBarra   = DURATA_BARRA;
        if (vitaAttuale == 0) morto = true;
    }

    public boolean isMorto()    { return morto; }
    public int     getVita()    { return vitaAttuale; }
    public int     getVitaMax() { return vitaMax; }

    public Rectangle getHitbox() {
        return new Rectangle((int) x, (int) y, dimensione, dimensione);
    }

    public boolean toccaGiocatore(float pgX, float pgY, int pgSize) {
        return getHitbox().intersects(new Rectangle((int) pgX, (int) pgY, pgSize, pgSize));
    }

    public void tickBarra() {
        if (timerBarra > 0) timerBarra--;
    }

    public boolean barraDaMostrare() {
        return timerBarra > 0;
    }

    public void disegnaBarraVita(Graphics2D g2, int offsetY) {
        if (!barraDaMostrare()) return;

        int barW = dimensione;
        int barH = 6;
        int bx   = (int) x;
        int by   = (int) y + offsetY;

        g2.setColor(new Color(30, 30, 30, 200));
        g2.fillRect(bx, by, barW, barH);

        float perc = (float) vitaAttuale / vitaMax;
        int riempito = (int)(barW * perc);

        Color colore;
        if      (perc > 0.6f) colore = new Color(50,  200, 50);
        else if (perc > 0.3f) colore = new Color(220, 180, 0);
        else                  colore = new Color(220, 50,  50);

        g2.setColor(colore);
        g2.fillRect(bx, by, riempito, barH);

        g2.setColor(new Color(0, 0, 0, 150));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRect(bx, by, barW, barH);

        if (timerBarra < 30) {
            int alpha = (int)(180 * (timerBarra / 30.0));
            g2.setColor(new Color(0, 0, 0, alpha));
            g2.fillRect(bx, by, barW, barH + 1);
        }
    }

    public void disegnaBarraVita(Graphics2D g2) {
        disegnaBarraVita(g2, -10);
    }

    public abstract void update(float pgX, float pgY);
    public abstract void draw(Graphics2D g2, BufferedImage sprite);
}

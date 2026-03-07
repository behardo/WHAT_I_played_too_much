import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * TileEffetto.java
 * Un tile speciale sovrapposto al pavimento normale con effetti sul giocatore.
 *
 * Tipi:
 *  VELENO         — mondo 2 (Fogne): danno nel tempo (verde)
 *  GHIACCIO       — mondo 3 (Tundra): slow temporaneo (azzurro chiaro)
 *  GHIACCIO_FORTE — mondo 3 (Tundra): freeze immediato (blu scuro, raro)
 *  FUOCO          — mondo 4 (Fornace): danno + burn (arancio)
 *  CANNONE        — mondo 5 (Castello): colpisce se in linea di tiro
 */
public class TileEffetto {

    public enum Tipo { VELENO, GHIACCIO, GHIACCIO_FORTE, FUOCO, CANNONE }

    public final int   col;
    public final int   rig;
    public final Tipo  tipo;

    // Timer per effetti ciclici (cannone, veleno tick)
    private int cooldown = 0;

    // Per cannone: timer tiro
    private static final int CANNONE_DELAY = 360; // 6 secondi

    public TileEffetto(int col, int rig, Tipo tipo) {
        this.col  = col;
        this.rig  = rig;
        this.tipo = tipo;
        // Offset iniziale random per evitare tutti-insieme
        this.cooldown = (int)(Math.random() * 90);
    }

    public void tick() {
        if (cooldown > 0) cooldown--;
    }

    public boolean isReady() {
        return cooldown <= 0;
    }

    public void resetCooldown() {
        switch (tipo) {
            case VELENO         -> cooldown = 50;
            case GHIACCIO       -> cooldown = 45;
            case GHIACCIO_FORTE -> cooldown = 80; // più raro
            case FUOCO          -> cooldown = 60;
            case CANNONE        -> cooldown = CANNONE_DELAY;
        }
    }

    /**
     * Controlla se il giocatore sta sopra questo tile.
     */
    public boolean toccaGiocatore(float pgX, float pgY, int pgSize, int tileSize) {
        int margin = tileSize / 4;
        int tx1 = col * tileSize + margin;
        int ty1 = rig * tileSize + margin;
        int tx2 = tx1 + tileSize - margin * 2;
        int ty2 = ty1 + tileSize - margin * 2;
        int px1 = (int)pgX + margin;
        int py1 = (int)pgY + margin;
        int px2 = px1 + pgSize - margin * 2;
        int py2 = py1 + pgSize - margin * 2;
        return px1 < tx2 && px2 > tx1 && py1 < ty2 && py2 > ty1;
    }

    /**
     * Disegna il tile sovrapposto al pavimento.
     * Usa l'immagine fornita oppure un fallback colorato.
     */
    public void draw(Graphics2D g2, int tileSize, BufferedImage img) {
        int px = col * tileSize;
        int py = rig * tileSize;

        if (img != null) {
            g2.drawImage(img, px, py, tileSize, tileSize, null);
        } else {
            // Fallback: rettangolo colorato semitrasparente
            long ms = System.currentTimeMillis();
            float pulse = 0.55f + 0.45f * (float)Math.sin(ms * 0.006 + col * 0.7 + rig * 0.5);
            Color col2 = switch (tipo) {
                case VELENO         -> new Color(60,  200, 60,  (int)(140 * pulse));
                case GHIACCIO       -> new Color(80,  200, 255, (int)(150 * pulse));
                case GHIACCIO_FORTE -> new Color(20,  80,  210, (int)(170 * pulse));
                case FUOCO          -> new Color(255, 120, 30,  (int)(160 * pulse));
                case CANNONE        -> new Color(180, 160, 80,  (int)(130 * pulse));
            };
            g2.setColor(col2);
            g2.fillRect(px + 2, py + 2, tileSize - 4, tileSize - 4);
            // Bordo
            Color bordo = switch (tipo) {
                case VELENO         -> new Color(30,  160, 30,  200);
                case GHIACCIO       -> new Color(120, 230, 255, 220);
                case GHIACCIO_FORTE -> new Color(80,  140, 255, 240);
                case FUOCO          -> new Color(255, 60,  0,   220);
                case CANNONE        -> new Color(220, 200, 80,  200);
            };
            g2.setColor(bordo);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRect(px + 2, py + 2, tileSize - 4, tileSize - 4);
            g2.setStroke(new BasicStroke(1f));
        }
    }
}
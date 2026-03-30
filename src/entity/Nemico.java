package entity;

import core.GameState;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Nemico.java — estende Entita.
 *
 * AI migliorata:
 *  - Inseguimento del giocatore con steering smussato
 *  - Separazione dagli altri nemici (no clipping)
 *  - Rimane dentro i bordi della stanza
 */
public class Nemico extends Entita {

    protected float velocita = 1.6f;
    protected int   size     = 50;

    public void setVelocita(float v) { this.velocita = v; }

    public Nemico(int tileX, int tileY, int tileSize, int vita) {
        super(tileX, tileY, tileSize, vita);
        this.dimensione = size;
    }

    // ── Update con separazione ────────────────────────────────────────────────

    public void update(float pgX, float pgY, List<Nemico> altri) {
        if (morto) return;

        float dx = (pgX + 25) - (x + size / 2f);
        float dy = (pgY + 25) - (y + size / 2f);
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        float vx = 0, vy = 0;
        if (dist > 1f) {
            vx = (dx / dist) * velocita;
            vy = (dy / dist) * velocita;
        }

        // Separazione dagli altri nemici — impedisce il clipping
        for (Nemico altro : altri) {
            if (altro == this || altro.morto) continue;
            float ox    = x - altro.x;
            float oy    = y - altro.y;
            float odist = (float) Math.sqrt(ox * ox + oy * oy);
            float minD  = (size + altro.size) * 0.6f;
            if (odist < minD && odist > 0.1f) {
                float push = (minD - odist) / minD;
                vx += (ox / odist) * push * 2.5f;
                vy += (oy / odist) * push * 2.5f;
            }
        }

        x += vx;
        y += vy;
        clampBordi();
    }

    /** Variante con controllo ostacoli: applica vx e vy separatamente e annulla l'asse che colpisce. */
    public void update(float pgX, float pgY, List<Nemico> altri, int[][] ostacoli) {
        if (morto) return;

        float dx = (pgX + 25) - (x + size / 2f);
        float dy = (pgY + 25) - (y + size / 2f);
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        float vx = 0, vy = 0;
        if (dist > 1f) {
            vx = (dx / dist) * velocita;
            vy = (dy / dist) * velocita;
        }

        // Separazione dagli altri nemici
        for (Nemico altro : altri) {
            if (altro == this || altro.morto) continue;
            float ox    = x - altro.x;
            float oy    = y - altro.y;
            float odist = (float) Math.sqrt(ox * ox + oy * oy);
            float minD  = (size + altro.size) * 0.6f;
            if (odist < minD && odist > 0.1f) {
                float push = (minD - odist) / minD;
                vx += (ox / odist) * push * 2.5f;
                vy += (oy / odist) * push * 2.5f;
            }
        }

        // Collisione ostacoli — applica X e Y separatamente
        x += vx;
        if (ostacoli != null && colpisce(ostacoli)) x -= vx;
        y += vy;
        if (ostacoli != null && colpisce(ostacoli)) y -= vy;

        clampBordi();
    }

    private boolean colpisce(int[][] ostacoli) {
        final int T = GameState.TILE_SIZE;
        final int m = 4;
        int x1 = (int)x + m, y1 = (int)y + m;
        int x2 = (int)x + size - m, y2 = (int)y + size - m;
        for (int[] o : ostacoli) {
            int ox1 = o[0]*T + m, oy1 = o[1]*T + m;
            int ox2 = (o[0]+1)*T - m, oy2 = (o[1]+1)*T - m;
            if (x1 < ox2 && x2 > ox1 && y1 < oy2 && y2 > oy1) return true;
        }
        return false;
    }

    @Override
    public void update(float pgX, float pgY) {
        update(pgX, pgY, java.util.Collections.emptyList());
    }

    protected void clampBordi() {
        int T = GameState.TILE_SIZE;
        int O = GameState.OFFSET;
        float minX = O * T, maxX = (O + GameState.COL_GIOCO) * T - size;
        float minY = O * T, maxY = (O + GameState.RIG_GIOCO) * T - size;
        if (x < minX) x = minX; if (x > maxX) x = maxX;
        if (y < minY) y = minY; if (y > maxY) y = maxY;
    }

    @Override
    public void draw(Graphics2D g2, BufferedImage img) {
        if (morto) return;
        if (img != null) g2.drawImage(img, (int) x, (int) y, size, size, null);
        else { g2.setColor(Color.ORANGE); g2.fillRect((int) x, (int) y, size, size); }
    }

    @Override
    public Rectangle getHitbox() { return new Rectangle((int) x, (int) y, size, size); }

    public float getX() { return x; }
    public float getY() { return y; }
}

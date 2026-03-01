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

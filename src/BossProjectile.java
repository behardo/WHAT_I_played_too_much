import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * BossProjectile.java
 *
 * Proiettile sparato dai boss. Supporta diversi tipi visivi/comportamentali:
 *  NORMAL  — proiettile standard (boss 1)
 *  CROCE   — proiettile a croce (boss 2), stesso comportamento ma tinta blu
 *  FUOCO   — proiettile del boss 3, arancione pulsante, causa burn
 *  FINALE  — proiettile del boss 4, viola, leggermente tracking
 */
public class BossProjectile {

    public enum Tipo { NORMAL, CROCE, FUOCO, FINALE }

    public float x, y;
    private float vx, vy;
    private static final float SPEED = 5.5f;
    private static final int   SIZE  = 20;

    private final Tipo          tipo;
    private final BufferedImage img;

    // Per FINALE: lieve homing
    private float pgX, pgY;
    private static final float HOMING = 0.04f;

    // Animazione
    private int tick = 0;

    // ── Costruttori ───────────────────────────────────────────────────────────

    public BossProjectile(float sx, float sy, float tx, float ty, BufferedImage img) {
        this(sx, sy, tx, ty, img, Tipo.NORMAL);
    }

    public BossProjectile(float sx, float sy, float tx, float ty,
                          BufferedImage img, Tipo tipo) {
        this.x    = sx;
        this.y    = sy;
        this.img  = img;
        this.tipo = tipo;
        float dx = tx - sx, dy = ty - sy;
        float d  = (float) Math.sqrt(dx * dx + dy * dy);
        if (d > 0.5f) { vx = dx / d * SPEED; vy = dy / d * SPEED; }
        else           { vx = SPEED; vy = 0; }
        this.pgX = tx;
        this.pgY = ty;
    }

    // ── Update ────────────────────────────────────────────────────────────────

    public void update() {
        tick++;
        if (tipo == Tipo.FINALE) {
            // Leggero homing verso l'ultima pos nota del giocatore
            float dx = pgX - x, dy = pgY - y;
            float d  = (float) Math.sqrt(dx * dx + dy * dy);
            if (d > 1f) {
                vx += (dx / d) * HOMING;
                vy += (dy / d) * HOMING;
                // Normalizza per mantenere velocità costante
                float speed = (float) Math.sqrt(vx * vx + vy * vy);
                if (speed > SPEED * 1.3f) { vx = vx / speed * SPEED * 1.3f; vy = vy / speed * SPEED * 1.3f; }
            }
        }
        x += vx;
        y += vy;
    }

    /** Aggiorna la posizione target per il tracking (solo FINALE). */
    public void aggiornaTarget(float tx, float ty) {
        this.pgX = tx;
        this.pgY = ty;
    }

    // ── Draw ──────────────────────────────────────────────────────────────────

    public void draw(Graphics2D g2) {
        if (img != null) {
            // Tinta per tipo
            g2.drawImage(img, (int) x - SIZE / 2, (int) y - SIZE / 2, SIZE, SIZE, null);
            Color tint = switch (tipo) {
                case FUOCO  -> new Color(255, 100, 0,  120);
                case FINALE -> new Color(180, 0,   255, 80);
                case CROCE  -> new Color(0,   150, 255, 60);
                default     -> null;
            };
            if (tint != null) {
                g2.setColor(tint);
                g2.fillOval((int) x - SIZE / 2, (int) y - SIZE / 2, SIZE, SIZE);
            }
        } else {
            disegnaFallback(g2);
        }
    }

    private void disegnaFallback(Graphics2D g2) {
        int ix = (int) x - SIZE / 2, iy = (int) y - SIZE / 2;
        switch (tipo) {
            case FUOCO -> {
                // Palla di fuoco pulsante
                float pulse = 0.8f + 0.2f * (float) Math.sin(tick * 0.4f);
                int sz = (int)(SIZE * pulse);
                g2.setColor(new Color(255, 60,  0));
                g2.fillOval(ix, iy, sz, sz);
                g2.setColor(new Color(255, 200, 0, 180));
                g2.fillOval(ix + 4, iy + 4, sz - 8, sz - 8);
            }
            case FINALE -> {
                // Sfera viola con alone
                g2.setColor(new Color(120, 0, 200, 100));
                g2.fillOval(ix - 4, iy - 4, SIZE + 8, SIZE + 8);
                g2.setColor(new Color(200, 100, 255));
                g2.fillOval(ix, iy, SIZE, SIZE);
            }
            case CROCE -> {
                g2.setColor(new Color(50, 180, 255));
                g2.fillRect(ix, iy + SIZE / 2 - 3, SIZE, 6);
                g2.fillRect(ix + SIZE / 2 - 3, iy, 6, SIZE);
            }
            default -> {
                g2.setColor(new Color(255, 80, 80));
                g2.fillOval(ix, iy, SIZE, SIZE);
            }
        }
    }

    // ── Hitbox ────────────────────────────────────────────────────────────────

    public Rectangle getHitbox() {
        return new Rectangle((int) x - SIZE / 2, (int) y - SIZE / 2, SIZE, SIZE);
    }

    public Tipo getTipo() { return tipo; }
}
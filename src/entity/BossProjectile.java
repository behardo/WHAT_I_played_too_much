package entity;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * BossProjectile.java
 */
public class BossProjectile {

    public enum Tipo { NORMAL, CROCE, FUOCO, FINALE }

    public float x, y;
    private float vx, vy;
    private static final float SPEED = 5.5f;
    private static final int   SIZE  = 20;

    private final Tipo          tipo;
    private final BufferedImage img;

    private float pgX, pgY;
    private static final float HOMING = 0.04f;

    private int tick = 0;

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

    public void update() {
        tick++;
        if (tipo == Tipo.FINALE) {
            float dx = pgX - x, dy = pgY - y;
            float d  = (float) Math.sqrt(dx * dx + dy * dy);
            if (d > 1f) {
                vx += (dx / d) * HOMING;
                vy += (dy / d) * HOMING;
                float speed = (float) Math.sqrt(vx * vx + vy * vy);
                if (speed > SPEED * 1.3f) { vx = vx / speed * SPEED * 1.3f; vy = vy / speed * SPEED * 1.3f; }
            }
        }
        x += vx;
        y += vy;
    }

    public void aggiornaTarget(float tx, float ty) {
        this.pgX = tx;
        this.pgY = ty;
    }

    public void draw(Graphics2D g2) {
        if (img != null) {
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
        int cx = (int) x, cy = (int) y;
        switch (tipo) {
            case FUOCO -> {
                float pulse = 0.85f + 0.15f * (float) Math.sin(tick * 0.35f);
                int outer = (int)(SIZE * 1.6f * pulse);
                int inner = (int)(SIZE * 0.9f * pulse);
                g2.setColor(new Color(255, 60, 0, 60));
                g2.fillOval(cx - outer/2, cy - outer/2, outer, outer);
                g2.setColor(new Color(255, 100, 0));
                g2.fillOval(cx - SIZE/2, cy - SIZE/2, SIZE, SIZE);
                g2.setColor(new Color(255, 230, 50, 220));
                g2.fillOval(cx - inner/2, cy - inner/2, inner, inner);
                g2.setColor(new Color(180, 40, 0));
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(cx - SIZE/2, cy - SIZE/2, SIZE, SIZE);
                g2.setStroke(new BasicStroke(1f));
            }
            case FINALE -> {
                int outer2 = (int)(SIZE * 1.8f);
                g2.setColor(new Color(100, 0, 180, 50));
                g2.fillOval(cx - outer2/2, cy - outer2/2, outer2, outer2);
                g2.setColor(new Color(160, 0, 255, 130));
                g2.fillOval(cx - SIZE/2 - 4, cy - SIZE/2 - 4, SIZE + 8, SIZE + 8);
                g2.setColor(new Color(220, 140, 255));
                g2.fillOval(cx - SIZE/2, cy - SIZE/2, SIZE, SIZE);
                g2.setColor(new Color(255, 200, 255, 200));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawLine(cx - SIZE/3, cy, cx + SIZE/3, cy);
                g2.drawLine(cx, cy - SIZE/3, cx, cy + SIZE/3);
                g2.setStroke(new BasicStroke(1f));
            }
            case CROCE -> {
                g2.setColor(new Color(30, 120, 255, 70));
                g2.fillOval(cx - SIZE/2 - 4, cy - SIZE/2 - 4, SIZE + 8, SIZE + 8);
                g2.setColor(new Color(80, 180, 255));
                g2.fillOval(cx - SIZE/2, cy - SIZE/2, SIZE, SIZE);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(3f));
                g2.drawLine(cx - SIZE/2 + 2, cy, cx + SIZE/2 - 2, cy);
                g2.drawLine(cx, cy - SIZE/2 + 2, cx, cy + SIZE/2 - 2);
                g2.setStroke(new BasicStroke(1f));
            }
            default -> {
                g2.setColor(new Color(255, 60, 60, 80));
                g2.fillOval(cx - SIZE/2 - 3, cy - SIZE/2 - 3, SIZE + 6, SIZE + 6);
                g2.setColor(new Color(255, 80, 80));
                g2.fillOval(cx - SIZE/2, cy - SIZE/2, SIZE, SIZE);
                g2.setColor(new Color(255, 200, 200, 180));
                g2.fillOval(cx - SIZE/4, cy - SIZE/4, SIZE/2, SIZE/2);
                g2.setColor(new Color(200, 30, 30));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(cx - SIZE/2, cy - SIZE/2, SIZE, SIZE);
                g2.setStroke(new BasicStroke(1f));
            }
        }
    }

    public Rectangle getHitbox() {
        return new Rectangle((int) x - SIZE / 2, (int) y - SIZE / 2, SIZE, SIZE);
    }

    public Tipo getTipo() { return tipo; }
}

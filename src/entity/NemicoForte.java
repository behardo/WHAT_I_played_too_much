package entity;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * NemicoForte — nemico veloce, più grande, si aggira attorno al giocatore
 * per evitare di essere prevedibile.
 */
public class NemicoForte extends Nemico {

    private static final float VEL     = 2.8f;
    private static final int   TAGLIA  = 58;
    // Offset orbitale: si avvicina da angoli diversi
    private float angleOffset;

    public NemicoForte(int tileX, int tileY, int tileSize, int vita) {
        super(tileX, tileY, tileSize, vita);
        this.velocita   = VEL;
        this.size       = TAGLIA;
        this.dimensione = TAGLIA;
        // Offset casuale per non sovrapporsi agli altri NemicoForte
        this.angleOffset = (float)(Math.random() * Math.PI * 2);
    }

    @Override
    public void update(float pgX, float pgY, List<Nemico> altri) {
        if (morto) return;

        // Punto target leggermente orbitale attorno al giocatore
        double angle = Math.atan2(y - pgY, x - pgX) + angleOffset * 0.01;
        float targetX = pgX + (float)(Math.cos(angle) * 30);
        float targetY = pgY + (float)(Math.sin(angle) * 30);

        float dx   = targetX - x;
        float dy   = targetY - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        float vx = 0, vy = 0;
        if (dist > 2f) {
            vx = (dx / dist) * velocita;
            vy = (dy / dist) * velocita;
        }

        // Separazione
        for (Nemico altro : altri) {
            if (altro == this || altro.morto) continue;
            float ox = x - altro.x, oy = y - altro.y;
            float od = (float) Math.sqrt(ox * ox + oy * oy);
            float md = (size + altro.size) * 0.55f;
            if (od < md && od > 0.1f) {
                float push = (md - od) / md;
                vx += (ox / od) * push * 3f;
                vy += (oy / od) * push * 3f;
            }
        }

        x += vx; y += vy;
        clampBordi();
    }

    @Override
    public void draw(Graphics2D g2, BufferedImage img) {
        if (morto) return;
        if (img != null) g2.drawImage(img, (int) x, (int) y, size, size, null);
        else {
            g2.setColor(new Color(220, 80, 0));
            g2.fillOval((int) x, (int) y, size, size);
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval((int) x, (int) y, size, size);
            g2.setStroke(new BasicStroke(1f));
        }
    }

    @Override
    public Rectangle getHitbox() { return new Rectangle((int) x, (int) y, size, size); }
}

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * ShopItem.java
 * Oggetto acquistabile in negozio o raccoglibile nella stanza Casa.
 *
 * Costruttore: ShopItem(tileX, tileY, tileSize, tipo, prezzo, img)
 *  tipo   — "CURA", "VELOCITA", "DANNO"
 *  prezzo — monete richieste (0 = gratis)
 *
 * Nella stanza Casa chiamare setMostraPrezzo(false) per nascondere il costo.
 */
public class ShopItem {

    // ── Posizione e dimensione ────────────────────────────────────────────────
    private final float x, y;
    private final int   size;

    // ── Dati ─────────────────────────────────────────────────────────────────
    private final String        tipo;
    private final int           prezzo;
    private final BufferedImage img;

    // ── Stato ────────────────────────────────────────────────────────────────
    private boolean acquistato   = false;
    private boolean mostraPrezzo = true;   // false nella stanza Casa

    // ── Costruttore ───────────────────────────────────────────────────────────
    public ShopItem(int tileX, int tileY, int tileSize, String tipo, int prezzo, BufferedImage img) {
        this.x      = tileX * tileSize;
        this.y      = tileY * tileSize;
        this.size   = tileSize;
        this.tipo   = tipo;
        this.prezzo = prezzo;
        this.img    = img;
    }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setMostraPrezzo(boolean v) { this.mostraPrezzo = v; }
    public void setAcquistato()            { this.acquistato   = true; }

    // ── Getters ───────────────────────────────────────────────────────────────
    public String  getTipo()  { return tipo;   }
    public int     getCosto() { return prezzo; }
    public boolean isAcquistato() { return acquistato; }

    // ── Hitbox ────────────────────────────────────────────────────────────────
    public Rectangle getHitbox() {
        return new Rectangle((int) x, (int) y, size, size);
    }

    /**
     * Controlla se il giocatore è sopra l'item e può raccoglierlo/acquistarlo.
     * Se prezzo == 0 (Casa) lo raccoglie al solo contatto, senza bisogno di monete.
     */
    public boolean controllaAcquisto(float pgX, float pgY, int pgSize, int monete) {
        if (acquistato) return false;
        Rectangle hbPG   = new Rectangle((int) pgX, (int) pgY, pgSize, pgSize);
        Rectangle hbItem = getHitbox();
        if (!hbPG.intersects(hbItem)) return false;
        return prezzo == 0 || monete >= prezzo;
    }

    // ── Draw ──────────────────────────────────────────────────────────────────
    public void draw(Graphics2D g2) {
        if (acquistato) return;

        int ix = (int) x, iy = (int) y;

        // ── Sprite o fallback colorato ────────────────────────────────────────
        if (img != null) {
            g2.drawImage(img, ix, iy, size, size, null);
        } else {
            Color col = switch (tipo) {
                case "CURA"     -> new Color(220, 60,  60);
                case "VELOCITA" -> new Color(60,  180, 255);
                case "DANNO"    -> new Color(255, 120, 30);
                default         -> new Color(180, 180, 180);
            };
            g2.setColor(col);
            g2.fillRoundRect(ix + 6, iy + 6, size - 12, size - 12, 10, 10);
            g2.setColor(col.brighter());
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(ix + 6, iy + 6, size - 12, size - 12, 10, 10);
            g2.setStroke(new BasicStroke(1f));
        }

        // ── Etichetta nome ────────────────────────────────────────────────────
        int fs = Math.max(9, size / 5);
        g2.setFont(new Font("Consolas", Font.BOLD, fs));
        FontMetrics fm = g2.getFontMetrics();
        String label = nomeBreveTipo();
        int lx = ix + size / 2 - fm.stringWidth(label) / 2;
        int ly = iy - 4;
        // Ombra
        g2.setColor(new Color(0, 0, 0, 160));
        g2.drawString(label, lx + 1, ly + 1);
        g2.setColor(Color.WHITE);
        g2.drawString(label, lx, ly);

        // ── Prezzo (solo se mostraPrezzo == true) ─────────────────────────────
        if (mostraPrezzo && prezzo > 0) {
            String prezzoStr = prezzo + " ¢";
            int px = ix + size / 2 - fm.stringWidth(prezzoStr) / 2;
            int py = iy + size + fs + 2;
            g2.setColor(new Color(0, 0, 0, 160));
            g2.drawString(prezzoStr, px + 1, py + 1);
            g2.setColor(new Color(255, 220, 60));
            g2.drawString(prezzoStr, px, py);
        }
    }

    private String nomeBreveTipo() {
        return switch (tipo) {
            case "CURA"     -> "CURA";
            case "VELOCITA" -> "VEL";
            case "DANNO"    -> "DMG";
            default         -> tipo;
        };
    }
}
import java.awt.*;

/**
 * MenuButton.java
 * Bottone interattivo riutilizzabile con:
 *  - Stato hover (cambia colore quando il mouse ci passa sopra)
 *  - Ombra per dare profondità
 *  - Stato disabilitato (grigio, non cliccabile)
 *  - Testo centrato automaticamente
 *
 * Uso:
 *   MenuButton btn = new MenuButton("GIOCA", x, y, larghezza, altezza);
 *   btn.aggiornaMouse(mouseX, mouseY);   // chiamato da mouseMoved
 *   btn.draw(g2);                        // chiamato da paintComponent
 *   if (btn.contains(clickPoint)) { ... } // chiamato da mouseClicked
 */
public class MenuButton {

    // ── Dati ─────────────────────────────────────────────────────────────────
    public final String    label;
    public Rectangle bounds;

    /** Aggiorna posizione e dimensione del bottone (per layout fluido). */
    public void setBounds(int x, int y, int w, int h) {
        this.bounds = new Rectangle(x, y, w, h);
    }
    public       boolean   hover     = false;
    public       boolean   disabilitato = false;

    // ── Colori personalizzabili ───────────────────────────────────────────────
    private Color coloreNormale  = new Color(40, 40, 60);
    private Color coloreHover    = new Color(70, 70, 120);
    private Color coloreBordo    = new Color(120, 120, 180);
    private Color coloreBordoHov = new Color(180, 180, 255);
    private Color coloreTesto    = Color.WHITE;
    private Color coloreOmbra    = new Color(0, 0, 0, 120);
    private Color coloreDisab    = new Color(50, 50, 50);

    // ── Costruttori ───────────────────────────────────────────────────────────

    public MenuButton(String label, int x, int y, int w, int h) {
        this.label  = label;
        this.bounds = new Rectangle(x, y, w, h);
    }

    /** Costruttore con colore hover personalizzato */
    public MenuButton(String label, int x, int y, int w, int h,
                      Color coloreHover, Color coloreBordoHover) {
        this(label, x, y, w, h);
        this.coloreHover    = coloreHover;
        this.coloreBordoHov = coloreBordoHover;
    }

    // ── Personalizzazione colori ──────────────────────────────────────────────

    public MenuButton setColori(Color normale, Color hover, Color bordoNorm, Color bordoHov) {
        this.coloreNormale  = normale;
        this.coloreHover    = hover;
        this.coloreBordo    = bordoNorm;
        this.coloreBordoHov = bordoHov;
        return this; // fluent
    }

    public MenuButton setColoreTesto(Color c)  { this.coloreTesto = c; return this; }

    // ── Update hover ──────────────────────────────────────────────────────────

    /**
     * Aggiorna lo stato hover in base alla posizione corrente del mouse (coordinate logiche).
     * Da chiamare in mouseMoved/mouseDragged.
     */
    public void aggiornaMouse(int mouseX, int mouseY) {
        hover = !disabilitato && bounds.contains(mouseX, mouseY);
    }

    // ── Controllo click ───────────────────────────────────────────────────────

    public boolean contains(Point p) {
        return !disabilitato && bounds.contains(p);
    }

    public boolean contains(int x, int y) {
        return !disabilitato && bounds.contains(x, y);
    }

    // ── Disegno ───────────────────────────────────────────────────────────────

    public void draw(Graphics2D g2) {
        int x = bounds.x, y = bounds.y, w = bounds.width, h = bounds.height;
        int raggio = 8; // arrotondamento angoli

        // Ombra (offset di 4px in basso-destra)
        g2.setColor(coloreOmbra);
        g2.fillRoundRect(x + 4, y + 4, w, h, raggio, raggio);

        // Sfondo
        if (disabilitato) {
            g2.setColor(coloreDisab);
        } else {
            g2.setColor(hover ? coloreHover : coloreNormale);
        }
        g2.fillRoundRect(x, y, w, h, raggio, raggio);

        // Bordo
        g2.setColor(disabilitato ? Color.DARK_GRAY : (hover ? coloreBordoHov : coloreBordo));
        g2.setStroke(new BasicStroke(hover ? 2.5f : 1.5f));
        g2.drawRoundRect(x, y, w, h, raggio, raggio);
        g2.setStroke(new BasicStroke(1f)); // reset

        // Evidenziazione superiore (effetto luce)
        if (!disabilitato) {
            g2.setColor(new Color(255, 255, 255, hover ? 30 : 15));
            g2.fillRoundRect(x + 2, y + 2, w - 4, h / 2, raggio, raggio);
        }

        // Testo centrato — font proporzionale all'altezza del bottone
        int btnFs = Math.max(10, (int)(h * 0.42));
        g2.setFont(new Font("Consolas", Font.BOLD, btnFs));
        FontMetrics fm = g2.getFontMetrics();
        int tx = x + (w - fm.stringWidth(label)) / 2;
        int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2;

        // Ombra testo
        g2.setColor(new Color(0, 0, 0, 100));
        g2.drawString(label, tx + 1, ty + 1);

        // Testo principale
        g2.setColor(disabilitato ? Color.GRAY : coloreTesto);
        g2.drawString(label, tx, ty);
    }

    // ── Utility posizione ─────────────────────────────────────────────────────

    /** Sposta il bottone a una nuova posizione x,y */
    public void setPosizione(int x, int y) {
        bounds.setLocation(x, y);
    }

    /** Centra orizzontalmente rispetto a una larghezza data */
    public void centraSu(int larghezzaContenitore) {
        bounds.x = (larghezzaContenitore - bounds.width) / 2;
    }
}
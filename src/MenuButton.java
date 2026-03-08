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
    public String          label;
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

    // ── Font personalizzabile ─────────────────────────────────────────────────
    private java.awt.Font fontCustom = null;
    public MenuButton setFont(java.awt.Font f) { this.fontCustom = f; return this; }

    // ── Icona/bandiera opzionale ──────────────────────────────────────────────
    private java.awt.image.BufferedImage imgIcona = null;
    public MenuButton setIcona(java.awt.image.BufferedImage img) { this.imgIcona = img; return this; }


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

        // ── Ombra offset ──────────────────────────────────────────────────────
        g2.setColor(new Color(0, 0, 0, hover ? 140 : 100));
        g2.fillRoundRect(x + 4, y + 5, w, h, 6, 6);

        // ── Sfondo con stile 3D pixel-art ────────────────────────────────────
        Color bg = disabilitato ? coloreDisab : (hover ? coloreHover : coloreNormale);
        g2.setColor(bg);
        g2.fillRoundRect(x, y, w, h, 6, 6);

        // Top highlight (bordo superiore chiaro — effetto pressione)
        if (!disabilitato) {
            Color hlTop = new Color(
                    Math.min(255, bg.getRed()   + 55),
                    Math.min(255, bg.getGreen() + 38),
                    Math.min(255, bg.getBlue()  + 22), 200);
            g2.setColor(hlTop);
            g2.fillRoundRect(x, y, w, 3, 3, 3);
            g2.fillRoundRect(x, y, 3, h, 3, 3);
            // Bottom-right ombra
            Color shBot = new Color(
                    Math.max(0, bg.getRed()   - 30),
                    Math.max(0, bg.getGreen() - 22),
                    Math.max(0, bg.getBlue()  - 10), 200);
            g2.setColor(shBot);
            g2.fillRoundRect(x + w - 3, y, 3, h, 3, 3);
            g2.fillRoundRect(x, y + h - 3, w, 3, 3, 3);
        }

        // ── Bordo pixel-art a due strati ──────────────────────────────────────
        Color brd = disabilitato ? Color.DARK_GRAY : (hover ? coloreBordoHov : coloreBordo);
        g2.setStroke(new BasicStroke(hover ? 2f : 1.5f));
        g2.setColor(new Color(0, 0, 0, 80));
        g2.drawRoundRect(x + 1, y + 1, w - 1, h - 1, 6, 6);
        g2.setColor(brd);
        g2.drawRoundRect(x, y, w, h, 6, 6);
        g2.setStroke(new BasicStroke(1f));

        // ── Hover: glow animato ────────────────────────────────────────────────
        if (hover && !disabilitato) {
            long ms = System.currentTimeMillis();
            float gp = 0.5f + 0.5f * (float) Math.sin(ms * 0.006);
            g2.setColor(new Color(
                    coloreBordoHov.getRed(), coloreBordoHov.getGreen(), coloreBordoHov.getBlue(),
                    Math.max(0, (int)(45 * gp))));
            g2.setStroke(new BasicStroke(3f));
            g2.drawRoundRect(x - 1, y - 1, w + 2, h + 2, 8, 8);
            g2.setStroke(new BasicStroke(1f));
            // Highlight inset
            g2.setColor(new Color(255, 255, 255, (int)(22 * gp)));
            g2.fillRoundRect(x + 3, y + 3, w - 6, h / 3, 4, 4);
        }

        // ── Testo + icona centrati ────────────────────────────────────────────
        int btnFs = Math.max(10, (int)(h * 0.42));
        if (fontCustom != null)
            g2.setFont(fontCustom.deriveFont(Font.PLAIN, (float)btnFs));
        else
            g2.setFont(new Font("Consolas", Font.BOLD, btnFs));
        FontMetrics fm = g2.getFontMetrics();

        int flagW = 0, flagH = 0, flagGap = 0;
        if (imgIcona != null) {
            flagH   = (int)(h * 0.55f);
            flagW   = (int)(flagH * ((float)imgIcona.getWidth() / imgIcona.getHeight()));
            flagGap = 6;
        }
        int totalW = flagW + flagGap + fm.stringWidth(label);
        int startX = x + (w - totalW) / 2;
        int ty = y + (h + fm.getAscent() - fm.getDescent()) / 2;

        // Disegna bandiera
        if (imgIcona != null) {
            int fy = y + (h - flagH) / 2;
            g2.drawImage(imgIcona, startX, fy, flagW, flagH, null);
            startX += flagW + flagGap;
        }

        // Ombra testo
        g2.setColor(new Color(0, 0, 0, hover ? 130 : 90));
        g2.drawString(label, startX + 1, ty + 1);
        // Testo principale
        g2.setColor(disabilitato ? Color.GRAY : coloreTesto);
        g2.drawString(label, startX, ty);
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
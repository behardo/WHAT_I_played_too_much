package render;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * BitmapFont.java
 * Font bitmap caricato da PNG sprite sheet.
 *
 * FORMATO PNG ATTESO:
 *   - Griglia di caratteri a larghezza FISSA, una riga
 *   - Ordine ASCII da spazio (32) a tilde (126), totale 95 caratteri
 *   - Esempio: 950×16px con caratteri 10×16px ciascuno
 *   - Nome file: /font.png
 *
 * Se il PNG non viene trovato, BitmapFont.isDisponibile() ritorna false
 * e il RenderEngine continuerà a usare i Font Java normali come fallback.
 *
 * USO:
 *   bitmapFont.disegna(g2, "CIAO", x, y, scala);
 *   bitmapFont.disegnaCentrato(g2, "CIAO", centroX, y, scala);
 */
public class BitmapFont {

    // Caratteri supportati: da ASCII 32 (spazio) a 126 (~)
    private static final int CHAR_START = 32;
    private static final int CHAR_END   = 126;
    private static final int NUM_CHARS  = CHAR_END - CHAR_START + 1; // 95

    private final BufferedImage sheet;
    private final int charW;   // larghezza singolo carattere nel PNG
    private final int charH;   // altezza singolo carattere nel PNG
    private final boolean disponibile;

    public BitmapFont(BufferedImage sheetImg) {
        if (sheetImg != null) {
            this.sheet  = sheetImg;
            this.charW  = sheetImg.getWidth() / NUM_CHARS;
            this.charH  = sheetImg.getHeight();
            this.disponibile = (charW > 0);
        } else {
            this.sheet  = null;
            this.charW  = 8;
            this.charH  = 8;
            this.disponibile = false;
        }
    }

    /** True se il font PNG è stato caricato correttamente. */
    public boolean isDisponibile() { return disponibile; }

    public int getCharW() { return charW; }
    public int getCharH() { return charH; }

    /**
     * Larghezza in pixel di una stringa al 100% (scala 1.0).
     * Usa scala intera per evitare sfocature.
     */
    public int larghezza(String testo, float scala) {
        return (int)(testo.length() * charW * scala);
    }

    /**
     * Disegna il testo alla posizione (x, y) = angolo top-left.
     * @param scala  moltiplicatore dimensione (1.0 = nativo, 2.0 = doppio, ecc.)
     */
    public void disegna(Graphics2D g2, String testo, int x, int y, float scala) {
        if (!disponibile || testo == null) return;
        int sw = (int)(charW * scala);
        int sh = (int)(charH * scala);
        for (int i = 0; i < testo.length(); i++) {
            int c = testo.charAt(i);
            if (c < CHAR_START || c > CHAR_END) {
                x += sw; // Avanza anche per caratteri non supportati
                continue;
            }
            int col = c - CHAR_START;
            int srcX = col * charW;
            g2.drawImage(sheet,
                x, y, x + sw, y + sh,          // dst
                srcX, 0, srcX + charW, charH,   // src
                null);
            x += sw;
        }
    }

    /**
     * Disegna il testo centrato orizzontalmente attorno a centroX.
     */
    public void disegnaCentrato(Graphics2D g2, String testo, int centroX, int y, float scala) {
        if (!disponibile || testo == null) return;
        int totalW = larghezza(testo, scala);
        disegna(g2, testo, centroX - totalW / 2, y, scala);
    }

    /**
     * Disegna il testo allineato a destra rispetto a destX.
     */
    public void disegnaDestra(Graphics2D g2, String testo, int destX, int y, float scala) {
        if (!disponibile || testo == null) return;
        int totalW = larghezza(testo, scala);
        disegna(g2, testo, destX - totalW, y, scala);
    }

    /**
     * Scala suggerita per ottenere un'altezza target in pixel.
     * Es: scalaPer(24) → 1.5 se charH=16
     */
    public float scalaPer(int altezzaTarget) {
        if (charH == 0) return 1f;
        return (float) altezzaTarget / charH;
    }
}

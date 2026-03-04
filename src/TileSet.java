import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * TileSet.java
 * Contiene il tema visivo di muri e pavimento per ogni mondo.
 *
 * Ogni mondo ha:
 *  - Nome (es. "IL CANTIERE", "LE FOGNE")
 *  - Colore di sfondo fallback per muri e pavimento (se le immagini non ci sono)
 *  - Immagini muro e pavimento (prese da ResourceLoader)
 *  - Colore tematico per UI (nome mondo, boss bar, ecc.)
 *
 * L'istanza corrente si ottiene con TileSet.perMondo(mondoAttuale, res).
 */
public class TileSet {

    // ── Dati del tema ─────────────────────────────────────────────────────────
    public final String        nomeMondo;
    public final BufferedImage imgMuro;
    public final BufferedImage imgPavimento;
    public final Color         coloreTemaMuro;
    public final Color         coloreTemaFondo;
    public final Color         coloreTemaUI;

    // ── Costruttore ───────────────────────────────────────────────────────────
    public TileSet(String nomeMondo,
                   BufferedImage imgMuro, BufferedImage imgPavimento,
                   Color coloreTemaMuro, Color coloreTemaFondo, Color coloreTemaUI) {
        this.nomeMondo      = nomeMondo;
        this.imgMuro        = imgMuro;
        this.imgPavimento   = imgPavimento;
        this.coloreTemaMuro = coloreTemaMuro;
        this.coloreTemaFondo = coloreTemaFondo;
        this.coloreTemaUI   = coloreTemaUI;
    }

    // ── Factory per mondo ─────────────────────────────────────────────────────

    /**
     * Restituisce il TileSet corretto per il mondo attuale.
     * I mondi dispari usano il tema 1, pari il tema 2.
     * In modalità infinita oltre il mondo 4 i temi si alternano.
     */
    public static TileSet perMondo(int mondo, ResourceLoader res) {
        // Mondo 0 = stanza Casa speciale
        if (mondo == 0) return new TileSet(
                "CASA",
                res.imgMuroCasa      != null ? res.imgMuroCasa      : res.imgMuroMondo1,
                res.imgPavimentoCasa != null ? res.imgPavimentoCasa : res.imgPavimentoMondo1,
                new Color(120, 90, 60),
                new Color(180, 140, 100),
                new Color(255, 220, 130)
        );

        // Normalizza al ciclo di 4 mondi per infinita
        int temaIndex = ((mondo - 1) % 4) + 1;

        return switch (temaIndex) {
            case 1 -> new TileSet(
                    "IL CANTIERE",
                    res.imgMuroMondo1, res.imgPavimentoMondo1,
                    new Color(60,  45,  30),
                    new Color(100, 85,  60),
                    new Color(220, 170, 80)
            );
            case 2 -> new TileSet(
                    "LE FOGNE",
                    res.imgMuroMondo2, res.imgPavimentoMondo2,
                    new Color(30,  50,  70),
                    new Color(50,  70,  90),
                    new Color(80,  160, 220)
            );
            case 3 -> new TileSet(
                    "LA FORNACE",
                    res.imgMuroMondo3, res.imgPavimentoMondo3,
                    new Color(80,  30,  20),
                    new Color(60,  20,  10),
                    new Color(255, 80,  30)
            );
            case 4 -> new TileSet(
                    "IL CASTELLO",
                    res.imgMuroMondo4, res.imgPavimentoMondo4,
                    new Color(40,  40,  60),
                    new Color(20,  20,  35),
                    new Color(180, 80,  220)
            );
            default -> new TileSet(
                    "L'ABISSO",
                    null, null,
                    new Color(15, 15, 25),
                    new Color(25, 25, 40),
                    new Color(150, 150, 200)
            );
        };
    }

    // ── Utilità disegno ───────────────────────────────────────────────────────

    /**
     * True se il tile è muro (bordo esterno).
     */
    public static boolean isMuro(int col, int rig) {
        return col < GameState.OFFSET
                || col >= GameState.COL_GIOCO + GameState.OFFSET
                || rig < GameState.OFFSET
                || rig >= GameState.RIG_GIOCO + GameState.OFFSET;
    }
}
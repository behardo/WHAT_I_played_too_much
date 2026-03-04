import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * DialogoNarrazione.java
 *
 * Dialogo JRPG multi-pagina generico.
 * Ogni "pagina" ha:
 *  - Un nome parlante  (es. "MARIO", "BOSS 1")
 *  - Il testo
 *  - Un'immagine sprite opzionale (null = nessuna)
 *  - Un flag isLeft: true = sprite+nome a sinistra (giocatore),
 *                    false = sprite+nome a destra (boss/shopkeeper)
 *
 * Uso:
 *   DialogoNarrazione d = new DialogoNarrazione();
 *   d.aggiungi("MARIO", sprite, "Ciao!", true);
 *   d.aggiungi("BOSS",  bossImg, "Muori!", false);
 *   d.avvia();
 *   // ogni frame: if (d.isAttivo()) { ... }
 *   // su INVIO:   d.avanza();  → true se completato
 */
public class DialogoNarrazione {

    public static class Pagina {
        public final String        nome;
        public final BufferedImage sprite;
        public final String        testo;
        public final boolean       isLeft; // true = sinistra (protagonista)

        public Pagina(String nome, BufferedImage sprite, String testo, boolean isLeft) {
            this.nome   = nome;
            this.sprite = sprite;
            this.testo  = testo;
            this.isLeft = isLeft;
        }
    }

    private final List<Pagina> pagine  = new ArrayList<>();
    private int                indice  = 0;
    private boolean            attivo  = false;

    // ── Costruzione ───────────────────────────────────────────────────────────

    public void aggiungi(String nome, BufferedImage sprite, String testo, boolean isLeft) {
        pagine.add(new Pagina(nome, sprite, testo, isLeft));
    }

    public void pulisci() {
        pagine.clear();
        indice = 0;
        attivo = false;
    }

    public void avvia() {
        if (pagine.isEmpty()) return;
        indice = 0;
        attivo = true;
    }

    // ── Stato ─────────────────────────────────────────────────────────────────

    public boolean isAttivo()  { return attivo; }
    public Pagina  getPagina() { return attivo && indice < pagine.size() ? pagine.get(indice) : null; }
    public int     getIndice() { return indice; }
    public int     getTotale() { return pagine.size(); }

    /**
     * Avanza alla pagina successiva.
     * @return true se il dialogo è completato (ultima pagina superata)
     */
    public boolean avanza() {
        if (!attivo) return true;
        indice++;
        if (indice >= pagine.size()) {
            attivo = false;
            return true;
        }
        return false;
    }
}

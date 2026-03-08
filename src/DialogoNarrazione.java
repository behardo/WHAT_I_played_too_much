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
        public final String        nomeKey;   // chiave Lang.t() oppure nome diretto
        public final BufferedImage sprite;
        public final String        testoKey;  // chiave Lang.t() oppure testo diretto
        public final boolean       isLeft;
        private final boolean      usaLang;   // true = passa per Lang.t()

        public Pagina(String nomeKey, BufferedImage sprite, String testoKey,
                      boolean isLeft, boolean usaLang) {
            this.nomeKey  = nomeKey;
            this.sprite   = sprite;
            this.testoKey = testoKey;
            this.isLeft   = isLeft;
            this.usaLang  = usaLang;
        }
        /** Nome parlante, nella lingua corrente se usaLang=true */
        public String getNome()  { return usaLang ? Lang.t(nomeKey)  : nomeKey;  }
        /** Testo del dialogo, nella lingua corrente se usaLang=true */
        public String getTesto() { return usaLang ? Lang.t(testoKey) : testoKey; }

    }

    private final List<Pagina> pagine  = new ArrayList<>();
    private int                indice  = 0;
    private boolean            attivo  = false;

    // ── Costruzione ───────────────────────────────────────────────────────────

    /** Aggiunge una pagina con testo diretto (non tradotto dinamicamente). */
    public void aggiungi(String nome, BufferedImage sprite, String testo, boolean isLeft) {
        pagine.add(new Pagina(nome, sprite, testo, isLeft, false));
    }
    /** Aggiunge una pagina con chiavi Lang.t() — tradotta dinamicamente al display. */
    public void aggiungiKey(String nomeKey, BufferedImage sprite, String testoKey, boolean isLeft) {
        pagine.add(new Pagina(nomeKey, sprite, testoKey, isLeft, true));
    }
    /**
     * Aggiunge una pagina dove il nome è una stringa diretta (es. variabile nomePg)
     * ma il testo è una chiave Lang.t() — risolta dinamicamente al display.
     */
    public void aggiungiPgKey(String nomeDirecto, BufferedImage sprite, String testoKey, boolean isLeft) {
        // Usiamo una Pagina specializzata: nomeKey=nome diretto, testoKey=chiave
        pagine.add(new Pagina(nomeDirecto, sprite, testoKey, isLeft, false) {
            @Override public String getTesto() { return Lang.t(testoKey); }
        });
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
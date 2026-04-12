package data;

/**
 * SistemaPersonaggi.java
 * Gestisce lo sblocco dei personaggi tramite progressione e il personaggio segreto.
 *
 * Regole sblocco:
 *  - BELLGERD (0): sempre sbloccato
 *  - VLAD      (1): si sblocca battendo il mondo 1
 *  - PAUL      (2): si sblocca battendo il mondo 2
 *  - JUICY     (3): si sblocca battendo il mondo 3
 *  - ???       (4): personaggio segreto, si attiva premendo B 5 volte nella selezione
 *
 * Lo sblocco si resetta ogni sessione (no salvataggio su file).
 *
 * Il personaggio segreto (DEBUG) ha statistiche volutamente assurde:
 *  - Vita:      99
 *  - Velocità:  12.0
 *  - Danno:     25
 */
public class SistemaPersonaggi {

    // Quanti personaggi base (escluso segreto)
    public static final int NUM_BASE     = 4;
    public static final int INDICE_SEGRETO = 4;

    // ── Stato sblocco ─────────────────────────────────────────────────────────
    /** mondiSconfitti[i] = true se il boss del mondo i+1 è stato battuto in questa sessione */
    private final boolean[] mondiSconfitti = new boolean[4];

    /** True se il personaggio segreto è stato attivato con la combo B×5 */
    private boolean segretoAttivo = false;

    // ── Combo B×5 ─────────────────────────────────────────────────────────────
    private int contatoreBCombo = 0;
    private static final int B_RICHIESTE = 5;

    // ── Sblocco tramite progressione ──────────────────────────────────────────

    /**
     * Registra che il boss del mondo dato è stato sconfitto.
     * Chiamato da RoomManager.avanzaAlMondoSuccessivo().
     */
    public void registraBossSconfitto(int mondo) {
        if (mondo >= 1 && mondo <= 4) {
            mondiSconfitti[mondo - 1] = true;
        }
    }

    /**
     * True se il personaggio all'indice dato è sbloccato.
     *   0 → sempre
     *   1 → dopo boss mondo 1
     *   2 → dopo boss mondo 2
     *   3 → dopo boss mondo 3
     *   4 → dopo combo B×5
     */
    public boolean isSbloccato(int indice) {
        return switch (indice) {
            case 0 -> true;
            case 1 -> mondiSconfitti[0];
            case 2 -> mondiSconfitti[1];
            case 3 -> mondiSconfitti[2];
            case 4 -> segretoAttivo;
            default -> false;
        };
    }

    /**
     * Testo mostrato sulla card quando il PG è bloccato.
     */
    public String testoSblocco(int indice) {
        return switch (indice) {
            case 1 -> Lang.t("pg.sblocco.1");
            case 2 -> Lang.t("pg.sblocco.2");
            case 3 -> Lang.t("pg.sblocco.3");
            case 4 -> Lang.t("pg.sblocco.4");
            default -> "";
        };
    }

    // ── Combo segreto ─────────────────────────────────────────────────────────

    /**
     * Registra una pressione del tasto B nella schermata di selezione PG.
     * Resetta il contatore se si preme altro.
     * @return true se la combo è completata
     */
    public boolean registraPressione(boolean isB) {
        if (isB) {
            contatoreBCombo++;
            if (contatoreBCombo >= B_RICHIESTE) {
                contatoreBCombo = 0;
                segretoAttivo   = true;
                return true;
            }
        } else {
            contatoreBCombo = 0;
        }
        return false;
    }

    public int  getContatoreBCombo() { return contatoreBCombo; }
    public boolean isSegretoAttivo() { return segretoAttivo; }

    /**
     * Quanti personaggi totali sono disponibili in questo momento.
     * Include il segreto solo se attivato.
     */
    public int numPersonaggiDisponibili() {
        return segretoAttivo ? NUM_BASE + 1 : NUM_BASE;
    }

    /**
     * Reset sessione (non resetta i mondi sconfitti — quelli persistono nella sessione).
     * Resetta solo il contatore combo.
     */
    public void resetCombo() {
        contatoreBCombo = 0;
    }

    /**
     * Reset parziale (torna al menu): mantiene i mondi sconfitti (sblocchi personaggi),
     * resetta solo il contatore combo e il segreto.
     */
    public void resetSoloCombo() {
        contatoreBCombo = 0;
        segretoAttivo   = false;
    }

    /**
     * Reset completo (nuova partita): azzera tutto inclusi gli sblocchi.
     */

    /**
     * Sblocca Ditto direttamente (es. dopo decodifica nota terminale).
     */
    public void sblocaDitto() {
        segretoAttivo   = true;
        contatoreBCombo = 0;
    }

    public void resetCompleto() {
        contatoreBCombo = 0;
        segretoAttivo   = false;
        for (int i = 0; i < mondiSconfitti.length; i++) mondiSconfitti[i] = false;
    }
}
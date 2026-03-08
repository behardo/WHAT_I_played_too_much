import java.awt.image.BufferedImage;

/**
 * DatiPersonaggio.java
 * Record dati per ogni personaggio selezionabile.
 */
public class DatiPersonaggio {

    public final String        nome;
    public final int           vitaMax;
    public final float         velocitaBase;
    public final int           dannoBase;
    public final BufferedImage imgIcona;
    public final BufferedImage imgGioco;
    public final String        descrizioneKey; // chiave Lang.t() per la descrizione
    /** Ritorna la descrizione nella lingua corrente. */
    public String descrizione() { return Lang.t(descrizioneKey); }
    /** Compatibilità — alias di descrizione() */
    public String getDescrizione() { return Lang.t(descrizioneKey); }
    public final BufferedImage imgProiettile; // Proiettile specifico del personaggio

    public DatiPersonaggio(String nome, int vitaMax, float velocitaBase, int dannoBase,
                           BufferedImage imgIcona, BufferedImage imgGioco, String descrizioneKey,
                           BufferedImage imgProiettile) {
        this.nome          = nome;
        this.vitaMax       = vitaMax;
        this.velocitaBase  = velocitaBase;
        this.dannoBase     = dannoBase;
        this.imgIcona      = imgIcona;
        this.imgGioco      = imgGioco;
        this.descrizioneKey = descrizioneKey;
        this.imgProiettile = imgProiettile;
    }
}
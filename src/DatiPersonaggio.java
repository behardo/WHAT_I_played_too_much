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
    public final String        descrizione;
    public final BufferedImage imgProiettile; // Proiettile specifico del personaggio

    public DatiPersonaggio(String nome, int vitaMax, float velocitaBase, int dannoBase,
                           BufferedImage imgIcona, BufferedImage imgGioco, String descrizione,
                           BufferedImage imgProiettile) {
        this.nome          = nome;
        this.vitaMax       = vitaMax;
        this.velocitaBase  = velocitaBase;
        this.dannoBase     = dannoBase;
        this.imgIcona      = imgIcona;
        this.imgGioco      = imgGioco;
        this.descrizione   = descrizione;
        this.imgProiettile = imgProiettile;
    }
}
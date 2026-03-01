import java.awt.image.BufferedImage;

/**
 * DatiPersonaggio.java
 * Record dati per ogni personaggio selezionabile.
 * Contiene statistiche base e riferimenti alle immagini.
 */
public class DatiPersonaggio {

    public final String       nome;
    public final int          vitaMax;
    public final float        velocitaBase;
    public final int          dannoBase;
    public final BufferedImage imgIcona;   // Usata nel menu selezione PG
    public final BufferedImage imgGioco;   // Usata durante il gioco
    public final String       descrizione;

    public DatiPersonaggio(String nome, int vitaMax, float velocitaBase, int dannoBase,
                           BufferedImage imgIcona, BufferedImage imgGioco, String descrizione) {
        this.nome          = nome;
        this.vitaMax       = vitaMax;
        this.velocitaBase  = velocitaBase;
        this.dannoBase     = dannoBase;
        this.imgIcona      = imgIcona;
        this.imgGioco      = imgGioco;
        this.descrizione   = descrizione;
    }
}
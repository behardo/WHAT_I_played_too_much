import java.awt.image.BufferedImage;

// --- SELEZIONE PERSONAGGIO: Classe per memorizzare le statistiche ---
public class DatiPersonaggio {
    public String nome;
    public int vitaMax;
    public float velocitaBase;
    public int dannoBase;
    public BufferedImage imgIcona; // Immagine per il menu
    public BufferedImage imgGioco; // Immagine usata nel livello
    public String descrizione;

    public DatiPersonaggio(String nome, int vitaMax, float velocitaBase, int dannoBase,
                           BufferedImage imgIcona, BufferedImage imgGioco, String descrizione) {
        this.nome = nome;
        this.vitaMax = vitaMax;
        this.velocitaBase = velocitaBase;
        this.dannoBase = dannoBase;
        this.imgIcona = imgIcona;
        this.imgGioco = imgGioco;
        this.descrizione = descrizione;
    }
}
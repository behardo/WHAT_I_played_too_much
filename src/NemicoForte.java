import java.awt.*;
import java.awt.image.BufferedImage;

// --- NUOVO NEMICO: Classe per il nemico più forte ---
public class NemicoForte extends Nemico {

    // Parametri specifici per il nemico forte
    private static final int VITA_FORTE = 8; // Più vita (quelli normali ne hanno 3 o 5)
    private static final float VELOCITA_FORTE = 2.2f; // Più veloce (normali 1.8f)
    private static final int SIZE_FORTE = 55; // Leggermente più grande

    public NemicoForte(int grigliaX, int grigliaY, int tileSize) {
        // Inizializziamo il Nemico base con i nuovi parametri
        super(grigliaX, grigliaY, tileSize, VITA_FORTE);
        this.velocita = VELOCITA_FORTE;
        this.size = SIZE_FORTE;
    }

    // Usiamo il draw base, ma il codice principale passerà l'immagine nemico2.png
}
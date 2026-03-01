import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * ResourceLoader.java
 * Centralizza il caricamento di tutte le risorse grafiche del gioco.
 * Tutte le immagini sono pubbliche e accessibili dalle altre classi.
 * Se un'immagine non viene trovata, il campo rimane null e le classi
 * di rendering disegnano un fallback a colori.
 */
public class ResourceLoader {

    // ── Personaggio ───────────────────────────────────────────────────────────
    public BufferedImage imgPersonaggioDefault;
    public BufferedImage imgPersonaggioVeloce;
    public BufferedImage imgPersonaggioForte;
    public BufferedImage imgPersonaggioTank;

    // ── Icone selezione personaggio ───────────────────────────────────────────
    public BufferedImage imgIconaP0;
    public BufferedImage imgIconaP1;
    public BufferedImage imgIconaP2;
    public BufferedImage imgIconaP3;

    // ── Icone selezione modalità ──────────────────────────────────────────────
    public BufferedImage imgIconaStoria;
    public BufferedImage imgIconaInfinita;

    // ── Ambiente ──────────────────────────────────────────────────────────────
    public BufferedImage imgPorta;
    public BufferedImage imgMuroMondo1;
    public BufferedImage imgPavimentoMondo1;
    public BufferedImage imgMuroMondo2;
    public BufferedImage imgPavimentoMondo2;
    public BufferedImage imgShopDoor;

    // ── Nemici e Boss ─────────────────────────────────────────────────────────
    public BufferedImage imgNemico;
    public BufferedImage imgNemico2;
    public BufferedImage imgBoss;
    public BufferedImage imgBossProjectile;

    // ── Oggetti e UI ──────────────────────────────────────────────────────────
    public BufferedImage imgCuore;
    public BufferedImage imgPugno;
    public BufferedImage imgCura;
    public BufferedImage imgMoneta;
    public BufferedImage imgShopkeeper;
    public BufferedImage imgItemSpeed;
    public BufferedImage imgItemDamage;

    // ── Riferimento al pannello per getResource() ─────────────────────────────
    private final Object context;

    /**
     * @param context qualsiasi oggetto della stessa classpath (es. il JPanel principale)
     */
    public ResourceLoader(Object context) {
        this.context = context;
        caricaTutto();
    }

    // ── Caricamento ───────────────────────────────────────────────────────────

    private void caricaTutto() {
        // Personaggi
        imgPersonaggioDefault  = load("/personaggio.png");
        imgPersonaggioVeloce   = load("/personaggio_veloce.png");
        imgPersonaggioForte    = load("/personaggio_forte.png");
        imgPersonaggioTank     = load("/personaggio_tank.png");

        // Icone selezione
        imgIconaP0             = load("/icona_p0.png");
        imgIconaP1             = load("/icona_p1.png");
        imgIconaP2             = load("/icona_p2.png");
        imgIconaP3             = load("/icona_p3.png");
        imgIconaStoria         = load("/icona_storia.png");
        imgIconaInfinita       = load("/icona_infinita.png");

        // Ambiente
        imgPorta               = load("/porta.png");
        imgMuroMondo1          = load("/muro.png");
        imgPavimentoMondo1     = load("/pavimento.png");
        imgMuroMondo2          = load("/muro2.png");
        imgPavimentoMondo2     = load("/pavimento2.png");
        imgShopDoor            = load("/shop_door.png");

        // Nemici
        imgNemico              = load("/nemico.png");
        imgNemico2             = load("/nemico2.png");
        imgBoss                = load("/boss.png");
        imgBossProjectile      = load("/bullet.png");

        // Oggetti / UI
        imgCuore               = load("/cuore.png");
        imgPugno               = load("/pugno.png");
        imgCura                = load("/cura.png");
        imgMoneta              = load("/coin.png");
        imgShopkeeper          = load("/shopkeeper.png");
        imgItemSpeed           = load("/item_speed.png");
        imgItemDamage          = load("/item_damage.png");
    }

    /**
     * Carica una singola immagine dal classpath.
     * @param path percorso assoluto rispetto alla root del classpath (es. "/nemico.png")
     * @return l'immagine caricata, oppure null se non trovata
     */
    public BufferedImage load(String path) {
        try {
            java.net.URL url = context.getClass().getResource(path);
            if (url != null) return ImageIO.read(url);
            System.err.println("[ResourceLoader] Immagine non trovata: " + path);
        } catch (IOException e) {
            System.err.println("[ResourceLoader] Errore caricamento: " + path + " → " + e.getMessage());
        }
        return null;
    }

    /**
     * Restituisce l'immagine di gioco corretta per il personaggio selezionato.
     * @param indice indice nel listaPersonaggi di UIManager
     */
    public BufferedImage getImgGiocatorePerIndice(int indice) {
        return switch (indice) {
            case 1  -> imgPersonaggioVeloce  != null ? imgPersonaggioVeloce  : imgPersonaggioDefault;
            case 2  -> imgPersonaggioForte   != null ? imgPersonaggioForte   : imgPersonaggioDefault;
            case 3  -> imgPersonaggioTank    != null ? imgPersonaggioTank    : imgPersonaggioDefault;
            default -> imgPersonaggioDefault;
        };
    }

    /**
     * Restituisce l'icona (menu selezione PG) per l'indice dato.
     */
    public BufferedImage getIconaPerIndice(int indice) {
        return switch (indice) {
            case 0  -> imgIconaP0 != null ? imgIconaP0 : imgPersonaggioDefault;
            case 1  -> imgIconaP1 != null ? imgIconaP1 : imgPersonaggioVeloce;
            case 2  -> imgIconaP2 != null ? imgIconaP2 : imgPersonaggioForte;
            case 3  -> imgIconaP3 != null ? imgIconaP3 : imgPersonaggioTank;
            default -> imgPersonaggioDefault;
        };
    }
}
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
    public BufferedImage imgMuroMondo3;
    public BufferedImage imgPavimentoMondo3;
    public BufferedImage imgMuroMondo4;
    public BufferedImage imgPavimentoMondo4;
    public BufferedImage imgShopDoor;

    // ── Nemici per mondo (indice 0 = mondo 1, ecc.) ───────────────────────────
    // Se l'immagine specifica non esiste, fallback al mondo 1
    public BufferedImage[] imgNemicoPerMondo  = new BufferedImage[4];
    public BufferedImage[] imgNemicoFortePerMondo = new BufferedImage[4];
    public BufferedImage[] imgBossPerMondo    = new BufferedImage[4];
    public BufferedImage   imgBossProjectile;

    // Mantenuti per compatibilità con codice esistente
    public BufferedImage imgNemico;
    public BufferedImage imgNemico2;
    public BufferedImage imgBoss;

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
        imgMuroMondo3          = caricaConFallback("/muro3.png",       imgMuroMondo1);
        imgPavimentoMondo3     = caricaConFallback("/pavimento3.png",  imgPavimentoMondo1);
        imgMuroMondo4          = caricaConFallback("/muro4.png",       imgMuroMondo2);
        imgPavimentoMondo4     = caricaConFallback("/pavimento4.png",  imgPavimentoMondo2);
        imgShopDoor            = load("/shop_door.png");

        // Nemici base (compatibilità)
        imgNemico              = load("/nemico.png");
        imgNemico2             = load("/nemico2.png");
        imgBoss                = load("/boss.png");
        imgBossProjectile      = load("/bullet.png");

        // ── Nemici per mondo specifico ────────────────────────────────────────
        // Mondo 1 — usa gli sprite base come fallback
        imgNemicoPerMondo[0]      = caricaConFallback("/nemico_m1.png",      imgNemico);
        imgNemicoFortePerMondo[0] = caricaConFallback("/nemico_forte_m1.png", imgNemico2);
        imgBossPerMondo[0]        = caricaConFallback("/boss_m1.png",         imgBoss);

        // Mondo 2 — fogne/buio
        imgNemicoPerMondo[1]      = caricaConFallback("/nemico_m2.png",      imgNemico);
        imgNemicoFortePerMondo[1] = caricaConFallback("/nemico_forte_m2.png", imgNemico2);
        imgBossPerMondo[1]        = caricaConFallback("/boss_m2.png",         imgBoss);

        // Mondo 3 — fornace/fuoco
        imgNemicoPerMondo[2]      = caricaConFallback("/nemico_m3.png",      imgNemico);
        imgNemicoFortePerMondo[2] = caricaConFallback("/nemico_forte_m3.png", imgNemico2);
        imgBossPerMondo[2]        = caricaConFallback("/boss_m3.png",         imgBoss);

        // Mondo 4 — castello
        imgNemicoPerMondo[3]      = caricaConFallback("/nemico_m4.png",      imgNemico);
        imgNemicoFortePerMondo[3] = caricaConFallback("/nemico_forte_m4.png", imgNemico2);
        imgBossPerMondo[3]        = caricaConFallback("/boss_m4.png",         imgBoss);

        // Oggetti / UI
        imgCuore               = load("/cuore.png");
        imgPugno               = load("/pugno.png");
        imgCura                = load("/cura.png");
        imgMoneta              = load("/coin.png");
        imgShopkeeper          = load("/shopkeeper.png");
        imgItemSpeed           = load("/item_speed.png");
        imgItemDamage          = load("/item_damage.png");
    }

    // ── Accessor per sprite per mondo ─────────────────────────────────────────

    /**
     * Sprite nemico normale per il mondo dato (1-4, ciclico per infinita).
     */
    public BufferedImage getNemicoSprite(int mondoAttuale) {
        int i = ((mondoAttuale - 1) % 4);
        return imgNemicoPerMondo[i] != null ? imgNemicoPerMondo[i] : imgNemico;
    }

    /**
     * Sprite nemico forte per il mondo dato.
     */
    public BufferedImage getNemicoForteSprite(int mondoAttuale) {
        int i = ((mondoAttuale - 1) % 4);
        return imgNemicoFortePerMondo[i] != null ? imgNemicoFortePerMondo[i] : imgNemico2;
    }

    /**
     * Sprite boss per il mondo dato.
     */
    public BufferedImage getBossSprite(int mondoAttuale) {
        int i = ((mondoAttuale - 1) % 4);
        return imgBossPerMondo[i] != null ? imgBossPerMondo[i] : imgBoss;
    }

    // ── Utilità ───────────────────────────────────────────────────────────────

    /** Carica un'immagine; se non esiste ritorna il fallback dato. */
    private BufferedImage caricaConFallback(String path, BufferedImage fallback) {
        BufferedImage img = load(path);
        return img != null ? img : fallback;
    }

    public BufferedImage load(String path) {
        try {
            java.net.URL url = context.getClass().getResource(path);
            if (url != null) return ImageIO.read(url);
            // Non loggare errori per gli sprite per-mondo opzionali (attesi null)
            if (!path.contains("_m1") && !path.contains("_m2")
                    && !path.contains("_m3") && !path.contains("_m4")) {
                System.err.println("[ResourceLoader] Immagine non trovata: " + path);
            }
        } catch (IOException e) {
            System.err.println("[ResourceLoader] Errore caricamento: " + path + " → " + e.getMessage());
        }
        return null;
    }

    public BufferedImage getImgGiocatorePerIndice(int indice) {
        return switch (indice) {
            case 1  -> imgPersonaggioVeloce  != null ? imgPersonaggioVeloce  : imgPersonaggioDefault;
            case 2  -> imgPersonaggioForte   != null ? imgPersonaggioForte   : imgPersonaggioDefault;
            case 3  -> imgPersonaggioTank    != null ? imgPersonaggioTank    : imgPersonaggioDefault;
            default -> imgPersonaggioDefault;
        };
    }

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
package ui;

import render.BitmapFont;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * ui.ResourceLoader.java
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
    public BufferedImage imgPersonaggioGod;
    public BufferedImage imgOstacolo;          // ostacolo.png — sovrapposto alla tile
    public BufferedImage[] imgOstacoloPerMondo = new BufferedImage[5]; // ostacolo_m1..5.png
    public BufferedImage imgBurn;              // burn.png — effetto bruciatura sul PG

    // ── Icone selezione personaggio ───────────────────────────────────────────
    public BufferedImage imgIconaP0;
    public BufferedImage imgIconaP1;
    public BufferedImage imgIconaP2;
    public BufferedImage imgIconaP3;
    public BufferedImage imgIconaP4;

    // ── Icone selezione modalità ──────────────────────────────────────────────
    public BufferedImage imgIconaStoria;
    public BufferedImage imgIconaInfinita;

    // ── Ambiente ──────────────────────────────────────────────────────────────
    public BufferedImage imgPorta;
    public BufferedImage imgMuroCasa;
    public BufferedImage imgPavimentoCasa;
    public BufferedImage imgMuroMondo1;
    public BufferedImage imgPavimentoMondo1;
    public BufferedImage imgMuroMondo2;
    public BufferedImage imgPavimentoMondo2;
    public BufferedImage imgMuroMondo3;
    public BufferedImage imgPavimentoMondo3;
    public BufferedImage imgMuroMondo4;
    public BufferedImage imgPavimentoMondo4;
    public BufferedImage imgMuroMondo5;
    public BufferedImage imgPavimentoMondo5;
    public BufferedImage imgShopDoor;

    // ── Tile effetto per mondo ────────────────────────────────────────────────
    public BufferedImage imgTileVeleno;    // mondo 2 - Fogne
    public BufferedImage imgTileGhiaccio;        // mondo 3 - Tundra (slow)
    public BufferedImage imgTileGhiaccioForte;  // mondo 3 - Tundra (freeze)
    public BufferedImage imgTileFuoco;     // mondo 4 - Fornace
    public BufferedImage imgTileCannone;   // mondo 5 - Castello

    // ── Nemici per mondo (indice 0 = mondo 1, ecc.) ───────────────────────────
    // Se l'immagine specifica non esiste, fallback al mondo 1
    public BufferedImage[] imgNemicoPerMondo  = new BufferedImage[5];
    public BufferedImage[] imgNemicoFortePerMondo = new BufferedImage[5];
    public BufferedImage[] imgBossPerMondo    = new BufferedImage[5];
    public BufferedImage   imgBossProjectile;

    // ── Proiettili per personaggio (0=BELLGERD, 1=VLAD, 2=PAUL, 3=JUICY, 4=GOD)
    public BufferedImage[] imgProiettilePerPG   = new BufferedImage[5];
    // ── Proiettili per boss (0=tipo1, 1=tipo2, 2=tipo3, 3=tipo4) ─────────────
    public BufferedImage[] imgProiettilePerBoss = new BufferedImage[5];
    // ── Sprite mercante nemico ────────────────────────────────────────────────
    public BufferedImage   imgShopkeeperNemico;
    // ── Sfondi personalizzabili ───────────────────────────────────────────────
    public BufferedImage   imgSfondoMenu;
    public BufferedImage   imgOcchioMenu;
    public BufferedImage   imgBandieraIT;   // 🇮🇹 bandiera italiana
    public BufferedImage   imgBandieraEN;   // 🇬🇧 bandiera inglese
    public BufferedImage   imgOcchioMenu2;
    public BufferedImage   imgOcchioMenuSmall;
    public BufferedImage   imgSfondoVittoria;
    public BufferedImage   imgSfondoGameOver;
    // ── Font bitmap ───────────────────────────────────────────────────────────
    public BitmapFont bitmapFont;
    public java.awt.Font   fontCustom;      // font principale UI
    public java.awt.Font   fontCustomBold;  // variante bold

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
    public BufferedImage imgNota;    // nota con codice debug in Casa
    public BufferedImage imgCapo;    // NPC Capo nell'Ufficio

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
        imgPersonaggioGod      = load("/personaggio_god.png");
        imgOstacolo            = load("/ostacolo.png");
        imgBurn                = load("/burn.png");
        imgOstacoloPerMondo[0] = caricaConFallback("/ostacolo_m1.png", imgOstacolo);
        imgOstacoloPerMondo[1] = caricaConFallback("/ostacolo_m2.png", imgOstacolo);
        imgOstacoloPerMondo[2] = caricaConFallback("/ostacolo_m3.png", imgOstacolo);
        imgOstacoloPerMondo[3] = caricaConFallback("/ostacolo_m4.png", imgOstacolo);
        imgOstacoloPerMondo[4] = caricaConFallback("/ostacolo_m5.png", imgOstacolo);

        // Icone selezione
        imgIconaP0             = load("/icona_p0.png");
        imgIconaP1             = load("/icona_p1.png");
        imgIconaP2             = load("/icona_p2.png");
        imgIconaP3             = load("/icona_p3.png");
        imgIconaP4             = load("/icona_p4.png");
        imgIconaStoria         = load("/icona_storia.png");
        imgIconaInfinita       = load("/icona_infinita.png");

        // Ambiente
        imgPorta               = load("/porta.png");
        imgMuroCasa            = caricaConFallback("/muro_casa.png",       null);
        imgPavimentoCasa       = caricaConFallback("/pavimento_casa.png",  null);
        imgMuroMondo1          = load("/muro.png");
        imgPavimentoMondo1     = load("/pavimento.png");
        imgMuroMondo2          = load("/muro2.png");
        imgPavimentoMondo2     = load("/pavimento2.png");
        imgMuroMondo3          = caricaConFallback("/muro3.png",       imgMuroMondo1);
        imgPavimentoMondo3     = caricaConFallback("/pavimento3.png",  imgPavimentoMondo1);
        imgMuroMondo4          = caricaConFallback("/muro4.png",       imgMuroMondo2);
        imgPavimentoMondo4     = caricaConFallback("/pavimento4.png",  imgPavimentoMondo2);
        imgMuroMondo5          = caricaConFallback("/muro5.png",       imgMuroMondo4);
        imgPavimentoMondo5     = caricaConFallback("/pavimento5.png",  imgPavimentoMondo4);

        // Tile effetto
        imgTileVeleno   = caricaConFallback("/tile_veleno.png",   null);
        imgTileGhiaccio      = caricaConFallback("/tile_ghiaccio.png",       null);
        imgTileGhiaccioForte = caricaConFallback("/tile_ghiaccio_forte.png", null);
        imgTileFuoco    = caricaConFallback("/tile_fuoco.png",    null);
        imgTileCannone  = caricaConFallback("/tile_cannone.png",  null);
        imgShopDoor            = load("/shop_door.png");

        // Nemici base (compatibilità)
        imgNemico              = load("/nemico.png");
        imgNemico2             = load("/nemico2.png");
        imgBoss                = load("/boss.png");
        imgBossProjectile      = load("/bullet.png");

        // ── Proiettili per personaggio ────────────────────────────────────────
        // Se non trovati, usano imgPugno come fallback
        imgProiettilePerPG[0] = caricaConFallback("/bullet_bellgerd.png", imgPugno);
        imgProiettilePerPG[1] = caricaConFallback("/bullet_vlad.png",     imgPugno);
        imgProiettilePerPG[2] = caricaConFallback("/bullet_paul.png",     imgPugno);
        imgProiettilePerPG[3] = caricaConFallback("/bullet_juicy.png",    imgPugno);
        imgProiettilePerPG[4] = caricaConFallback("/bullet_god.png",      imgPugno);

        // ── Proiettili per boss ───────────────────────────────────────────────
        imgProiettilePerBoss[0] = caricaConFallback("/bullet_boss1.png",  imgBossProjectile);
        imgProiettilePerBoss[1] = caricaConFallback("/bullet_boss2.png",  imgBossProjectile);
        imgProiettilePerBoss[2] = caricaConFallback("/bullet_boss3.png",  imgBossProjectile);
        imgProiettilePerBoss[3] = caricaConFallback("/bullet_boss4.png",  imgBossProjectile);
        imgProiettilePerBoss[4] = caricaConFallback("/bullet_boss5.png",  imgBossProjectile);

        // ── Sprite mercante nemico ────────────────────────────────────────────
        imgShopkeeperNemico = caricaConFallback("/shopkeeper_nemico.png", imgShopkeeper);

        // ── Sfondi personalizzabili ───────────────────────────────────────────
        imgSfondoMenu      = load("/sfondo_menu.png");
        imgOcchioMenu      = load("/occhio_menu.png");
        imgBandieraIT      = load("/bandiera_it.png");
        imgBandieraEN      = load("/bandiera_en.png");
        imgOcchioMenu2     = load("/occhio_menu2.png");
        imgOcchioMenuSmall = load("/occhio_menu_small.png");
        imgSfondoVittoria = load("/sfondo_vittoria.png");  // null = usa sfondo procedurale
        imgSfondoGameOver = load("/sfondo_gameover.png");  // null = usa sfondo procedurale

        // ── Font bitmap ───────────────────────────────────────────────────────
        bitmapFont = new BitmapFont(load("/font.png"));

        // Font PixelifySans — Regular per testo normale, Bold per titoli/HUD
        fontCustom     = caricaFontTTF("/PixelifySans-Regular.ttf");
        fontCustomBold = caricaFontTTF("/PixelifySans-Bold.ttf");
        // Fallback: se Bold manca usa Regular
        if (fontCustomBold == null) fontCustomBold = fontCustom;
        // Compatibilità: se Regular manca prova il vecchio PHONIXEA
        if (fontCustom == null) {
            fontCustom     = caricaFontTTF("/PHONIXEA.ttf");
            fontCustomBold = fontCustom;
        }

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

        // Mondo 4 — fornace
        imgNemicoPerMondo[3]      = caricaConFallback("/nemico_m4.png",      imgNemico);
        imgNemicoFortePerMondo[3] = caricaConFallback("/nemico_forte_m4.png", imgNemico2);
        imgBossPerMondo[3]        = caricaConFallback("/boss_m4.png",         imgBoss);

        // Mondo 5 — castello
        imgNemicoPerMondo[4]      = caricaConFallback("/nemico_m5.png",       imgNemicoPerMondo[0]);
        imgNemicoFortePerMondo[4] = caricaConFallback("/nemico_forte_m5.png", imgNemicoFortePerMondo[0]);
        imgBossPerMondo[4]        = caricaConFallback("/boss_m5.png",          imgBossPerMondo[0]);

        // Oggetti / UI
        imgCuore               = load("/cuore.png");
        imgPugno               = load("/pugno.png");
        imgCura                = load("/cura.png");
        imgNota                = load("/nota.png");
        imgCapo                = load("/capo.png");
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
        int i = ((mondoAttuale - 1) % 5);
        return imgNemicoPerMondo[i] != null ? imgNemicoPerMondo[i] : imgNemico;
    }

    public BufferedImage getNemicoForteSprite(int mondoAttuale) {
        int i = ((mondoAttuale - 1) % 5);
        return imgNemicoFortePerMondo[i] != null ? imgNemicoFortePerMondo[i] : imgNemico2;
    }

    public BufferedImage getBossSprite(int mondoAttuale) {
        int i = Math.max(0, Math.min(4, mondoAttuale - 1));
        return imgBossPerMondo[i] != null ? imgBossPerMondo[i] : imgBoss;
    }

    public BufferedImage getBossProiettile(int mondoAttuale) {
        int i = Math.max(0, Math.min(4, mondoAttuale - 1));
        return imgProiettilePerBoss[i] != null ? imgProiettilePerBoss[i] : imgBossProjectile;
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
                System.err.println("[ui.ResourceLoader] Immagine non trovata: " + path);
            }
        } catch (IOException e) {
            System.err.println("[ui.ResourceLoader] Errore caricamento: " + path + " → " + e.getMessage());
        }
        return null;
    }

    /**
     * Proiettile del personaggio per indice (0-4).
     * Fallback: imgPugno se null.
     */
    public BufferedImage getBulletPerPG(int indice) {
        if (indice >= 0 && indice < imgProiettilePerPG.length
                && imgProiettilePerPG[indice] != null)
            return imgProiettilePerPG[indice];
        return imgPugno;
    }

    /**
     * Proiettile del boss per tipo (1-4).
     * Fallback: imgBossProjectile se null.
     */
    public BufferedImage getBulletPerBoss(int tipo) {
        int i = Math.max(0, Math.min(tipo - 1, imgProiettilePerBoss.length - 1));
        if (imgProiettilePerBoss[i] != null) return imgProiettilePerBoss[i];
        return imgBossProjectile;
    }

    public BufferedImage getImgGiocatorePerIndice(int indice) {
        return switch (indice) {
            case 1  -> imgPersonaggioVeloce  != null ? imgPersonaggioVeloce  : imgPersonaggioDefault;
            case 2  -> imgPersonaggioForte   != null ? imgPersonaggioForte   : imgPersonaggioDefault;
            case 3  -> imgPersonaggioTank    != null ? imgPersonaggioTank    : imgPersonaggioDefault;
            case 4  -> imgPersonaggioGod     != null ? imgPersonaggioGod     : imgPersonaggioDefault;
            default -> imgPersonaggioDefault;
        };
    }

    public BufferedImage getIconaPerIndice(int indice) {
        return switch (indice) {
            case 0  -> imgIconaP0 != null ? imgIconaP0 : imgPersonaggioDefault;
            case 1  -> imgIconaP1 != null ? imgIconaP1 : imgPersonaggioVeloce;
            case 2  -> imgIconaP2 != null ? imgIconaP2 : imgPersonaggioForte;
            case 3  -> imgIconaP3 != null ? imgIconaP3 : imgPersonaggioTank;
            case 4  -> imgPersonaggioGod != null ? imgPersonaggioGod : imgPersonaggioDefault;
            default -> imgPersonaggioDefault;
        };
    }

    /** Carica un font TTF/OTF dalla cartella risorse. Ritorna null se non trovato. */
    private java.awt.Font caricaFontTTF(String path) {
        try (java.io.InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) return null;
            return java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, is);
        } catch (Exception e) {
            System.out.println("[ui.ResourceLoader] Font non trovato: " + path);
            return null;
        }
    }
}

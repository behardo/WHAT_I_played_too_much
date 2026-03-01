import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * GameLoop.java
 * Contiene tutta la logica di aggiornamento frame-per-frame del gioco:
 *  - Movimento giocatore e rilevamento bordi/transizione stanza
 *  - Sparo e aggiornamento pugni
 *  - Collisioni pugni/nemici
 *  - Raccolta oggetti (cure, monete, shop items)
 *  - Tick timer boss e invulnerabilità
 *
 * Viene chiamato ogni 16ms da actionPerformed() del JPanel principale.
 * Non disegna nulla: comunica con RenderEngine solo tramite dati condivisi.
 */
public class GameLoop {

    private final GameState   state;
    private final RoomManager roomMgr;

    // Lista pugni condivisa con RenderEngine
    public final List<Pugno> pugniAttivi = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────
    public GameLoop(GameState state, RoomManager roomMgr) {
        this.state   = state;
        this.roomMgr = roomMgr;
    }

    // ── Tick principale ───────────────────────────────────────────────────────

    /**
     * Aggiorna tutto il gioco di un frame.
     * Chiamato SOLO quando statoGioco == GIOCO.
     */
    public void tick() {
        state.tickTimerBoss();
        state.tickInvulnerabilita();
        aggiornaMov();
        aggiornaSparo();
        aggiornaPugni();
        aggiornaShop();
        aggiornaCollisioni();
        raccogliOggetti();
    }

    // ── Movimento e transizioni ───────────────────────────────────────────────

    private void aggiornaMov() {
        final int T = GameState.TILE_SIZE;
        final int O = GameState.OFFSET;

        float minX = O * T;
        float maxX = (O + GameState.COL_GIOCO) * T - GameState.PG_SIZE;
        float minY = O * T;
        float maxY = (O + GameState.RIG_GIOCO) * T - GameState.PG_SIZE;

        if (state.up    && state.y > minY) state.y -= state.velocita;
        if (state.down  && state.y < maxY) state.y += state.velocita;

        // Zona porta (y centrale) per transizioni orizzontali
        float centroPortaY = (GameState.RIG_TOTALI * T) / 2f;
        boolean inZonaPorta = state.y > centroPortaY - T && state.y < centroPortaY + T;

        if (state.left) {
            if (state.x > minX) {
                state.x -= state.velocita;
            } else if (inZonaPorta && state.indiceStanzaMemoria > 0) {
                roomMgr.tornaAllaStanzaPrecedente();
                pugniAttivi.clear();
            }
        }

        if (state.right) {
            if (state.x < maxX) {
                state.x += state.velocita;
            } else if (inZonaPorta) {
                gestisciTransizioneDestra(maxX);
            }
        }
    }

    private void gestisciTransizioneDestra(float maxX) {
        boolean isBossStanza = state.stanzaNelMondo == GameState.STANZA_BOSS;

        if (isBossStanza && state.bossSpawnato) {
            if (state.bossSconfitto) {
                // Avanza al mondo successivo (RoomManager decide se vittoria o no)
                roomMgr.avanzaAlMondoSuccessivo();
                pugniAttivi.clear();
            } else {
                // Boss ancora vivo: blocca il passaggio
                state.x = maxX - 10;
            }
        } else {
            // Stanza normale: entra nella stanza successiva
            roomMgr.entraNellaStanzaSuccessiva();
            pugniAttivi.clear();
        }
    }

    // ── Sparo ─────────────────────────────────────────────────────────────────

    private void aggiornaSparo() {
        if (state.cooldownSparo > 0) {
            state.cooldownSparo--;
            return;
        }

        int dirX = 0, dirY = 0;
        if (state.shootUp)    dirY = -1;
        if (state.shootDown)  dirY =  1;
        if (state.shootLeft)  dirX = -1;
        if (state.shootRight) dirX =  1;

        if (dirX != 0 || dirY != 0) {
            // ResourceLoader non è disponibile qui: il pugno usa imgPugno dal costruttore
            // Il riferimento all'immagine viene passato tramite factory o da chi costruisce il GameLoop
            BufferedImageRef imgPugno = pungnoImageRef;
            pugniAttivi.add(new Pugno(state.x, state.y, dirX, dirY,
                    imgPugno != null ? imgPugno.img : null,
                    state.dannoPugno));
            state.cooldownSparo = GameState.SPARO_DELAY;
        }
    }

    // ── Buffer immagine pugno (impostato da WhatIvePlayedTooMuch) ─────────────
    private BufferedImageRef pungnoImageRef;

    public void setPungoImage(java.awt.image.BufferedImage img) {
        this.pungnoImageRef = new BufferedImageRef(img);
    }

    // Wrapper minimale
    private static class BufferedImageRef {
        final java.awt.image.BufferedImage img;
        BufferedImageRef(java.awt.image.BufferedImage img) { this.img = img; }
    }

    // ── Aggiornamento pugni ───────────────────────────────────────────────────

    private void aggiornaPugni() {
        for (int i = 0; i < pugniAttivi.size(); i++) {
            Pugno p = pugniAttivi.get(i);
            p.update();
            if (p.daRimuovere) {
                pugniAttivi.remove(i--);
            }
        }
    }

    // ── Shop ──────────────────────────────────────────────────────────────────

    private void aggiornaShop() {
        Rectangle hbPG = new Rectangle((int) state.x, (int) state.y,
                GameState.PG_SIZE, GameState.PG_SIZE);

        // Shopkeeper battuta
        for (Shopkeeper sk : roomMgr.getShopkeepersCorrenti()) {
            if (hbPG.intersects(sk.getHitbox())) sk.attivaBattuta();
        }

        // Acquisto items
        for (ShopItem si : roomMgr.getShopItemsCorrenti()) {
            if (si.controllaAcquisto(state.x, state.y, GameState.PG_SIZE, state.monete)) {
                state.monete -= si.getCosto();
                si.setAcquistato();
                applicaEffettoItem(si.getTipo());
            }
        }
    }

    private void applicaEffettoItem(String tipo) {
        switch (tipo) {
            case "CURA"     -> { state.vite = Math.min(state.vite + 1, state.viteMaxGiocatore); }
            case "VELOCITA" -> state.velocita   += 1.5f;
            case "DANNO"    -> state.dannoPugno  += 1;
        }
    }

    // ── Collisioni ────────────────────────────────────────────────────────────

    private void aggiornaCollisioni() {
        List<Nemico> nemici = roomMgr.getNemiciCorrenti();

        // Nemici vs giocatore
        for (Nemico n : nemici) {
            n.update(state.x, state.y);

            if (n.toccaGiocatore(state.x, state.y, GameState.PG_SIZE)) {
                state.riceviDanno();
            }
            if (n instanceof Boss b) {
                if (b.controllaCollisioneProiettili(state.x, state.y, GameState.PG_SIZE)) {
                    state.riceviDanno();
                }
            }
        }

        // Pugni vs nemici
        for (int i = 0; i < pugniAttivi.size(); i++) {
            Pugno     p     = pugniAttivi.get(i);
            Rectangle hbP   = p.getHitbox();

            for (int j = 0; j < nemici.size(); j++) {
                Nemico    n   = nemici.get(j);
                if (!hbP.intersects(n.getHitbox())) continue;

                n.subisciDanno(p.getDanno());
                p.daRimuovere = true;

                if (n.isMorto()) {
                    float dropX = n.x;
                    float dropY = n.y;
                    boolean isBoss = n instanceof Boss;

                    nemici.remove(j--);

                    if (isBoss) {
                        state.bossSconfitto = true;
                    } else if (nemici.isEmpty() && state.stanzaNelMondo != 4) {
                        spawnDrop((int) dropX, (int) dropY);
                    }
                }
                break; // Un pugno colpisce un solo nemico
            }
        }
    }

    private void spawnDrop(int dropPixelX, int dropPixelY) {
        int tx = dropPixelX / GameState.TILE_SIZE;
        int ty = dropPixelY / GameState.TILE_SIZE;

        if (state.vite < state.viteMaxGiocatore) {
            roomMgr.getCureCorrenti().add(
                    new Cura(tx, ty, GameState.TILE_SIZE, curaImageRef));
        } else {
            roomMgr.getMoneteCorrenti().add(
                    new Moneta(tx, ty, GameState.TILE_SIZE, monetaImageRef));
        }
    }

    // Immagini drop (impostate da WhatIvePlayedTooMuch dopo ResourceLoader)
    private java.awt.image.BufferedImage curaImageRef;
    private java.awt.image.BufferedImage monetaImageRef;

    public void setDropImages(java.awt.image.BufferedImage cura,
                              java.awt.image.BufferedImage moneta) {
        this.curaImageRef   = cura;
        this.monetaImageRef = moneta;
    }

    // ── Raccolta oggetti ──────────────────────────────────────────────────────

    private void raccogliOggetti() {
        List<Cura>   cure   = roomMgr.getCureCorrenti();
        List<Moneta> monete = roomMgr.getMoneteCorrenti();

        for (int i = 0; i < cure.size(); i++) {
            if (cure.get(i).controllaRaccolta(state.x, state.y, GameState.PG_SIZE)) {
                state.vite = Math.min(state.vite + 1, state.viteMaxGiocatore);
                cure.remove(i--);
            }
        }
        for (int i = 0; i < monete.size(); i++) {
            Moneta m = monete.get(i);
            if (m.controllaRaccolta(state.x, state.y, GameState.PG_SIZE)) {
                state.monete += m.getValore();
                monete.remove(i--);
            }
        }
    }
}
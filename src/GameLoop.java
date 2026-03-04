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
    private UIManager ui;  // Per proiettile per PG
    private ResourceLoader res;  // Per sprite shopkeeper dialogo

    // Lista pugni condivisa con RenderEngine
    public final List<Pugno> pugniAttivi = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────
    public GameLoop(GameState state, RoomManager roomMgr) {
        this.state   = state;
        this.roomMgr = roomMgr;
    }

    public GameLoop(GameState state, RoomManager roomMgr, UIManager ui) {
        this.state   = state;
        this.roomMgr = roomMgr;
        this.ui      = ui;
        roomMgr.setPugniAttiviRef(pugniAttivi); // Per pulizia pugni al cambio stanza shop
    }

    // ── Tick principale ───────────────────────────────────────────────────────

    /**
     * Aggiorna tutto il gioco di un frame.
     * Chiamato SOLO quando statoGioco == GIOCO.
     */
    public void tick() {
        // Blocca il gameplay durante i dialoghi narrazione (boss intro / shopkeeper)
        if (state.dialogoNarrazione.isAttivo()) return;
        if (state.mostraDialogoCasa) return;

        state.tickTimerBoss();
        state.tickInvulnerabilita();
        aggiornaMov();
        aggiornaSparo();
        aggiornaPugni();
        aggiornaShop();
        aggiornaDialogoShopkeeper();
        aggiornaCollisioni();
        raccogliOggetti();
    }

    // ── Dialogo shopkeeper ────────────────────────────────────────────────────

    private void aggiornaDialogoShopkeeper() {
        if (!roomMgr.inStanzaShop) return;
        java.util.List<Shopkeeper> sks = roomMgr.getShopkeeperShop();
        if (sks.isEmpty() && roomMgr.getShopNemici().isEmpty()) return;
        if (sks.isEmpty()) return;

        Shopkeeper sk = sks.get(0);
        DialogoShopkeeper dialogo = state.dialogoShopkeeper;

        dialogo.aggiorna(state.x, state.y, sk.getX(), sk.getY());

        // Quando il dialogo base si attiva, costruiamo il DialogoNarrazione JRPG
        if (dialogo.getStato() == DialogoShopkeeper.Stato.MOSTRA_DIALOGO
                && !state.dialogoNarrazione.isAttivo()
                && !state.dialogoShopkeeperNarrazioneAvviata) {
            state.dialogoShopkeeperNarrazioneAvviata = true;
            java.awt.image.BufferedImage sprPg = res.getImgGiocatorePerIndice(state.indicePersonaggioSelezionato);
            java.awt.image.BufferedImage sprSk = shopkeeperImgRef;
            state.dialogoNarrazione.pulisci();
            state.dialogoNarrazione.aggiungi("NEGOZIANTE", sprSk,
                    "Ehi! Non toccare la merce se non hai intenzione di comprare.", false);
            state.dialogoNarrazione.aggiungi(state.nomePersonaggioCorrente(), sprPg,
                    "Rilassati, sto solo dando un'occhiata...", true);
            state.dialogoNarrazione.aggiungi("NEGOZIANTE", sprSk,
                    "Vuoi attaccarmi?!", false);
            state.dialogoNarrazione.avvia();
        }

        if (dialogo.getStato() == DialogoShopkeeper.Stato.ATTACCO) {
            float skX = sk.getX(), skY = sk.getY();
            sks.clear();
            roomMgr.getItemsShop().clear();
            state.monete += 20;
            java.awt.image.BufferedImage imgSKN = shopkeeperNemicoImgRef != null
                    ? shopkeeperNemicoImgRef : shopkeeperImgRef;
            roomMgr.getShopNemici().add(new ShopkeeperNemico(skX, skY, imgSKN));
            dialogo.consuma();
            state.dialogoNarrazione.pulisci();
            state.dialogoShopkeeperNarrazioneAvviata = false;
        } else if (dialogo.getStato() == DialogoShopkeeper.Stato.RIFIUTO) {
            dialogo.consuma();
            state.dialogoNarrazione.pulisci();
            state.dialogoShopkeeperNarrazioneAvviata = false;
        }
    }

    // Immagine shopkeeper per ShopkeeperNemico (impostata da WhatIvePlayedTooMuch)
    private java.awt.image.BufferedImage shopkeeperImgRef;
    private java.awt.image.BufferedImage shopkeeperNemicoImgRef;
    public void setResourceLoader(ResourceLoader r) { this.res = r; }

    public void setShopkeeperImage(java.awt.image.BufferedImage img) {
        this.shopkeeperImgRef = img;
    }
    public void setShopkeeperNemicoImage(java.awt.image.BufferedImage img) {
        this.shopkeeperNemicoImgRef = img;
    }

    // ── Movimento e transizioni ───────────────────────────────────────────────

    private void aggiornaMov() {
        final int T = GameState.TILE_SIZE;
        final int O = GameState.OFFSET;

        float minX = O * T;
        float maxX = (O + GameState.COL_GIOCO) * T - GameState.PG_SIZE;
        float minY = O * T;
        float maxY = (O + GameState.RIG_GIOCO) * T - GameState.PG_SIZE;

        // ── Stanza Shop Nord ──────────────────────────────────────────────────
        // Dentro la stanza shop: solo movimento libero + porta a sud per uscire
        if (roomMgr.inStanzaShop) {
            if (state.up    && state.y > minY) state.y -= state.velocita;
            if (state.down  && state.y < maxY) state.y += state.velocita;
            if (state.left  && state.x > minX) state.x -= state.velocita;
            if (state.right && state.x < maxX) state.x += state.velocita;

            // Porta sud (bordo basso): esce dallo shop
            if (state.y >= maxY && state.down) {
                roomMgr.esciDalloShop();
            }
            return; // Nessuna altra transizione dentro lo shop
        }

        // ── Movimento verticale ───────────────────────────────────────────────
        if (state.up    && state.y > minY) { state.y -= state.velocita; if (collideOstacolo()) state.y += state.velocita; }
        if (state.down  && state.y < maxY) { state.y += state.velocita; if (collideOstacolo()) state.y -= state.velocita; }

        // Porta nord shop: stanza 1 del nuovo mondo, shop sbloccato, giocatore in cima
        boolean inZonaPortaNord = state.x > (GameState.LARGHEZZA_GIOCO / 2f - GameState.TILE_SIZE)
                && state.x < (GameState.LARGHEZZA_GIOCO / 2f + GameState.TILE_SIZE);
        if (state.up && state.y <= minY
                && state.stanzaNelMondo == 1
                && state.shopSbloccato
                && inZonaPortaNord) {
            roomMgr.assicuraShopGenerato();
            roomMgr.entraNelloShop();
            return;
        }

        // ── Movimento orizzontale ─────────────────────────────────────────────
        float centroPortaY = (GameState.RIG_TOTALI * T) / 2f;
        boolean inZonaPorta = state.y > centroPortaY - T && state.y < centroPortaY + T;

        if (state.left) {
            if (state.x > minX) {
                state.x -= state.velocita;
                if (collideOstacolo()) state.x += state.velocita;
            } else if (inZonaPorta && state.indiceStanzaMemoria > 0) {
                // Bloccato se boss fight attiva (boss spawned e non sconfitto)
                boolean bossAttivo = state.stanzaNelMondo == GameState.STANZA_BOSS
                        && state.bossSpawnato && !state.bossSconfitto;
                if (!bossAttivo) {
                    roomMgr.tornaAllaStanzaPrecedente();
                    pugniAttivi.clear();
                } else {
                    state.x = minX + 2; // Spingi indietro
                }
            }
        }

        if (state.right) {
            if (state.x < maxX) {
                state.x += state.velocita;
                if (collideOstacolo()) state.x -= state.velocita;
            } else if (inZonaPorta) {
                gestisciTransizioneDestra(maxX);
            }
        }
    }

    /**
     * True se la stanza corrente è "pulita" (nessun nemico vivo).
     * La stanza boss si considera pulita solo dopo bossSconfitto.
     * La stanza 1 (ingresso) è sempre pulita.
     */
    /** True se il giocatore si sovrappone a una tile ostacolo. */
    private boolean collideOstacolo() {
        int[][] ostacoli = roomMgr.getOstacoliCorrenti();
        if (ostacoli == null) return false;
        final int T = GameState.TILE_SIZE;
        final int margin = 4; // pixel di tolleranza ai bordi
        int pgX1 = (int)state.x + margin;
        int pgY1 = (int)state.y + margin;
        int pgX2 = (int)state.x + GameState.PG_SIZE - margin;
        int pgY2 = (int)state.y + GameState.PG_SIZE - margin;
        for (int[] o : ostacoli) {
            int ox1 = o[0] * T + margin;
            int oy1 = o[1] * T + margin;
            int ox2 = (o[0]+1) * T - margin;
            int oy2 = (o[1]+1) * T - margin;
            if (pgX1 < ox2 && pgX2 > ox1 && pgY1 < oy2 && pgY2 > oy1) return true;
        }
        return false;
    }

    private boolean stanzaPulita() {
        if (state.stanzaNelMondo == 1) return true;
        return roomMgr.getNemiciCorrenti().isEmpty();
    }

    private void gestisciTransizioneDestra(float maxX) {
        boolean isBossStanza = state.stanzaNelMondo == GameState.STANZA_BOSS;

        if (isBossStanza && state.bossSpawnato) {
            if (state.bossSconfitto) {
                roomMgr.avanzaAlMondoSuccessivo();
                pugniAttivi.clear();
            } else {
                state.x = maxX - 10; // Boss ancora vivo: blocca
            }
        } else {
            // Stanza normale: passa solo se pulita
            if (stanzaPulita()) {
                roomMgr.entraNellaStanzaSuccessiva();
                pugniAttivi.clear();
            } else {
                state.x = maxX - 10; // Nemici ancora vivi: blocca
            }
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
            // Usa il proiettile specifico del personaggio selezionato
            java.awt.image.BufferedImage imgBullet = null;
            if (ui != null) {
                DatiPersonaggio pg = ui.listaPersonaggi.get(state.indicePersonaggioSelezionato);
                imgBullet = pg.imgProiettile;
            }
            if (imgBullet == null && pungnoImageRef != null) imgBullet = pungnoImageRef.img;
            pugniAttivi.add(new Pugno(state.x, state.y, dirX, dirY, imgBullet, state.dannoPugno));
            state.audio.suonaEffetto(AudioManager.SFX_SPARO);
            state.cooldownSparo = GameState.SPARO_DELAY;
        }
    }

    // ── Fallback immagine pugno (impostato da WhatIvePlayedTooMuch) ──────────
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

        // Usa le liste corrette in base a dove si trova il giocatore
        java.util.List<Shopkeeper> shopkeepers = roomMgr.inStanzaShop
                ? roomMgr.getShopkeeperShop()
                : roomMgr.getShopkeepersCorrenti();
        java.util.List<ShopItem> items = roomMgr.inStanzaShop
                ? roomMgr.getItemsShop()
                : roomMgr.getShopItemsCorrenti();

        for (Shopkeeper sk : shopkeepers) {
            if (hbPG.intersects(sk.getHitbox())) sk.attivaBattuta();
        }

        for (ShopItem si : items) {
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
        List<Nemico> nemici = roomMgr.inStanzaShop
                ? roomMgr.getShopNemici()
                : roomMgr.getNemiciCorrenti();

        // Nemici vs giocatore — update con lista per separazione
        int[][] ostacoli = roomMgr.getOstacoliCorrenti();
        for (int i = 0; i < nemici.size(); i++) {
            Nemico n = nemici.get(i);

            // Setup boss al primo frame (burn callback + pugni ref)
            if (n instanceof Boss b && !b.isSetupDone()) {
                b.setOnBurnPlayer(() -> {
                    state.burnAttivo = true;
                    state.burnTimer  = GameState.BURN_DURATA;
                    state.burnTick   = 0;
                });
                b.setPugniAttiviRef(pugniAttivi);
                b.markSetupDone();
            }

            n.update(state.x, state.y, nemici, ostacoli);

            if (n.toccaGiocatore(state.x, state.y, GameState.PG_SIZE)) {
                state.riceviDanno();
            }
            if (n instanceof Boss b) {
                BossProjectile.Tipo colpito =
                        b.controllaCollisioneProiettiliConTipo(state.x, state.y, GameState.PG_SIZE);
                if (colpito != null) {
                    state.riceviDanno();
                    // Proiettile di fuoco → applica burn
                    if (colpito == BossProjectile.Tipo.FUOCO) {
                        state.burnAttivo = true;
                        state.burnTimer  = GameState.BURN_DURATA;
                        state.burnTick   = 0;
                    }
                }
            }
        }

        // Aggiorna effetto burn
        if (state.burnAttivo) {
            state.burnTimer--;
            state.burnTick++;
            if (state.burnTick >= GameState.BURN_INTERVALLO) {
                state.burnTick = 0;
                if (state.vite > 0) state.vite -= GameState.BURN_DANNO;
            }
            if (state.burnTimer <= 0) {
                state.burnAttivo = false;
                state.burnTimer  = 0;
            }
        }

        // Tick barre vita
        for (Nemico n : nemici) n.tickBarra();

        // Pugni vs nemici
        for (int i = 0; i < pugniAttivi.size(); i++) {
            Pugno     p   = pugniAttivi.get(i);
            Rectangle hbP = p.getHitbox();

            for (int j = 0; j < nemici.size(); j++) {
                Nemico n = nemici.get(j);
                if (!hbP.intersects(n.getHitbox())) continue;

                n.subisciDanno(p.getDanno());
                p.daRimuovere = true;

                if (n.isMorto()) {
                    float dropX  = n.x;
                    float dropY  = n.y;
                    boolean isBoss = n instanceof Boss;

                    nemici.remove(j--);
                    state.audio.suonaEffetto(AudioManager.SFX_MORTE_NEMICO);

                    if (isBoss) {
                        state.bossSconfitto = true;
                        state.audio.suonaMusica(AudioManager.VITTORIA);
                    } else if (nemici.isEmpty() && !roomMgr.inStanzaShop
                            && state.stanzaNelMondo != GameState.STANZA_BOSS) {
                        spawnDrop((int) dropX, (int) dropY);
                    }
                }
                break;
            }
        }
    }

    private void spawnDrop(int dropPixelX, int dropPixelY) {
        int tx = dropPixelX / GameState.TILE_SIZE;
        int ty = dropPixelY / GameState.TILE_SIZE;

        // Sposta su una tile libera se quella di drop è un ostacolo o un muro
        int[] tile = trovaTileLibera(tx, ty);
        tx = tile[0]; ty = tile[1];

        if (state.vite < state.viteMaxGiocatore) {
            roomMgr.getCureCorrenti().add(
                    new Cura(tx, ty, GameState.TILE_SIZE, curaImageRef));
        } else {
            roomMgr.getMoneteCorrenti().add(
                    new Moneta(tx, ty, GameState.TILE_SIZE, monetaImageRef));
        }
    }

    /** Trova la tile libera più vicina a (tx,ty) — non muro, non ostacolo. */
    private int[] trovaTileLibera(int tx, int ty) {
        if (!TileSet.isMuro(tx, ty) && !roomMgr.isOstacolo(tx, ty)) return new int[]{tx, ty};
        // Cerca a spirale
        for (int r = 1; r <= 4; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    if (Math.abs(dx) != r && Math.abs(dy) != r) continue;
                    int nx = tx + dx, ny = ty + dy;
                    if (!TileSet.isMuro(nx, ny) && !roomMgr.isOstacolo(nx, ny))
                        return new int[]{nx, ny};
                }
            }
        }
        return new int[]{tx, ty}; // fallback
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
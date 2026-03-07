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

    // Proiettili sparati dai tile cannone (verso il giocatore)
    public final List<BossProjectile> proiettiliCannone = new ArrayList<>();

    // ─────────────────────────────────────────────────────────────────────────
    public GameLoop(GameState state, RoomManager roomMgr) {
        this.state   = state;
        this.roomMgr = roomMgr;
    }

    public GameLoop(GameState state, RoomManager roomMgr, UIManager ui) {
        this.state   = state;
        this.roomMgr = roomMgr;
        this.ui      = ui;
        roomMgr.setPugniAttiviRef(pugniAttivi);
        roomMgr.setProiettiliCannoneRef(proiettiliCannone);
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
        aggiornaMelee();
        aggiornaPugni();
        aggiornaShop();
        aggiornaDialogoShopkeeper();
        aggiornaTileEffetto();
        aggiornaProiettiliCannone();
        aggiornaCollisioni();
        aggiornaNota();
        aggiornaUfficio();
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

        // Freeze: blocca completamente il movimento
        if (state.freezeAttivo) return;

        // Slow: velocità ridotta
        float vel = state.velocita;
        if (state.slowAttivo) vel *= GameState.SLOW_MULT;

        // Aggiorna direzione faccia (per melee)
        if      (state.up)    { state.facingX = 0;  state.facingY = -1; }
        else if (state.down)  { state.facingX = 0;  state.facingY =  1; }
        else if (state.left)  { state.facingX = -1; state.facingY =  0; }
        else if (state.right) { state.facingX =  1; state.facingY =  0; }

        // ── Stanza Bonus (stanza ardua opzionale) ──────────────────────────────
        // Una volta dentro non si puo uscire finche non si battono tutti i nemici.
        // L'uscita avviene automaticamente dopo aver raccolto il powerup.
        if (roomMgr.inStanzaBonus) {
            if (state.up    && state.y > minY) state.y -= vel;
            if (state.down  && state.y < maxY) state.y += vel;
            if (state.left  && state.x > minX) state.x -= vel;
            if (state.right && state.x < maxX) state.x += vel;
            // Muri nord/sud invalicabili — il giocatore è bloccato
            if (state.y <= minY) state.y = minY + 2;
            if (state.y >= maxY) state.y = maxY - 2;
            return;
        }

        // ── Stanza Shop Nord ──────────────────────────────────────────────────
        if (roomMgr.inStanzaShop) {
            if (state.up    && state.y > minY) state.y -= vel;
            if (state.down  && state.y < maxY) state.y += vel;
            if (state.left  && state.x > minX) state.x -= vel;
            if (state.right && state.x < maxX) state.x += vel;

            if (state.y >= maxY && state.down) {
                roomMgr.esciDalloShop();
            }
            return;
        }

        // ── Movimento verticale ───────────────────────────────────────────────
        if (state.up    && state.y > minY) { state.y -= vel; if (collideOstacolo()) state.y += vel; }
        if (state.down  && state.y < maxY) { state.y += vel; if (collideOstacolo()) state.y -= vel; }

        // Porta sud ardua: stanza random del mondo, pulita, non ancora completata
        boolean in62Aperta = state.stanzaNelMondo == state.stanzaConPortaArdua
                && !state.ardua_completed
                && stanzaPulita()
                && inZonaCentroX();
        if (state.down && state.y >= maxY && in62Aperta) {
            roomMgr.entraInBonus();
            return;
        }

        // Porta nord shop
        boolean inZonaPortaNord = inZonaCentroX();
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
                state.x -= vel;
                if (collideOstacolo()) state.x += vel;
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
                state.x += vel;
                if (collideOstacolo()) state.x -= vel;
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
    private boolean inZonaCentroX() {
        return state.x > (GameState.LARGHEZZA_GIOCO / 2f - GameState.TILE_SIZE)
                && state.x < (GameState.LARGHEZZA_GIOCO / 2f + GameState.TILE_SIZE);
    }

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
            if (stanzaPulita()) {
                roomMgr.entraNellaStanzaSuccessiva();
                pugniAttivi.clear();
            } else {
                state.x = maxX - 10;
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
            state.cooldownSparo = Math.max(4, GameState.SPARO_DELAY - state.sparoDelayRiduzione + state.arduaMalusFireRate);
        }
    }

    // ── Nota in Casa ─────────────────────────────────────────────────────────

    private void aggiornaNota() {
        if (state.mostraNota) return; // popup aperto, aspetta input
        Nota nota = roomMgr.getNotaCasa();
        if (nota == null || nota.isRaccolta()) return;
        nota.update();
        if (nota.controllaRaccolta(state.x, state.y, GameState.PG_SIZE)) {
            nota.raccogli();
            state.notaRaccolta = true;
            state.mostraNota   = true;
        }
    }

    // ── Ufficio ───────────────────────────────────────────────────────────────

    private void aggiornaUfficio() {
        if (state.statoGioco != GameState.StatoGioco.UFFICIO) return;
        if (!state.ufficioDialogoAvviato) {
            state.ufficioDialogoAvviato = true;
            avviaDialogoCapo();
        }
        // Quando il dialogo finisce e viene chiuso → VITTORIA_STORIA
        // Gestito dall'InputHandler su INVIO nell'ultima pagina
    }

    private void avviaDialogoCapo() {
        java.awt.image.BufferedImage sprCapo = res != null ? res.imgCapo : null;
        java.awt.image.BufferedImage sprPg   = res != null
                ? res.getImgGiocatorePerIndice(state.indicePersonaggioSelezionato) : null;
        String nomePg = state.nomePersonaggioCorrente();

        state.dialogoNarrazione.pulisci();
        state.dialogoNarrazione.aggiungi("CAPO", sprCapo,
                "Finalmente sei arrivato. Ti aspettavo.", false);
        state.dialogoNarrazione.aggiungi(nomePg, sprPg,
                "Ho attraversato quattro mondi per arrivare qui.", true);
        state.dialogoNarrazione.aggiungi("CAPO", sprCapo,
                "Lo so. Ho visto tutto.", false);
        state.dialogoNarrazione.aggiungi(nomePg, sprPg,
                "Allora sai anche perche sono in ritardo.", true);
        state.dialogoNarrazione.aggiungi("CAPO", sprCapo,
                "Sai cosa mi fa piu ridere? La riunione e stata spostata a domani.", false);
        state.dialogoNarrazione.aggiungi(nomePg, sprPg,
                "...", true);
        state.dialogoNarrazione.aggiungi("CAPO", sprCapo,
                "Siediti. Hai l'aria di qualcuno che ha bisogno di un caffe.", false);
        state.dialogoNarrazione.avvia();
    }

    private void aggiornaMelee() {
        if (!state.meleeUnlocked) return;
        if (state.meleeCooldown  > 0) state.meleeCooldown--;
        if (state.meleeNomeTimer > 0) state.meleeNomeTimer--;
        if (state.arduaRicompensaTimer > 0) {
            state.arduaRicompensaTimer--;
            if (state.arduaRicompensaTimer == 0) {
                state.arduaRicompensaMsg = "";
                // Esci dalla stanza bonus quando il popup finisce (qualsiasi ricompensa)
                if (roomMgr.inStanzaBonus && state.ardua_completed) {
                    roomMgr.esciDaBonus();
                }
            }
        }

        if (state.meleeAttivo) {
            state.meleeAttivo = false;
            if (state.meleeCooldown <= 0) {
                // Centro hitbox davanti al personaggio
                int reach = getMeleeReach();
                state.meleeHitX = (int)(state.x + GameState.PG_SIZE / 2f
                        + state.facingX * reach);
                state.meleeHitY = (int)(state.y + GameState.PG_SIZE / 2f
                        + state.facingY * reach);
                state.meleeTimer    = GameState.MELEE_DURATION;
                state.meleeCooldown = GameState.MELEE_DELAY;
                state.meleeNomeTimer = GameState.MELEE_NOME_DURATA;
            }
        }
    }

    /** Danno melee per personaggio. */
    private int getMeleeDanno() {
        return switch (state.indicePersonaggioSelezionato) {
            case 0 -> 3;   // BELLGERD — Colpo di Valigia
            case 1 -> 2;   // VLAD     — Chiave Inglese
            case 2 -> 6;   // PAUL     — Accetta
            case 3 -> 4;   // JUICY    — Colpo di Gamepad
            case 4 -> 99;  // D.I.T.T.O. — Cancellazione
            default -> 3;
        };
    }

    /** Raggio hitbox melee (px). */
    private int getMeleeRaggio() {
        return switch (state.indicePersonaggioSelezionato) {
            case 0 -> 40;   // BELLGERD — Colpo di Valigia
            case 1 -> 30;   // VLAD     — Chiave Inglese
            case 2 -> 55;   // PAUL     — Accetta
            case 3 -> 50;   // JUICY    — Colpo di Gamepad
            case 4 -> 80;   // D.I.T.T.O. — Cancellazione
            default -> 40;
        };
    }

    /** Distanza del centro hitbox dal personaggio. */
    private int getMeleeReach() {
        return switch (state.indicePersonaggioSelezionato) {
            case 0 -> 55;   // BELLGERD — Colpo di Valigia
            case 1 -> 45;   // VLAD     — Chiave Inglese
            case 2 -> 50;   // PAUL     — Accetta
            case 3 -> 35;   // JUICY    — Colpo di Gamepad
            case 4 -> 60;   // D.I.T.T.O. — Cancellazione
            default -> 50;
        };
    }

    private BufferedImageRef pungnoImageRef;

    public void setPungoImage(java.awt.image.BufferedImage img) {
        this.pungnoImageRef = new BufferedImageRef(img);
    }

    // Wrapper minimale
    private static class BufferedImageRef {
        final java.awt.image.BufferedImage img;
        BufferedImageRef(java.awt.image.BufferedImage img) { this.img = img; }
    }

    // ── Tile effetto (veleno/ghiaccio/fuoco/cannone) ──────────────────────────

    private void aggiornaTileEffetto() {
        // Timer freeze/slow decrementati SEMPRE, indipendentemente dai tile presenti
        if (state.freezeAttivo) {
            state.freezeTimer--;
            if (state.freezeTimer <= 0) { state.freezeAttivo = false; state.freezeTimer = 0; }
        }
        if (state.slowAttivo) {
            state.slowTimer--;
            if (state.slowTimer <= 0) { state.slowAttivo = false; state.slowTimer = 0; }
        }

        List<TileEffetto> tiles = roomMgr.getTileEffettoCorrenti();
        if (tiles == null || tiles.isEmpty()) return;

        for (TileEffetto te : tiles) {
            te.tick();
            boolean sopra = te.toccaGiocatore(state.x, state.y, GameState.PG_SIZE, GameState.TILE_SIZE);

            // Cannone: spara a intervallo fisso INDIPENDENTEMENTE dalla posizione del giocatore
            if (te.tipo == TileEffetto.Tipo.CANNONE) {
                if (te.isReady()) lanciaProiettileCannone(te);
                continue;
            }

            if (!sopra || !te.isReady()) continue;

            switch (te.tipo) {
                case VELENO -> {
                    state.riceviDanno();
                    if (state.arduaRicompensaTimer <= 50) {
                        state.arduaRicompensaMsg   = "VELENO!";
                        state.arduaRicompensaTimer = 50;
                    }
                    te.resetCooldown();
                }
                case FUOCO -> {
                    state.riceviDanno();
                    if (!state.burnAttivo) {
                        state.burnAttivo = true;
                        state.burnTimer  = GameState.BURN_DURATA;
                        state.burnTick   = 0;
                    }
                    te.resetCooldown();
                }
                case GHIACCIO -> {
                    state.slowAttivo = true;
                    state.slowTimer  = GameState.SLOW_DURATA;
                    te.resetCooldown();
                }
                case GHIACCIO_FORTE -> {
                    if (!state.freezeAttivo) {
                        state.freezeAttivo = true;
                        state.freezeTimer  = GameState.FREEZE_DURATA;
                        if (state.arduaRicompensaTimer <= 50) {
                            state.arduaRicompensaMsg   = "CONGELATO!";
                            state.arduaRicompensaTimer = 50;
                        }
                    }
                    te.resetCooldown();
                }
                case CANNONE -> {
                    lanciaProiettileCannone(te);
                    te.resetCooldown();
                }
            }
        }
    }

    private void lanciaProiettileCannone(TileEffetto te) {
        int T = GameState.TILE_SIZE;
        // Posizione centro tile cannone (pixel)
        float sx = te.col * T + T / 2f;
        float sy = te.rig * T + T / 2f;
        // Posizione centro giocatore (pixel)
        float tx = state.x + GameState.PG_SIZE / 2f;
        float ty = state.y + GameState.PG_SIZE / 2f;
        // Spara un BossProjectile verso il giocatore
        proiettiliCannone.add(new BossProjectile(sx, sy, tx, ty,
                null, BossProjectile.Tipo.NORMAL));
        te.resetCooldown();
    }

    private void aggiornaProiettiliCannone() {
        if (proiettiliCannone.isEmpty()) return;
        int T   = GameState.TILE_SIZE;
        int pgW = GameState.PG_SIZE;
        Rectangle hbPG = new Rectangle((int)state.x, (int)state.y, pgW, pgW);
        int maxX = GameState.COL_TOTALI * T;
        int maxY = GameState.RIG_TOTALI * T;

        for (int i = proiettiliCannone.size() - 1; i >= 0; i--) {
            BossProjectile p = proiettiliCannone.get(i);
            p.update();
            // Rimuovi se fuori schermo
            if (p.x < 0 || p.x > maxX || p.y < 0 || p.y > maxY) {
                proiettiliCannone.remove(i);
                continue;
            }
            // Collisione con giocatore
            Rectangle hbP = new Rectangle((int)p.x - 8, (int)p.y - 8, 16, 16);
            if (!state.invulnerabile && hbP.intersects(hbPG)) {
                state.riceviDanno();
                if (state.arduaRicompensaTimer <= 50) {
                    state.arduaRicompensaMsg   = "CANNONE!";
                    state.arduaRicompensaTimer = 50;
                }
                proiettiliCannone.remove(i);
            }
        }

        // Pulisci i proiettili al cambio stanza
        if (roomMgr.inStanzaBonus != (proiettiliCannone.size() > 0 &&
                roomMgr.getTileEffettoCorrenti() != null &&
                roomMgr.getTileEffettoCorrenti().stream()
                        .anyMatch(te -> te.tipo == TileEffetto.Tipo.CANNONE))) {
            // non fare nulla - li pulisce aggiornaTransizioneStanza
        }
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
        java.util.List<ShopItem> items = roomMgr.inStanzaBonus
                ? roomMgr.getItemsBonus()
                : roomMgr.inStanzaShop
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
                // Esce dalla bonus room quando il popup finisce (gestito dal timer in aggiornaMisc)
            }
        }
    }

    private void applicaEffettoItem(String tipo) {
        switch (tipo) {
            case "CURA", "HP UP" -> {
                state.viteMaxGiocatore++;
                state.vite = Math.min(state.vite + 1, state.viteMaxGiocatore);
                state.arduaRicompensaMsg   = "+1 SLOT VITA!";
                state.arduaRicompensaTimer = 90;
            }
            case "VELOCITA" -> {
                state.velocita += 1.5f;
                state.arduaRicompensaMsg   = "+1.5 VELOCITA!";
                state.arduaRicompensaTimer = 90;
            }
            case "DANNO" -> {
                state.dannoPugno += 2;
                state.arduaRicompensaMsg   = "+2 DANNO!";
                state.arduaRicompensaTimer = 90;
            }
            case "FIRE RATE" -> {
                state.sparoDelayRiduzione = Math.min(state.sparoDelayRiduzione + 3, 8);
                state.arduaRicompensaMsg   = "FIRE RATE UP!";
                state.arduaRicompensaTimer = 90;
            }
        }
    }

    // ── Collisioni ────────────────────────────────────────────────────────────

    private void aggiornaCollisioni() {
        List<Nemico> nemici = roomMgr.inStanzaBonus
                ? roomMgr.getNemiciBonus()
                : roomMgr.inStanzaShop
                ? roomMgr.getShopNemici()
                : roomMgr.getNemiciCorrenti();

        // Nemici vs giocatore — update con lista per separazione
        int[][] ostacoli = roomMgr.getOstacoliCorrenti();
        for (int i = 0; i < nemici.size(); i++) {
            Nemico n = nemici.get(i);
            n.update(state.x, state.y, nemici, ostacoli);

            if (n.toccaGiocatore(state.x, state.y, GameState.PG_SIZE)) {
                state.riceviDanno();
                int mondoN = state.mondoAttuale;
                if (n instanceof NemicoForte) {
                    // Mondo 3 (Fornace) → burn a contatto
                    if (mondoN == 3 && !state.burnAttivo) {
                        state.burnAttivo = true;
                        state.burnTimer  = GameState.BURN_DURATA;
                        state.burnTick   = 0;
                    }
                    // Mondo 4 (Ghiacciaio): lo slow viene dai tile, non dal contatto nemico
                }
            }
            if (n instanceof Boss b) {
                BossProjectile.Tipo colpito =
                        b.controllaCollisioneProiettiliConTipo(state.x, state.y, GameState.PG_SIZE);
                if (colpito != null) {
                    state.riceviDanno();
                    if (colpito == BossProjectile.Tipo.FUOCO) {
                        // Boss tipo 4 (Ghiacciaio) → freeze; boss tipo 3 (Fornace) → burn
                        if (b.getTipo() == 4) {
                            if (!state.freezeAttivo) {
                                state.freezeAttivo = true;
                                state.freezeTimer  = GameState.FREEZE_DURATA;
                                state.arduaRicompensaMsg   = "CONGELATO!";
                                state.arduaRicompensaTimer = 60;
                            }
                        } else {
                            state.burnAttivo = true;
                            state.burnTimer  = GameState.BURN_DURATA;
                            state.burnTick   = 0;
                        }
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
                    boolean isShopkeeper = n instanceof ShopkeeperNemico;

                    nemici.remove(j--);
                    state.audio.suonaEffetto(AudioManager.SFX_MORTE_NEMICO);

                    if (isBoss) {
                        state.bossSconfitto = true;
                        state.audio.suonaMusica(AudioManager.VITTORIA);
                    } else if (isShopkeeper) {
                        // Shopkeeper sconfitto: sblocca melee o buffa danno se gia sbloccato
                        roomMgr.onShopkeeperSconfitto();
                    } else if (nemici.isEmpty() && roomMgr.inStanzaBonus) {
                        // Ultimo nemico nella stanza ardua: spawna ricompensa in stanza
                        roomMgr.completaStanzaArdua();
                    } else if (nemici.isEmpty() && !roomMgr.inStanzaShop
                            && state.stanzaNelMondo != GameState.STANZA_BOSS) {
                        spawnDrop((int) dropX, (int) dropY);
                    }
                }
                break;
            }
        }

        // ── Melee vs nemici ───────────────────────────────────────────────────
        if (state.meleeTimer > 0) {
            state.meleeTimer--;
            int meleeRaggio = getMeleeRaggio();
            Rectangle hbMelee = new Rectangle(
                    state.meleeHitX - meleeRaggio,
                    state.meleeHitY - meleeRaggio,
                    meleeRaggio * 2, meleeRaggio * 2);

            for (int j = nemici.size() - 1; j >= 0; j--) {
                Nemico n = nemici.get(j);
                if (!hbMelee.intersects(n.getHitbox())) continue;
                n.subisciDanno(getMeleeDanno());
                if (n.isMorto()) {
                    boolean isShopkeeper2 = n instanceof ShopkeeperNemico;
                    boolean isBoss2       = n instanceof Boss;
                    float dropX = n.x, dropY = n.y;
                    nemici.remove(j);
                    state.audio.suonaEffetto(AudioManager.SFX_MORTE_NEMICO);
                    if (isBoss2) {
                        state.bossSconfitto = true;
                        state.audio.suonaMusica(AudioManager.VITTORIA);
                    } else if (isShopkeeper2) {
                        roomMgr.onShopkeeperSconfitto();
                    } else if (nemici.isEmpty() && roomMgr.inStanzaBonus) {
                        roomMgr.completaStanzaArdua();
                    } else if (nemici.isEmpty() && !roomMgr.inStanzaShop
                            && state.stanzaNelMondo != GameState.STANZA_BOSS) {
                        spawnDrop((int) dropX, (int) dropY);
                    }
                }
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
package core;

import audio.AudioManager;
import entity.*;
import items.*;
import resource.ResourceLoader;
import room.RoomManager;
import room.StatNemico;
import room.TileEffetto;
import room.TileSet;
import ui.UIManager;
import ui.DialogoShopkeeper;
import data.DatiPersonaggio;
import data.Lang;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * GameLoop.java
 */
public class GameLoop {

    private final GameState   state;
    private final RoomManager roomMgr;
    private UIManager ui;
    private ResourceLoader res;

    public final List<Pugno> pugniAttivi = new ArrayList<>();
    public final List<BossProjectile> proiettiliCannone = new ArrayList<>();

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

    public void tick() {
        if (state.inBossRush) {
            aggiornaBossRush();
            return;
        }
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

    // ── Boss Rush ─────────────────────────────────────────────────────────────

    private void aggiornaBossRush() {
        if (state.dialogoNarrazione.isAttivo()) return;
        if (state.bossRushSceltaPowerUp) return;

        state.tickInvulnerabilita();
        aggiornaMovBossRush();
        aggiornaSparo();
        aggiornaMelee();
        aggiornaPugni();
        aggiornaTileEffetto();
        aggiornaProiettiliCannone();
        aggiornaCollisioniBossRush();

        if (!state.bossSpawnato) {
            spawnaBossRush(state.bossRushIndice);
        }
    }

    private void aggiornaMovBossRush() {
        final int T   = GameState.TILE_SIZE;
        final float minX = GameState.OFFSET * T;
        final float maxX = (GameState.COL_TOTALI - GameState.OFFSET - 1) * T - GameState.PG_SIZE;
        final float minY = GameState.OFFSET * T;
        final float maxY = (GameState.RIG_TOTALI - GameState.OFFSET - 1) * T - GameState.PG_SIZE;
        float vel = state.velocita;
        if (state.freezeAttivo) return;
        if (state.slowAttivo) vel *= GameState.SLOW_MULT;

        if (state.up    && state.y > minY) { state.y -= vel; if (collideOstacolo()) state.y += vel; }
        if (state.down  && state.y < maxY) { state.y += vel; if (collideOstacolo()) state.y -= vel; }
        if (state.left  && state.x > minX) { state.x -= vel; if (collideOstacolo()) state.x += vel; }
        if (state.right && state.x < maxX) { state.x += vel; if (collideOstacolo()) state.x -= vel; }

        state.x = Math.max(minX, Math.min(maxX, state.x));
        state.y = Math.max(minY, Math.min(maxY, state.y));
    }

    private void aggiornaCollisioniBossRush() {
        List<Nemico> nemici = roomMgr.getNemiciBossRush();
        int[][] ostacoli = null;

        for (int i = 0; i < nemici.size(); i++) {
            Nemico n = nemici.get(i);
            n.update(state.x, state.y, nemici, ostacoli);

            if (n.toccaGiocatore(state.x, state.y, GameState.PG_SIZE)) {
                state.riceviDanno();
            }
            if (n instanceof Boss b) {
                BossProjectile.Tipo colpito =
                        b.controllaCollisioneProiettiliConTipo(state.x, state.y, GameState.PG_SIZE);
                if (colpito != null) {
                    state.riceviDanno();
                    if (colpito == BossProjectile.Tipo.FUOCO) {
                        if (state.bossRushIndice == 4) {
                            if (!state.freezeAttivo) {
                                state.freezeAttivo = true;
                                state.freezeTimer  = GameState.FREEZE_DURATA;
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

        if (state.burnAttivo) {
            state.burnTimer--;
            state.burnTick++;
            if (state.burnTick >= GameState.BURN_INTERVALLO) {
                state.burnTick = 0;
                if (state.vite > 0) state.vite -= GameState.BURN_DANNO;
            }
            if (state.burnTimer <= 0) { state.burnAttivo = false; state.burnTimer = 0; }
        }

        for (Nemico n : nemici) n.tickBarra();

        for (int i = pugniAttivi.size()-1; i >= 0; i--) {
            Pugno p = pugniAttivi.get(i);
            if (p.daRimuovere) { pugniAttivi.remove(i); continue; }
            Rectangle hbP = p.getHitbox();
            for (int j = nemici.size()-1; j >= 0; j--) {
                Nemico n = nemici.get(j);
                if (!hbP.intersects(n.getHitbox())) continue;
                n.subisciDanno(p.getDanno());
                p.daRimuovere = true;
                if (n.isMorto()) {
                    nemici.remove(j);
                    state.audio.suonaEffetto(AudioManager.SFX_MORTE_NEMICO);
                    state.audio.suonaMusica(AudioManager.VITTORIA);
                    state.bossSconfitto = true;
                    pugniAttivi.clear();
                    roomMgr.bossRushBossSconfitto();
                    return;
                }
                break;
            }
        }

        if (state.meleeTimer > 0 && !state.bossRushSceltaPowerUp) {
            int meleeRaggio = getMeleeRaggio();
            Rectangle hbMelee = new Rectangle(
                    state.meleeHitX - meleeRaggio, state.meleeHitY - meleeRaggio,
                    meleeRaggio * 2, meleeRaggio * 2);
            for (int j = nemici.size()-1; j >= 0; j--) {
                Nemico n = nemici.get(j);
                if (!hbMelee.intersects(n.getHitbox())) continue;
                n.subisciDanno(getMeleeDanno());
                if (n.isMorto()) {
                    nemici.remove(j);
                    state.audio.suonaEffetto(AudioManager.SFX_MORTE_NEMICO);
                    state.audio.suonaMusica(AudioManager.VITTORIA);
                    state.bossSconfitto = true;
                    state.meleeTimer = 0;
                    state.meleeCooldown = GameState.MELEE_DELAY;
                    roomMgr.bossRushBossSconfitto();
                }
                break;
            }
        }

        if (state.vite <= 0) {
            state.statoGioco = GameState.StatoGioco.GAME_OVER;
        }
    }

    private void spawnaBossRush(int mondoBoss) {
        java.util.List<Nemico> nemici = roomMgr.getNemiciBossRush();
        if (!nemici.isEmpty()) return;
        int vitaBoss = StatNemico.vitaBoss(mondoBoss);
        Boss boss = new Boss(7, 2, GameState.TILE_SIZE, vitaBoss, mondoBoss);
        nemici.add(boss);
        state.bossSpawnato = true;
        state.bossSconfitto = false;
        state.dialogoNarrazione.pulisci();
        state.audio.suonaMusica(AudioManager.BOSS);
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

        if (dialogo.getStato() == DialogoShopkeeper.Stato.MOSTRA_DIALOGO
                && !state.dialogoNarrazione.isAttivo()
                && !state.dialogoShopkeeperNarrazioneAvviata) {
            state.dialogoShopkeeperNarrazioneAvviata = true;
            java.awt.image.BufferedImage sprPg = res.getImgGiocatorePerIndice(state.indicePersonaggioSelezionato);
            java.awt.image.BufferedImage sprSk = shopkeeperImgRef;
            state.dialogoNarrazione.pulisci();
            state.dialogoNarrazione.aggiungiKey("dial.shop.nome", sprSk, "dial.shop.sk0", false);
            state.dialogoNarrazione.aggiungiPgKey(state.nomePersonaggioCorrente(), sprPg, "dial.shop.pg0", true);
            state.dialogoNarrazione.aggiungiKey("dial.shop.nome", sprSk, "dial.shop.sk1", false);
            state.dialogoNarrazione.avvia();
        }

        if (dialogo.getStato() == DialogoShopkeeper.Stato.ATTACCO) {
            float skX = sk.getX(), skY = sk.getY();
            sks.clear();
            roomMgr.getItemsShop().clear();
            java.awt.image.BufferedImage imgSKN = shopkeeperNemicoImgRef != null
                    ? shopkeeperNemicoImgRef : shopkeeperImgRef;
            roomMgr.getShopNemici().add(new ShopkeeperNemico(skX, skY, imgSKN));
            state.shopkeeperFight = true;
            dialogo.consuma();
            state.dialogoNarrazione.pulisci();
            state.dialogoShopkeeperNarrazioneAvviata = false;
        } else if (dialogo.getStato() == DialogoShopkeeper.Stato.RIFIUTO) {
            dialogo.consuma();
            state.dialogoNarrazione.pulisci();
            state.dialogoShopkeeperNarrazioneAvviata = false;
        }
    }

    private java.awt.image.BufferedImage shopkeeperImgRef;
    private java.awt.image.BufferedImage shopkeeperNemicoImgRef;
    public void setResourceLoader(ResourceLoader r) { this.res = r; }
    public void setShopkeeperImage(java.awt.image.BufferedImage img) { this.shopkeeperImgRef = img; }
    public void setShopkeeperNemicoImage(java.awt.image.BufferedImage img) { this.shopkeeperNemicoImgRef = img; }

    // ── Movimento ─────────────────────────────────────────────────────────────

    private void aggiornaMov() {
        final int T = GameState.TILE_SIZE;
        final int O = GameState.OFFSET;

        float minX = O * T;
        float maxX = (O + GameState.COL_GIOCO) * T - GameState.PG_SIZE;
        float minY = O * T;
        float maxY = (O + GameState.RIG_GIOCO) * T - GameState.PG_SIZE;

        if (state.freezeAttivo) return;

        float vel = state.velocita;
        if (state.slowAttivo) vel *= GameState.SLOW_MULT;

        if      (state.up)    { state.facingX = 0;  state.facingY = -1; }
        else if (state.down)  { state.facingX = 0;  state.facingY =  1; }
        else if (state.left)  { state.facingX = -1; state.facingY =  0; }
        else if (state.right) { state.facingX =  1; state.facingY =  0; }

        if (roomMgr.inStanzaBonus) {
            if (state.up    && state.y > minY) state.y -= vel;
            if (state.down  && state.y < maxY) state.y += vel;
            if (state.left  && state.x > minX) state.x -= vel;
            if (state.right && state.x < maxX) state.x += vel;
            if (state.y <= minY) state.y = minY + 2;
            if (state.y >= maxY) state.y = maxY - 2;
            return;
        }

        if (roomMgr.inStanzaShop) {
            if (state.up    && state.y > minY) state.y -= vel;
            if (state.down  && state.y < maxY) state.y += vel;
            if (state.left  && state.x > minX) state.x -= vel;
            if (state.right && state.x < maxX) state.x += vel;
            if (state.y >= maxY && state.down) {
                // Non uscire durante il fight con lo shopkeeper
                if (state.shopkeeperFight) {
                    state.y = maxY - 2; // respingi
                } else {
                    roomMgr.esciDalloShop();
                }
            }
            return;
        }

        if (state.up    && state.y > minY) { state.y -= vel; if (collideOstacolo()) state.y += vel; }
        if (state.down  && state.y < maxY) { state.y += vel; if (collideOstacolo()) state.y -= vel; }

        boolean in62Aperta = state.stanzaNelMondo == state.stanzaConPortaArdua
                && !state.ardua_completed
                && stanzaPulita()
                && inZonaCentroX();
        if (state.down && state.y >= maxY && in62Aperta) {
            roomMgr.entraInBonus();
            return;
        }

        if (state.tombinoVisibile
                && state.stanzaNelMondo == GameState.STANZA_BOSS
                && state.mondoAttuale == 1
                && state.bossSconfitto) {
            int T2 = GameState.TILE_SIZE;
            float tombinoX = GameState.TOMBINO_COL * T2 + T2 / 2f;
            float tombinoY = GameState.TOMBINO_RIG * T2 + T2 / 2f;
            float pgCX = state.x + GameState.PG_SIZE / 2f;
            float pgCY = state.y + GameState.PG_SIZE / 2f;
            float dist = (float) Math.sqrt(Math.pow(pgCX - tombinoX, 2) + Math.pow(pgCY - tombinoY, 2));
            if (dist < T2 * 1.1f) {
                roomMgr.entraInBossRush();
                return;
            }
        }

        boolean inZonaPortaNord = inZonaCentroX();
        if (state.up && state.y <= minY
                && state.stanzaNelMondo == 1
                && state.shopSbloccato
                && inZonaPortaNord) {
            roomMgr.assicuraShopGenerato();
            roomMgr.entraNelloShop();
            return;
        }

        float centroPortaY = (GameState.RIG_TOTALI * T) / 2f;
        boolean inZonaPorta = state.y > centroPortaY - T && state.y < centroPortaY + T;

        if (state.left) {
            if (state.x > minX) {
                state.x -= vel;
                if (collideOstacolo()) state.x += vel;
            } else if (inZonaPorta && state.indiceStanzaMemoria > 0) {
                boolean bossAttivo = state.stanzaNelMondo == GameState.STANZA_BOSS
                        && state.bossSpawnato && !state.bossSconfitto;
                if (!bossAttivo) {
                    roomMgr.tornaAllaStanzaPrecedente();
                    pugniAttivi.clear();
                } else {
                    state.x = minX + 2;
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

    private boolean inZonaCentroX() {
        return state.x > (GameState.LARGHEZZA_GIOCO / 2f - GameState.TILE_SIZE)
                && state.x < (GameState.LARGHEZZA_GIOCO / 2f + GameState.TILE_SIZE);
    }

    private boolean collideOstacolo() {
        int[][] ostacoli = roomMgr.getOstacoliCorrenti();
        if (ostacoli == null) return false;
        final int T = GameState.TILE_SIZE;
        final int margin = 4;
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
                // Ferma timer speedrun se la storia è finita
                if (state.statoGioco == GameState.StatoGioco.UFFICIO
                        || state.statoGioco == GameState.StatoGioco.VITTORIA_STORIA) {
                    state.fermaRunTimer();
                }
                pugniAttivi.clear();
            } else {
                state.x = maxX - 10;
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

    // ── Nota ─────────────────────────────────────────────────────────────────

    private void aggiornaNota() {
        if (state.mostraNota) return;
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
    }

    private void avviaDialogoCapo() {
        java.awt.image.BufferedImage sprCapo = res != null ? res.imgCapo : null;
        java.awt.image.BufferedImage sprPg   = res != null
                ? res.getImgGiocatorePerIndice(state.indicePersonaggioSelezionato) : null;
        String nomePg = state.nomePersonaggioCorrente();

        state.dialogoNarrazione.pulisci();
        state.dialogoNarrazione.aggiungiKey("dial.capo.nome", sprCapo, "dial.capo.sk0", false);
        state.dialogoNarrazione.aggiungiPgKey(nomePg, sprPg, "dial.capo.pg0", true);
        state.dialogoNarrazione.aggiungiKey("dial.capo.nome", sprCapo, "dial.capo.sk1", false);
        state.dialogoNarrazione.aggiungiPgKey(nomePg, sprPg, "dial.capo.pg1", true);
        state.dialogoNarrazione.aggiungiKey("dial.capo.nome", sprCapo, "dial.capo.sk2", false);
        state.dialogoNarrazione.aggiungiPgKey(nomePg, sprPg, "dial.capo.pg2", true);
        state.dialogoNarrazione.aggiungiKey("dial.capo.nome", sprCapo, "dial.capo.sk3", false);
        state.dialogoNarrazione.avvia();
    }

    // ── Melee ─────────────────────────────────────────────────────────────────

    private void aggiornaMelee() {
        if (!state.meleeUnlocked) return;
        if (state.meleeCooldown  > 0) state.meleeCooldown--;
        if (state.meleeNomeTimer > 0) state.meleeNomeTimer--;
        if (state.arduaRicompensaTimer > 0) {
            state.arduaRicompensaTimer--;
            if (state.arduaRicompensaTimer == 0) {
                state.arduaRicompensaMsg = "";
                if (roomMgr.inStanzaBonus && state.ardua_completed) {
                    roomMgr.esciDaBonus();
                }
            }
        }

        if (state.meleeAttivo) {
            state.meleeAttivo = false;
            if (state.meleeCooldown <= 0) {
                int reach = getMeleeReach();
                state.meleeHitX = (int)(state.x + GameState.PG_SIZE / 2f + state.facingX * reach);
                state.meleeHitY = (int)(state.y + GameState.PG_SIZE / 2f + state.facingY * reach);
                state.meleeTimer    = GameState.MELEE_DURATION;
                state.meleeCooldown = GameState.MELEE_DELAY;
                state.meleeNomeTimer = GameState.MELEE_NOME_DURATA;
            }
        }
    }

    private int getMeleeDanno() {
        int base = switch (state.indicePersonaggioSelezionato) {
            case 0 -> 3;
            case 1 -> 2;
            case 2 -> 6;
            case 3 -> 4;
            case 4 -> 99;
            default -> 3;
        };
        return base + state.meleeDannoBonus;
    }

    private int getMeleeRaggio() {
        return switch (state.indicePersonaggioSelezionato) {
            case 0 -> 40;
            case 1 -> 30;
            case 2 -> 55;
            case 3 -> 50;
            case 4 -> 80;
            default -> 40;
        };
    }

    private int getMeleeReach() {
        return switch (state.indicePersonaggioSelezionato) {
            case 0 -> 55;
            case 1 -> 45;
            case 2 -> 50;
            case 3 -> 35;
            case 4 -> 60;
            default -> 50;
        };
    }

    private BufferedImageRef pungnoImageRef;
    public void setPungoImage(java.awt.image.BufferedImage img) { this.pungnoImageRef = new BufferedImageRef(img); }
    private static class BufferedImageRef {
        final java.awt.image.BufferedImage img;
        BufferedImageRef(java.awt.image.BufferedImage img) { this.img = img; }
    }

    // ── Tile effetto ──────────────────────────────────────────────────────────

    private void aggiornaTileEffetto() {
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

            if (te.tipo == TileEffetto.Tipo.CANNONE) {
                if (te.isReady()) lanciaProiettileCannone(te);
                continue;
            }

            if (!sopra || !te.isReady()) continue;

            switch (te.tipo) {
                case VELENO -> {
                    state.riceviDanno();
                    if (state.arduaRicompensaTimer <= 50) {
                        state.arduaRicompensaMsg   = Lang.t("popup.veleno");
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
                            state.arduaRicompensaMsg   = Lang.t("popup.congelato");
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
        float sx = te.col * T + T / 2f;
        float sy = te.rig * T + T / 2f;
        float tx = state.x + GameState.PG_SIZE / 2f;
        float ty = state.y + GameState.PG_SIZE / 2f;
        proiettiliCannone.add(new BossProjectile(sx, sy, tx, ty, null, BossProjectile.Tipo.NORMAL));
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
            if (p.x < 0 || p.x > maxX || p.y < 0 || p.y > maxY) {
                proiettiliCannone.remove(i);
                continue;
            }
            Rectangle hbP = new Rectangle((int)p.x - 8, (int)p.y - 8, 16, 16);
            if (!state.invulnerabile && hbP.intersects(hbPG)) {
                state.riceviDanno();
                if (state.arduaRicompensaTimer <= 50) {
                    state.arduaRicompensaMsg   = Lang.t("popup.cannone");
                    state.arduaRicompensaTimer = 50;
                }
                proiettiliCannone.remove(i);
            }
        }
    }

    // ── Pugni ─────────────────────────────────────────────────────────────────

    private void aggiornaPugni() {
        for (int i = 0; i < pugniAttivi.size(); i++) {
            Pugno p = pugniAttivi.get(i);
            p.update();
            if (p.daRimuovere) pugniAttivi.remove(i--);
        }
    }

    // ── Shop ──────────────────────────────────────────────────────────────────

    private void aggiornaShop() {
        Rectangle hbPG = new Rectangle((int) state.x, (int) state.y,
                GameState.PG_SIZE, GameState.PG_SIZE);

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
            }
        }
    }

    private void applicaEffettoItem(String tipo) {
        switch (tipo) {
            case "CURA", "HP UP" -> {
                state.viteMaxGiocatore++;
                state.vite = Math.min(state.vite + 1, state.viteMaxGiocatore);
                state.arduaRicompensaMsg   = Lang.t("popup.vita");
                state.arduaRicompensaTimer = 90;
            }
            case "VELOCITA" -> {
                state.velocita += 1.5f;
                state.arduaRicompensaMsg   = Lang.t("popup.velocita");
                state.arduaRicompensaTimer = 90;
            }
            case "DANNO" -> {
                state.dannoPugno += 2;
                state.arduaRicompensaMsg   = Lang.t("popup.danno2");
                state.arduaRicompensaTimer = 90;
            }
            case "FIRE RATE" -> {
                state.sparoDelayRiduzione = Math.min(state.sparoDelayRiduzione + 3, 8);
                state.arduaRicompensaMsg   = Lang.t("popup.firerate");
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

        int[][] ostacoli = roomMgr.getOstacoliCorrenti();
        for (int i = 0; i < nemici.size(); i++) {
            Nemico n = nemici.get(i);
            n.update(state.x, state.y, nemici, ostacoli);

            if (n instanceof ShopkeeperNemico skn) {
                if (skn.tentaAttaccoMelee(state.x, state.y)) {
                    state.riceviDanno();
                }
            } else if (n.toccaGiocatore(state.x, state.y, GameState.PG_SIZE)) {
                state.riceviDanno();
                int mondoN = state.mondoAttuale;
                if (n instanceof NemicoForte) {
                    if (mondoN == 3 && !state.burnAttivo) {
                        state.burnAttivo = true;
                        state.burnTimer  = GameState.BURN_DURATA;
                        state.burnTick   = 0;
                    }
                }
            }
            if (n instanceof Boss b) {
                BossProjectile.Tipo colpito =
                        b.controllaCollisioneProiettiliConTipo(state.x, state.y, GameState.PG_SIZE);
                if (colpito != null) {
                    state.riceviDanno();
                    if (colpito == BossProjectile.Tipo.FUOCO) {
                        if (b.getTipo() == 4) {
                            if (!state.freezeAttivo) {
                                state.freezeAttivo = true;
                                state.freezeTimer  = GameState.FREEZE_DURATA;
                                state.arduaRicompensaMsg   = Lang.t("popup.congelato");
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

        for (Nemico n : nemici) n.tickBarra();

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
                        if (state.inBossRush) {
                            roomMgr.bossRushBossSconfitto();
                        } else if (state.mondoAttuale == 1
                                && state.modalitaScelta == GameState.Modalita.STORIA
                                && !state.bossRushCompletata) {
                            state.tombinoVisibile = true;
                        }
                    } else if (isShopkeeper) {
                        state.shopkeeperFight = false;
                        roomMgr.onShopkeeperSconfitto();
                    } else if (nemici.isEmpty() && roomMgr.inStanzaBonus) {
                        roomMgr.completaStanzaArdua();
                    } else if (nemici.isEmpty() && !roomMgr.inStanzaShop
                            && state.stanzaNelMondo != GameState.STANZA_BOSS) {
                        spawnDrop((int) dropX, (int) dropY);
                    }
                }
                break;
            }
        }

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
                        if (state.inBossRush) {
                            roomMgr.bossRushBossSconfitto();
                        } else if (state.mondoAttuale == 1
                                && state.modalitaScelta == GameState.Modalita.STORIA
                                && !state.bossRushCompletata) {
                            state.tombinoVisibile = true;
                        }
                    } else if (isShopkeeper2) {
                        state.shopkeeperFight = false;
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

    // ── Spawn drop con fix bordi ──────────────────────────────────────────────

    private void spawnDrop(int dropPixelX, int dropPixelY) {
        int tx = dropPixelX / GameState.TILE_SIZE;
        int ty = dropPixelY / GameState.TILE_SIZE;

        // Clamp dentro i bordi giocabili (non nei muri)
        tx = Math.max(GameState.OFFSET + 1, Math.min(GameState.OFFSET + GameState.COL_GIOCO - 2, tx));
        ty = Math.max(GameState.OFFSET + 1, Math.min(GameState.OFFSET + GameState.RIG_GIOCO - 2, ty));

        int[] tile = trovaTileLibera(tx, ty);
        tx = tile[0]; ty = tile[1];

        if (state.vite < state.viteMaxGiocatore) {
            roomMgr.getCureCorrenti().add(new Cura(tx, ty, GameState.TILE_SIZE, curaImageRef));
        } else {
            roomMgr.getMoneteCorrenti().add(new Moneta(tx, ty, GameState.TILE_SIZE, monetaImageRef));
        }
    }

    private int[] trovaTileLibera(int tx, int ty) {
        // Clamp dentro i bordi giocabili
        tx = Math.max(GameState.OFFSET + 1, Math.min(GameState.OFFSET + GameState.COL_GIOCO - 2, tx));
        ty = Math.max(GameState.OFFSET + 1, Math.min(GameState.OFFSET + GameState.RIG_GIOCO - 2, ty));

        if (!TileSet.isMuro(tx, ty) && !roomMgr.isOstacolo(tx, ty)) return new int[]{tx, ty};
        for (int r = 1; r <= 4; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    if (Math.abs(dx) != r && Math.abs(dy) != r) continue;
                    int nx = tx + dx, ny = ty + dy;
                    if (nx <= GameState.OFFSET || nx >= GameState.OFFSET + GameState.COL_GIOCO - 1) continue;
                    if (ny <= GameState.OFFSET || ny >= GameState.OFFSET + GameState.RIG_GIOCO - 1) continue;
                    if (!TileSet.isMuro(nx, ny) && !roomMgr.isOstacolo(nx, ny))
                        return new int[]{nx, ny};
                }
            }
        }
        return new int[]{GameState.COL_TOTALI/2, GameState.RIG_TOTALI/2};
    }

    private java.awt.image.BufferedImage curaImageRef;
    private java.awt.image.BufferedImage monetaImageRef;

    public void setDropImages(java.awt.image.BufferedImage cura, java.awt.image.BufferedImage moneta) {
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
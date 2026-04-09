package room;

import core.GameState;
import data.Lang;
import entity.Boss;
import entity.Nemico;
import entity.NemicoForte;
import entity.Shopkeeper;
import items.Cura;
import items.Moneta;
import items.Nota;
import items.ShopItem;
import resource.ResourceLoader;
import entity.Pugno;
import entity.BossProjectile;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * RoomManager.java
 * Responsabile di:
 *  - Tenere in memoria lo stato di ogni stanza visitata
 *    (nemici, cure, monete, shopkeeper, items)
 *  - Generare nuove stanze proceduralmente
 *  - Gestire la transizione tra stanze e tra mondi
 *
 * Non modifica direttamente GameState.statoGioco:
 * comunica gli eventi importanti tramite RoomEventListener.
 */
public class RoomManager {

    // ── Liste di memoria (una entry per ogni stanza generata) ─────────────────
    public final List<Color>           memoriaColoriMondo1        = new ArrayList<>();
    public final List<Color>           memoriaColoriMondo2        = new ArrayList<>();
    public final List<List<Nemico>>    nemiciPerStanza            = new ArrayList<>();
    public final List<List<Cura>>      curePerStanza              = new ArrayList<>();
    public final List<List<Moneta>>    monetePerStanza            = new ArrayList<>();
    public final List<List<Shopkeeper>> shopkeepersPerStanza      = new ArrayList<>();
    public final List<List<ShopItem>>  shopItemsPerStanza         = new ArrayList<>();
    private Nota notaCasa = null;
    // Ostacoli inagibili per stanza
    public final List<int[][]>              ostacoliPerStanza      = new ArrayList<>();
    public       int[][]                    ostacoliStanzaCorrente = null;
    // Tile effetto per stanza (veleno/ghiaccio/fuoco/cannone)
    public final List<List<TileEffetto>>    tileEffettoPerStanza   = new ArrayList<>();
    public       List<TileEffetto>          tileEffettoCorrenti    = new ArrayList<>();
    // Tile effetto nella stanza bonus
    private      List<TileEffetto>          tileEffettoBonus       = new ArrayList<>();

    // ── Dipendenze ────────────────────────────────────────────────────────────
    private final GameState     state;
    private final ResourceLoader res;
    private final Random         random = new Random();

    // ── Callback eventi stanza ─────────────────────────────────────────────────
    private RoomEventListener eventListener;

    public void setEventListener(RoomEventListener listener) {
        this.eventListener = listener;
    }

    // ─────────────────────────────────────────────────────────────────────────
    public RoomManager(GameState state, ResourceLoader res) {
        this.state = state;
        this.res   = res;
        inizializzaStanzaDefault();
    }

    // ── Inizializzazione ──────────────────────────────────────────────────────

    /**
     * Crea la stanza 1 vuota del mondo 1 (prima stanza sempre vuota).
     */
    public void inizializzaStanzaDefault() {
        memoriaColoriMondo1.add(new Color(60, 60, 80));
        nemiciPerStanza.add(new ArrayList<>());
        curePerStanza.add(new ArrayList<>());
        monetePerStanza.add(new ArrayList<>());
        shopkeepersPerStanza.add(new ArrayList<>());
        shopItemsPerStanza.add(new ArrayList<>());
        ostacoliPerStanza.add(null);           // stanza 1: nessun ostacolo
        tileEffettoPerStanza.add(new ArrayList<>()); // stanza 1: nessun tile effetto
    }

    /**
     * Svuota tutta la memoria e reinizializza la stanza di default.
     * Chiamato da GameState.resetTotale() o al cambio mondo.
     */
    public void resetCompleto() {
        nemiciPerStanza.clear();
        curePerStanza.clear();
        monetePerStanza.clear();
        shopkeepersPerStanza.clear();
        shopItemsPerStanza.clear();
        ostacoliPerStanza.clear();
        ostacoliStanzaCorrente = null;
        tileEffettoPerStanza.clear();
        tileEffettoCorrenti = new ArrayList<>();
        tileEffettoBonus    = new ArrayList<>();
        memoriaColoriMondo1.clear();
        memoriaColoriMondo2.clear();
        resetShop();
        resetBonus();
        notaCasa = null;
        inizializzaStanzaDefault();
    }

    // ── Generazione stanza ────────────────────────────────────────────────────

    /**
     * Genera e memorizza una nuova stanza in base alla progressione attuale.
     * Aggiunge le liste alle collezioni di memoria.
     */
    public void generaNuovaStanza() {
        // Colore di sfondo
        if (state.mondoAttuale % 2 != 0) {
            memoriaColoriMondo1.add(new Color(
                    random.nextInt(100), random.nextInt(100), random.nextInt(100)));
        } else {
            memoriaColoriMondo2.add(new Color(
                    100 + random.nextInt(50), random.nextInt(100), 150 + random.nextInt(100)));
        }

        List<Nemico>     nuoviNemici      = new ArrayList<>();
        List<Cura>       nuoveCure        = new ArrayList<>();
        List<Moneta>     nuoveMonete      = new ArrayList<>();
        List<Shopkeeper> nuoviShopkeepers = new ArrayList<>();
        List<ShopItem>   nuoveItems       = new ArrayList<>();

        // Tile effetto: nei mondi 2-5 sostituiscono gli ostacoli fisici.
        // Mondo 1: ostacoli normali. Mondi 2+: solo tile effetto (calpestabili ma con effetto).
        int[][] ostacoli = null;
        List<TileEffetto> tileEff = new ArrayList<>();
        if (state.stanzaNelMondo < GameState.STANZA_BOSS && state.stanzaNelMondo > 1) {
            int m = ((state.mondoAttuale - 1) % 5) + 1;
            if (m == 1) {
                ostacoli = generaOstacoli(); // Cantiere: solo ostacoli fisici
            } else {
                tileEff = generaTileEffetto(); // Mondi 2-5: tile effetto al posto degli ostacoli
                // ostacoli rimane null → tile calpestabili
            }
        }

        if (state.stanzaNelMondo == GameState.STANZA_BOSS && !state.bossSpawnato) {
            generaStanzaBoss(nuoviNemici);

        } else if (state.stanzaNelMondo == GameState.STANZA_BOSS
                && state.bossSpawnato && !state.bossSconfitto) {
            generaStanzaBoss(nuoviNemici);

        } else if (state.stanzaNelMondo == 1 && state.mondoAttuale == 1
                && !state.stanzaCasaVisitata) {
            generaStanzaCasa(nuoveItems);
            state.stanzaCasaVisitata = true;

        } else if (state.stanzaNelMondo < GameState.STANZA_BOSS) {
            generaStanzaNormale(nuoviNemici, ostacoli);
        }

        nemiciPerStanza.add(nuoviNemici);
        curePerStanza.add(nuoveCure);
        monetePerStanza.add(nuoveMonete);
        shopkeepersPerStanza.add(nuoviShopkeepers);
        shopItemsPerStanza.add(nuoveItems);
        ostacoliPerStanza.add(ostacoli);
        tileEffettoPerStanza.add(tileEff);
    }

    private void generaStanzaCasa(List<ShopItem> items) {
        String pu = state.powerUpCasa;
        int cx = GameState.COL_TOTALI / 2;
        int ry = GameState.OFFSET + GameState.RIG_GIOCO / 2;

        // Premi cumulativi: ogni soglia include i premi delle soglie inferiori
        boolean daCura     = pu.equals("CURA")     || pu.equals("VELOCITA") || pu.equals("DANNO") || pu.equals("MELEE") || pu.equals("TUTTO");
        boolean daVelocita = pu.equals("VELOCITA") || pu.equals("DANNO")    || pu.equals("MELEE") || pu.equals("TUTTO");
        boolean daDanno    = pu.equals("DANNO")    || pu.equals("MELEE")    || pu.equals("TUTTO");
        boolean daMelee    = pu.equals("MELEE")    || pu.equals("TUTTO");

        // Posizioni scalate orizzontalmente per non sovrapporsi
        int totale = (daCura?1:0) + (daVelocita?1:0) + (daDanno?1:0) + (daMelee?1:0);
        int spacing = 2;
        int startX = cx - (totale - 1) * spacing / 2;
        int col = 0;

        if (daCura) {
            ShopItem si = new ShopItem(startX + col * spacing, ry, GameState.TILE_SIZE, "HP UP", 0, res.imgCura);
            si.setMostraPrezzo(false); items.add(si); col++;
        }
        if (daVelocita) {
            ShopItem si = new ShopItem(startX + col * spacing, ry, GameState.TILE_SIZE, "VELOCITA", 0, res.imgItemSpeed);
            si.setMostraPrezzo(false); items.add(si); col++;
        }
        if (daDanno) {
            ShopItem si = new ShopItem(startX + col * spacing, ry, GameState.TILE_SIZE, "DANNO", 0, res.imgItemDamage);
            si.setMostraPrezzo(false); items.add(si); col++;
        }
        if (daMelee && !state.meleeUnlocked) {
            // Sblocca melee direttamente senza item fisico
            state.meleeUnlocked = true;
        } else if (daMelee && state.meleeUnlocked) {
            // Melee già sbloccato: +3 danno pugno come bonus
            state.dannoPugno += 3;
        }

        // Nota con codice debug — posizione random libera (non sul power-up)
        if (!state.notaRaccolta) {
            int[] tilesLibere = { 2, 3, 4, 10, 11, 12 }; // colonne lontane dal centro
            int rCol = tilesLibere[random.nextInt(tilesLibere.length)];
            int rRig = GameState.OFFSET + 1 + random.nextInt(GameState.RIG_GIOCO - 2);
            notaCasa = new Nota(rCol, rRig, GameState.TILE_SIZE, res.imgNota);
        }
    }

    private void generaStanzaBoss(List<Nemico> nemici) {
        int vitaBoss = StatNemico.vitaBoss(state.mondoAttuale);
        Boss b = new Boss(7, 2, GameState.TILE_SIZE, vitaBoss, state.mondoAttuale);
        b.caricaProiettile(res.imgBossProjectile);
        b.caricaProiettiliPerTipo(res.imgProiettilePerBoss);
        // Burn callback — boss 4 (Fornace) tocca il giocatore durante carica
        b.setOnBurnPlayer(() -> {
            state.burnAttivo = true;
            state.burnTimer  = GameState.BURN_DURATA;
            state.burnTick   = 0;
        });
        // Freeze callback — boss 4 (Ghiaccio) congela il giocatore a contatto
        b.setOnFreezePlayer(() -> {
            if (!state.freezeAttivo) {
                state.freezeAttivo = true;
                state.freezeTimer  = GameState.FREEZE_DURATA;
                state.arduaRicompensaMsg   = Lang.t("popup.congelato");
                state.arduaRicompensaTimer = 80;
            }
        });
        if (pugniAttiviRef != null) b.setPugniAttiviRef(pugniAttiviRef);
        nemici.add(b);
        state.bossSpawnato       = true;
        state.bossSconfitto      = false;
        state.tempoRimanenteBoss = GameState.TEMPO_BOSS_DEFAULT;

        // ── Dialogo pre-boss ───────────────────────────────────────────────────
        int tipoBoss = (state.mondoAttuale - 1) % 5; // 0-4
        BufferedImage sprPg   = res.getImgGiocatorePerIndice(state.indicePersonaggioSelezionato);
        BufferedImage sprBoss = res.imgBossPerMondo[tipoBoss];
        String nomePg = state.nomePersonaggioCorrente();

        state.dialogoNarrazione.pulisci();
        switch (tipoBoss) {
            case 0 -> { // MANNIE — Cantiere M1
                state.dialogoNarrazione.aggiungiPgKey(nomePg, sprPg, "boss.m1.pg0", true);
                state.dialogoNarrazione.aggiungiKey("boss.m1.nome", sprBoss,
                        "boss.m1.b0", false);
            }
            case 1 -> { // PRESAGIO — Fogne M2
                state.dialogoNarrazione.aggiungiPgKey(nomePg, sprPg, "boss.m2.pg0", true);
            }
            case 2 -> { // RE FORNO — Fornace M3
                state.dialogoNarrazione.aggiungiPgKey(nomePg, sprPg, "boss.m3.pg0", true);
                state.dialogoNarrazione.aggiungiPgKey(nomePg, sprPg, "boss.m3.pg1", true);
                state.dialogoNarrazione.aggiungiKey("boss.m3.nome", sprBoss,
                        "boss.m3.b0", false);
                state.dialogoNarrazione.aggiungiPgKey(nomePg, sprPg, "boss.m3.pg2", true);
            }
            case 3 -> { // GELO — Ghiacciaio M4
                state.dialogoNarrazione.aggiungiPgKey(nomePg, sprPg, "boss.m4.pg0", true);
                state.dialogoNarrazione.aggiungiKey("boss.m4.nome", sprBoss,
                        "boss.m4.b0", false);
                state.dialogoNarrazione.aggiungiPgKey(nomePg, sprPg, "boss.m4.pg1", true);
            }
            case 4 -> { // YABBADUHLON — Castello M5
                state.dialogoNarrazione.aggiungiKey("boss.m5.nome", sprBoss,
                        "boss.m5.b0", false);
                state.dialogoNarrazione.aggiungiPgKey(nomePg, sprPg, "boss.m5.pg0", true);
                state.dialogoNarrazione.aggiungiPgKey(nomePg, sprPg, "boss.m5.pg1", true);
                state.dialogoNarrazione.aggiungiKey("boss.m5.nome", sprBoss,
                        "boss.m5.b1", false);
                state.dialogoNarrazione.aggiungiPgKey(nomePg, sprPg, "boss.m5.pg2", true);
                state.dialogoNarrazione.aggiungiPgKey(nomePg, sprPg, "boss.m5.pg3", true);
                state.dialogoNarrazione.aggiungiKey("boss.m5.nome", sprBoss,
                        "boss.m5.b2", false);
                state.dialogoNarrazione.aggiungiPgKey(nomePg, sprPg, "boss.m5.pg4", true);
            }
        }
        if (state.dialogoNarrazione.getTotale() > 0)
            state.dialogoNarrazione.avvia();
    }

    private void generaStanzaShop(List<Shopkeeper> shopkeepers, List<ShopItem> items) {
        shopkeepers.add(new Shopkeeper(7, 1, GameState.TILE_SIZE, res.imgShopkeeper));
        items.add(new ShopItem(5, 2, GameState.TILE_SIZE, "CURA",     2, res.imgCura));
        items.add(new ShopItem(7, 2, GameState.TILE_SIZE, "VELOCITA", 5, res.imgItemSpeed));
        items.add(new ShopItem(9, 2, GameState.TILE_SIZE, "DANNO",    7, res.imgItemDamage));
    }

    private void generaStanzaNormale(List<Nemico> nemici, int[][] ostacoli) {
        int m     = state.mondoAttuale;
        int quanti = StatNemico.quantiNemici(m, state.stanzaNelMondo, random);
        float probForte = StatNemico.probNemicoForte(m);
        // Tile effetto della stanza corrente (per evitare spawn sopra)
        List<TileEffetto> tileCorr = tileEffettoPerStanza.isEmpty() ? null
                : tileEffettoPerStanza.size() > state.indiceStanzaMemoria
                ? tileEffettoPerStanza.get(state.indiceStanzaMemoria) : null;

        for (int i = 0; i < quanti; i++) {
            int[] pos  = trovaPosizioneSicura(ostacoli, tileCorr);
            boolean forte = random.nextFloat() < probForte;

            if (forte) {
                Nemico nf = new NemicoForte(pos[0], pos[1], GameState.TILE_SIZE,
                        StatNemico.vitaNemicoForte(m));
                nf.setVelocita(StatNemico.velocitaNemicoForte(m));
                nemici.add(nf);
            } else {
                Nemico n = new Nemico(pos[0], pos[1], GameState.TILE_SIZE,
                        StatNemico.vitaNemico(m));
                n.setVelocita(StatNemico.velocitaNemico(m));
                nemici.add(n);
            }
        }
    }

    /**
     * Stanza Ardua (stanza 7, pre-boss).
     * Nemici misti da mondi diversi, piu numerosi e veloci.
     * Applica malus casuali al giocatore per la durata della stanza.
     */
    private void generaStanzaArdua(List<Nemico> nemici, int[][] ostacoli) {
        int m = state.mondoAttuale;
        int quanti = 5 + m;
        int mondiDisponibili = 4;

        for (int i = 0; i < quanti; i++) {
            int mondoNemico = 1 + random.nextInt(mondiDisponibili);
            int[] pos = trovaPosizioneSicura(ostacoli, tileEffettoBonus);
            boolean forte = random.nextFloat() < 0.5f;

            if (forte) {
                Nemico nf = new NemicoForte(pos[0], pos[1], GameState.TILE_SIZE,
                        StatNemico.vitaNemicoForte(mondoNemico));
                nf.setVelocita(StatNemico.velocitaNemicoForte(mondoNemico) * 1.2f);
                nemici.add(nf);
            } else {
                Nemico n = new Nemico(pos[0], pos[1], GameState.TILE_SIZE,
                        StatNemico.vitaNemico(mondoNemico));
                n.setVelocita(StatNemico.velocitaNemico(mondoNemico) * 1.2f);
                nemici.add(n);
            }
        }

        // Applica malus casuali (1 o 2 tra danno / velocita / fire rate)
        state.arduaMalusDanno    = 0;
        state.arduaMalusVelocita = 0f;
        state.arduaMalusFireRate = 0;
        java.util.List<Integer> pool = new java.util.ArrayList<>(java.util.Arrays.asList(0, 1, 2));
        java.util.Collections.shuffle(pool, random);
        int quanti_malus = 1 + random.nextInt(2);
        for (int i = 0; i < quanti_malus; i++) {
            switch (pool.get(i)) {
                case 0 -> state.arduaMalusDanno    = 2;
                case 1 -> state.arduaMalusVelocita = 2.0f;
                case 2 -> state.arduaMalusFireRate  = 5;
            }
        }
        state.dannoPugno = Math.max(1, state.dannoPugno - state.arduaMalusDanno);
        state.velocita   = Math.max(1f, state.velocita  - state.arduaMalusVelocita);
        // arduaMalusFireRate viene applicato nel cooldown sparo
    }

    /** Rimuove i malus ardua quando il giocatore esce dalla stanza. */
    public void rimuoviMalusArdua() {
        state.dannoPugno += state.arduaMalusDanno;
        state.velocita   += state.arduaMalusVelocita;
        state.arduaMalusDanno    = 0;
        state.arduaMalusVelocita = 0f;
        state.arduaMalusFireRate = 0;
    }

    /**
     * Chiamato dal GameLoop quando la stanza ardua viene completata.
     * Assegna la ricompensa al giocatore.
     */
    public void completaStanzaArdua() {
        if (state.ardua_completed) return;
        state.ardua_completed = true;

        // Ricompensa principale
        int cx = GameState.COL_TOTALI / 2;
        int ry = GameState.OFFSET + GameState.RIG_GIOCO / 2;

        if (!state.meleeUnlocked) {
            state.meleeUnlocked       = true;
            state.meleeUnlockedDaArdua = true;
            state.arduaRicompensaMsg   = Lang.t("popup.melee");
            state.arduaRicompensaTimer = 90;
        } else {
            int roll = random.nextInt(4);
            String tipo; java.awt.image.BufferedImage img;
            switch (roll) {
                case 0 -> { tipo = "HP UP";      img = res.imgCura; }
                case 1 -> { tipo = "DANNO";      img = res.imgItemDamage; }
                case 2 -> { tipo = "VELOCITA";   img = res.imgItemSpeed; }
                default -> { tipo = "FIRE RATE"; img = res.imgItemSpeed; }
            }
            ShopItem drop = new ShopItem(cx, ry, GameState.TILE_SIZE, tipo, 0, img);
            drop.setMostraPrezzo(false);
            itemsBonus.add(drop);
        }

        // 40% chance cura extra in posizione sicura (evita tile effetto)
        if (random.nextFloat() < 0.40f) {
            int[] pos = trovaPosizioneSicura(ostacoliBonus, tileEffettoBonus);
            cureBonus.add(new Cura(pos[0], pos[1], GameState.TILE_SIZE, res.imgCura));
        }
    }

    /**
     * Chiamato quando lo shopkeeper viene sconfitto.
     * Se melee gia sbloccato da ardua, buffra il danno pugno invece di sbloccare melee.
     */
    public void onShopkeeperSconfitto() {
        state.monete += 20; // bottino shopkeeper sconfitto
        if (!state.meleeUnlocked) {
            state.meleeUnlocked = true;
            state.arduaRicompensaMsg   = Lang.t("popup.melee");
            state.arduaRicompensaTimer = 90;
        } else {
            // Melee già sbloccato: potenzia danno melee
            state.meleeDannoBonus += 2;
            state.arduaRicompensaMsg   = "+" + state.meleeDannoBonus + " " + Lang.t("popup.danno");
            state.arduaRicompensaTimer = 90;
        }
    }

    public boolean isStanzaArdua() {
        return state.stanzaNelMondo == GameState.STANZA_ARDUA;
    }

    // ── Transizioni ───────────────────────────────────────────────────────────

    /**
     * Entra nella stanza successiva (movimento verso destra).
     * Se la stanza non esiste ancora, la genera.
     */
    public void entraNellaStanzaSuccessiva() {
        state.x = GameState.OFFSET * GameState.TILE_SIZE + 20f;
        state.indiceStanzaMemoria++;
        state.stanzaNelMondo++;
        if (state.indiceStanzaMemoria >= nemiciPerStanza.size()) {
            generaNuovaStanza();
        }
        aggiornaTileEffettoCorrenti();
    }

    public void tornaAllaStanzaPrecedente() {
        // Blocca: non si può tornare prima della stanza 1
        if (state.stanzaNelMondo <= 1 || state.indiceStanzaMemoria <= 0) {
            state.x = (GameState.OFFSET + 1) * GameState.TILE_SIZE; // respingi
            return;
        }
        int maxX = (GameState.COL_TOTALI - GameState.OFFSET - 1) * GameState.TILE_SIZE - 20;
        state.x = maxX;
        state.indiceStanzaMemoria--;
        state.stanzaNelMondo--;
        aggiornaTileEffettoCorrenti();
    }

    /**
     * Avanza al mondo successivo: resetta memoria stanze, mantiene monete e upgrades.
     * Notifica il listener se storia finita.
     */
    public Nota getNotaCasa() { return notaCasa; }

    public void avanzaAlMondoSuccessivo() {
        if (state.modalitaScelta == GameState.Modalita.STORIA
                && state.mondoAttuale == GameState.MONDI_STORIA_MAX) {
            // Dopo il castello: entra nell'Ufficio
            state.statoGioco = GameState.StatoGioco.UFFICIO;
            state.ufficioDialogoAvviato = false;
            if (eventListener != null) eventListener.onVittoriaStoria();
            return;
        }

        // Registra lo sblocco personaggio per questo mondo
        state.sistemaPersonaggi.registraBossSconfitto(state.mondoAttuale);

        state.mondoAttuale++;
        state.stanzaNelMondo      = 1;
        state.indiceStanzaMemoria = 0;
        state.bossSpawnato        = false;
        state.bossSconfitto       = false;
        state.shopSbloccato       = true;

        resetCompleto();
        state.resetGiocatore();
        state.dialogoShopkeeper.reset();
        state.dialogoNarrazione.pulisci();
        state.dialogoShopkeeperNarrazioneAvviata = false;

        if (eventListener != null) eventListener.onCambioMondo(state.mondoAttuale);
    }

    // ── Shop room separata ────────────────────────────────────────────────────

    /**
     * True se il giocatore si trova attualmente nella stanza shop nord.
     * Usato da GameLoop e RenderEngine per comportamento speciale.
     */
    public boolean inStanzaShop = false;

    // ── Stanza Bonus (porta a sud dalla stanza 6) ───────────────────────────────
    public boolean inStanzaBonus = false;

    // ── Boss Rush — lista nemici dedicata ─────────────────────────────────────
    private final java.util.List<Nemico> nemiciBossRush = new java.util.ArrayList<>();
    public java.util.List<Nemico> getNemiciBossRush() { return nemiciBossRush; }

    private final java.util.List<Nemico>   nemiciBonus  = new java.util.ArrayList<>();
    private final java.util.List<ShopItem> itemsBonus   = new java.util.ArrayList<>();
    private final java.util.List<Cura>     cureBonus    = new java.util.ArrayList<>();
    private int[][] ostacoliBonus = null;
    private boolean stanzaBonusGenerata = false;

    public java.util.List<Nemico>   getNemiciBonus()  { return nemiciBonus; }
    public java.util.List<ShopItem> getItemsBonus()   { return itemsBonus; }
    public java.util.List<Cura>     getCureBonus()    { return cureBonus; }

    public void assicuraBonusGenerata() {
        if (stanzaBonusGenerata) return;
        nemiciBonus.clear();
        itemsBonus.clear();
        cureBonus.clear();

        // Ostacoli/tile effetto coerenti col mondo corrente
        int m = ((state.mondoAttuale - 1) % 5) + 1;
        if (m == 1) {
            ostacoliBonus    = generaOstacoli();
            tileEffettoBonus = new ArrayList<>();
        } else {
            ostacoliBonus    = null;
            tileEffettoBonus = generaTileEffetto();
        }
        generaStanzaArdua(nemiciBonus, ostacoliBonus);
        stanzaBonusGenerata = true;
    }

    public void entraInBonus() {
        assicuraBonusGenerata();
        inStanzaBonus = true;
        state.y = GameState.OFFSET * GameState.TILE_SIZE + 20f;
        pugniAttiviRef.clear(); proiettiliCannoneRef.clear();
        aggiornaTileEffettoCorrenti();
        // Reset popup ricompensa (evita che il popup del tetris appaia nella bonus)
        state.arduaRicompensaMsg   = "";
        state.arduaRicompensaTimer = 0;
    }

    public void esciDaBonus() {
        rimuoviMalusArdua();
        inStanzaBonus = false;
        state.y = (GameState.RIG_GIOCO) * GameState.TILE_SIZE - GameState.PG_SIZE - 10f;
        pugniAttiviRef.clear(); proiettiliCannoneRef.clear();
        aggiornaTileEffettoCorrenti();
    }

    public void resetBonus() {
        stanzaBonusGenerata = false;
        nemiciBonus.clear();
        itemsBonus.clear();
        cureBonus.clear();
        ostacoliBonus = null;
        inStanzaBonus = false;
        state.ardua_completed    = false;
        state.arduaMalusDanno    = 0;
        state.arduaMalusVelocita = 0f;
        state.arduaMalusFireRate = 0;
        // Porta ardua appare in una stanza random tra 3 e 6 per questo mondo
        state.stanzaConPortaArdua = 3 + random.nextInt(4); // 3,4,5 o 6
    }

    /**
     * Entra nella stanza shop (porta a nord dalla stanza 1 del nuovo mondo).
     * Memorizza la posizione precedente per il ritorno.
     */
    public void entraNelloShop() {
        inStanzaShop = true;
        state.shopSbloccato = false; // Consumato: non riappare finché non batti il prossimo boss
        state.y = (GameState.RIG_GIOCO) * GameState.TILE_SIZE - GameState.PG_SIZE - 10f; // Entra dal basso della stanza shop
        pugniAttiviRef.clear(); proiettiliCannoneRef.clear();
    }

    /**
     * Esce dalla stanza shop (porta a sud, torna alla stanza 1).
     */
    public void esciDalloShop() {
        inStanzaShop = false;
        state.y = GameState.OFFSET * GameState.TILE_SIZE + 20f; // Torna nella stanza 1 in cima
        pugniAttiviRef.clear(); proiettiliCannoneRef.clear();
    }

    // Riferimento ai pugni attivi passato da GameLoop per pulirli al cambio stanza shop
    private java.util.List<Pugno> pugniAttiviRef = new java.util.ArrayList<>();
    public void setPugniAttiviRef(java.util.List<Pugno> pugni) { this.pugniAttiviRef = pugni; }

    private java.util.List<BossProjectile> proiettiliCannoneRef = new java.util.ArrayList<>();
    public void setProiettiliCannoneRef(java.util.List<BossProjectile> proj) { this.proiettiliCannoneRef = proj; }

    // ── Contenuto stanza shop ─────────────────────────────────────────────────

    // Lista separata per la stanza shop (non fa parte della memoria stanze normale)
    private final java.util.List<Shopkeeper>     shopkeeperShop = new java.util.ArrayList<>();
    private final java.util.List<ShopItem>       itemsShop      = new java.util.ArrayList<>();
    private final java.util.List<Nemico>         shopNemici     = new java.util.ArrayList<>();
    private boolean shopGenerato = false;

    public java.util.List<Shopkeeper> getShopkeeperShop() { return shopkeeperShop; }
    public java.util.List<ShopItem>   getItemsShop()      { return itemsShop; }
    public java.util.List<Nemico>     getShopNemici()     { return shopNemici; }

    /**
     * Genera il contenuto della stanza shop se non è ancora stato generato.
     */
    public void assicuraShopGenerato() {
        if (shopGenerato) return;
        shopkeeperShop.clear();
        itemsShop.clear();
        shopkeeperShop.add(new Shopkeeper(7, 1, GameState.TILE_SIZE, res.imgShopkeeper));
        itemsShop.add(new ShopItem(4, 3, GameState.TILE_SIZE, "CURA",     2, res.imgCura));
        itemsShop.add(new ShopItem(7, 3, GameState.TILE_SIZE, "VELOCITA", 5, res.imgItemSpeed));
        itemsShop.add(new ShopItem(10, 3, GameState.TILE_SIZE, "DANNO",   7, res.imgItemDamage));
        shopGenerato = true;
    }

    /** Resetta il flag shopGenerato (chiamato al cambio mondo per refresh items) */
    public void resetShop() {
        shopGenerato = false;
        shopkeeperShop.clear();
        itemsShop.clear();
        shopNemici.clear();
        inStanzaShop = false;
    }

    public List<Nemico>     getNemiciCorrenti()      {
        int idx = state.indiceStanzaMemoria;
        if (state.inBossRush) return nemiciBossRush; // boss rush usa lista dedicata
        return (idx < nemiciPerStanza.size()) ? nemiciPerStanza.get(idx) : new java.util.ArrayList<>();
    }
    public List<Cura>       getCureCorrenti()         { int idx=state.indiceStanzaMemoria; return (idx < curePerStanza.size()) ? curePerStanza.get(idx) : new java.util.ArrayList<>(); }
    public List<Moneta>     getMoneteCorrenti()       { int idx=state.indiceStanzaMemoria; return (idx < monetePerStanza.size()) ? monetePerStanza.get(idx) : new java.util.ArrayList<>(); }
    public List<Shopkeeper> getShopkeepersCorrenti()  { int idx=state.indiceStanzaMemoria; return (idx < shopkeepersPerStanza.size()) ? shopkeepersPerStanza.get(idx) : new java.util.ArrayList<>(); }
    public List<ShopItem>   getShopItemsCorrenti()    { int idx=state.indiceStanzaMemoria; return (idx < shopItemsPerStanza.size()) ? shopItemsPerStanza.get(idx) : new java.util.ArrayList<>(); }
    public int[][]          getOstacoliCorrenti() {
        if (inStanzaBonus) return ostacoliBonus;
        int idx = state.indiceStanzaMemoria;
        ostacoliStanzaCorrente = (idx < ostacoliPerStanza.size()) ? ostacoliPerStanza.get(idx) : null;
        return ostacoliStanzaCorrente;
    }

    // ── Utilità ───────────────────────────────────────────────────────────────

    /**
     * Calcola la vita del boss in base alla modalità e al mondo attuale.
     */
    public int calcolaVitaBoss() {
        int vita = 100;
        if (state.modalitaScelta == GameState.Modalita.INFINITA) {
            vita += ((state.mondoAttuale - 1) / 2) * 50;
        }
        return vita;
    }

    /**
     * Trova coordinate di spawn sicure (lontane dalle porte).
     * @return array [x, y] in coordinate tile
     */
    private int[] trovaPosizioneSicura(int[][] ostacoli) {
        return trovaPosizioneSicura(ostacoli, null);
    }

    private int[] trovaPosizioneSicura(int[][] ostacoli, List<TileEffetto> tileEffetto) {
        int safeX, safeY;
        int pgTileX = GameState.OFFSET + 1;
        int pgTileY = GameState.OFFSET + GameState.RIG_GIOCO / 2;
        int maxTentativi = 300;
        do {
            safeX = random.nextInt(GameState.COL_GIOCO) + GameState.OFFSET;
            safeY = random.nextInt(GameState.RIG_GIOCO) + GameState.OFFSET;
            if (TileSet.isMuro(safeX, safeY)) { maxTentativi--; continue; }
            if (isOstacoloIn(safeX, safeY, ostacoli)) { maxTentativi--; continue; }
            // Evita tile effetto
            if (tileEffetto != null) {
                boolean suTile = false;
                for (TileEffetto te : tileEffetto)
                    if (te.col == safeX && te.rig == safeY) { suTile = true; break; }
                if (suTile) { maxTentativi--; continue; }
            }
            if (safeX <= GameState.OFFSET + 4) { maxTentativi--; continue; }
            if (safeX >= GameState.COL_TOTALI - GameState.OFFSET - 5) { maxTentativi--; continue; }
            int dx = safeX - pgTileX, dy = safeY - pgTileY;
            if (dx*dx + dy*dy < 25) { maxTentativi--; continue; }
            return new int[]{safeX, safeY};
        } while (maxTentativi > 0);
        return new int[]{GameState.COL_TOTALI/2, GameState.RIG_TOTALI/2};
    }

    /** Controlla ostacoli passati come parametro (usato durante la generazione). */
    private boolean isOstacoloIn(int col, int rig, int[][] ostacoli) {
        if (ostacoli == null) return false;
        for (int[] o : ostacoli) if (o[0]==col && o[1]==rig) return true;
        return false;
    }

    public boolean isOstacolo(int col, int rig) {
        if (ostacoliStanzaCorrente == null) return false;
        for (int[] o : ostacoliStanzaCorrente)
            if (o[0] == col && o[1] == rig) return true;
        return false;
    }

    /**
     * Genera 2-4 tile ostacolo casuali, garantendo che il corridoio
     * orizzontale delle porte (riga centrale ±1 tile ai bordi) rimanga libero.
     */
    private int[][] generaOstacoli() {
        int portaY = GameState.OFFSET + GameState.RIG_GIOCO / 2;
        int quanti = 2 + random.nextInt(3); // 2, 3 o 4
        List<int[]> lista = new ArrayList<>();
        int tentativi = 0;
        while (lista.size() < quanti && tentativi < 300) {
            tentativi++;
            int col = random.nextInt(GameState.COL_GIOCO) + GameState.OFFSET;
            int rig = random.nextInt(GameState.RIG_GIOCO) + GameState.OFFSET;
            if (TileSet.isMuro(col, rig)) continue;
            // Non bloccare le 5 colonne attorno alle porte sulla riga centrale
            if (rig == portaY && col <= GameState.OFFSET + 4) continue;
            if (rig == portaY && col >= GameState.COL_TOTALI - GameState.OFFSET - 5) continue;
            // Non nella zona spawn giocatore (prime 4 colonne)
            if (col <= GameState.OFFSET + 3) continue;
            // Non vicino alla porta destra (ultime 4 colonne)
            if (col >= GameState.COL_TOTALI - GameState.OFFSET - 4) continue;
            // No duplicati
            boolean dup = false;
            for (int[] o : lista) if (o[0]==col && o[1]==rig) { dup=true; break; }
            if (!dup) lista.add(new int[]{col, rig});
        }
        return lista.toArray(new int[0][]);
    }

    /**
     * Genera tile effetto casuali per la stanza — sostituiscono gli ostacoli fisici.
     * M2=VELENO, M3=FUOCO, M4=GHIACCIO mix (75% slow + 25% freeze), M5=CANNONE. 3-5 per stanza.
     */
    private List<TileEffetto> generaTileEffetto() {
        List<TileEffetto> lista = new ArrayList<>();
        int m = ((state.mondoAttuale - 1) % 5) + 1;

        // Mondo 5 (Castello): max 2 cannoni, posizionati in punti strategici
        int base  = (m == 5) ? 2 : 3 + random.nextInt(2); // 3-4 altri mondi
        int bonus = (m >= 4 && m != 5) ? random.nextInt(2) : 0;
        int quanti = base + bonus;

        int portaY = GameState.OFFSET + GameState.RIG_GIOCO / 2;
        int tentativi = 0;

        while (lista.size() < quanti && tentativi < 300) {
            tentativi++;
            int col = random.nextInt(GameState.COL_GIOCO) + GameState.OFFSET;
            int rig = random.nextInt(GameState.RIG_GIOCO) + GameState.OFFSET;
            if (TileSet.isMuro(col, rig)) continue;
            if (col <= GameState.OFFSET + 3) continue;
            if (col >= GameState.COL_TOTALI - GameState.OFFSET - 4) continue;
            if (rig == portaY && col <= GameState.OFFSET + 5) continue;
            if (rig == portaY && col >= GameState.COL_TOTALI - GameState.OFFSET - 5) continue;
            boolean dup = false;
            for (TileEffetto te : lista)
                if (te.col == col && te.rig == rig) { dup = true; break; }
            if (dup) continue;

            TileEffetto.Tipo tipo = switch (m) {
                case 2 -> TileEffetto.Tipo.VELENO;
                case 3 -> TileEffetto.Tipo.FUOCO;
                case 4 -> random.nextFloat() < 0.75f
                        ? TileEffetto.Tipo.GHIACCIO
                        : TileEffetto.Tipo.GHIACCIO_FORTE;
                case 5 -> TileEffetto.Tipo.CANNONE;
                default -> null;
            };
            if (tipo == null) break;
            lista.add(new TileEffetto(col, rig, tipo));
        }
        return lista;
    }

    /** Aggiorna i tile effetto correnti quando si entra in una stanza. */
    public void aggiornaTileEffettoCorrenti() {
        int idx = state.indiceStanzaMemoria;
        if (inStanzaBonus) {
            tileEffettoCorrenti = tileEffettoBonus;
        } else if (idx < tileEffettoPerStanza.size()) {
            tileEffettoCorrenti = tileEffettoPerStanza.get(idx);
        } else {
            tileEffettoCorrenti = new ArrayList<>();
        }
    }

    public List<TileEffetto> getTileEffettoCorrenti() {
        return tileEffettoCorrenti;
    }

    // ── Interfaccia callback ──────────────────────────────────────────────────
    public interface RoomEventListener {
        void onCambioMondo(int nuovoMondo);
        void onVittoriaStoria();
    }

    // ── Boss Rush ─────────────────────────────────────────────────────────────

    /** Entra nella boss rush: prepara la stanza col boss dell'indice richiesto */
    public void entraInBossRush() {
        state.inBossRush             = true;
        state.tombinoVisibile        = false;
        state.bossRushIndice         = 2; // inizia da Presagio
        state.bossRushSconfitti      = 0;
        state.bossRushSceltaPowerUp  = false;
        state.statoGioco             = GameState.StatoGioco.BOSS_RUSH;
        preparaStanzaBossRush();
    }

    /** Prepara la stanza per il boss corrente della rush */
    public void preparaStanzaBossRush() {
        nemiciBossRush.clear();
        if (pugniAttiviRef != null) pugniAttiviRef.clear();
        if (proiettiliCannoneRef != null) proiettiliCannoneRef.clear();
        state.bossSpawnato        = false;
        state.bossSconfitto       = false;
        // Reset timer boss per evitare GAME_OVER immediato
        state.tempoRimanenteBoss  = GameState.TEMPO_BOSS_DEFAULT;
        // burnAttivo/freeze resettatati
        state.burnAttivo   = false; state.burnTimer  = 0;
        state.freezeAttivo = false; state.freezeTimer = 0;
        state.slowAttivo   = false; state.slowTimer   = 0;
        // Posiziona giocatore al centro
        state.x = (GameState.COL_TOTALI / 2f) * GameState.TILE_SIZE - GameState.PG_SIZE / 2f;
        state.y = (GameState.RIG_TOTALI / 2f) * GameState.TILE_SIZE - GameState.PG_SIZE / 2f;
    }

    /** Chiamato quando un boss della rush viene sconfitto */
    public void bossRushBossSconfitto() {
        state.bossRushSconfitti++;
        state.bossRushSceltaPowerUp = true;
        // Genera 3 opzioni random distinte
        java.util.List<Integer> pool = new java.util.ArrayList<>(java.util.Arrays.asList(1,2,3,4));
        java.util.Collections.shuffle(pool, random);
        state.bossRushOpzioni = new int[]{ pool.get(0), pool.get(1), pool.get(2) };
        state.bossRushOpzioneScelta = 0;
    }

    /** Applica il power-up scelto e passa al boss successivo o termina */
    public void applicaPowerUpBossRush(int opzione) {
        int tipo = state.bossRushOpzioni[opzione];
        // Salva il power-up
        if      (state.bossRushSconfitti == 1) state.bossRushPowerUp1 = tipo;
        else if (state.bossRushSconfitti == 2) state.bossRushPowerUp2 = tipo;
        else                                    state.bossRushPowerUp3 = tipo;
        // Applica immediatamente
        applicaTipoPowerUp(tipo);
        state.bossRushSceltaPowerUp = false;

        if (state.bossRushSconfitti >= 3) {
            // Boss rush completata → vai direttamente al castello
            state.bossRushCompletata = true;
            state.inBossRush         = false;
            // Registra sconfitte boss 2,3,4 per sblocco personaggi
            state.sistemaPersonaggi.registraBossSconfitto(2);
            state.sistemaPersonaggi.registraBossSconfitto(3);
            state.sistemaPersonaggi.registraBossSconfitto(4);
            state.mondoAttuale       = 5; // Castello
            state.stanzaNelMondo     = 1;
            state.bossSpawnato       = false;
            state.bossSconfitto      = false;
            state.shopSbloccato      = true;
            state.stanzaCasaVisitata = true; // salta la Casa
            state.statoGioco         = GameState.StatoGioco.GIOCO;
            resetCompleto();
            // resetCompleto chiama inizializzaStanzaDefault che aggiunge stanza vuota (index 0)
            // Sostituiscila con una stanza normale con nemici
            nemiciPerStanza.clear();
            curePerStanza.clear();
            monetePerStanza.clear();
            shopkeepersPerStanza.clear();
            shopItemsPerStanza.clear();
            ostacoliPerStanza.clear();
            tileEffettoPerStanza.clear();
            // Genera stanza 1 castello con nemici reali
            generaNuovaStanza();
            state.indiceStanzaMemoria = 0;
            state.stanzaNelMondo      = 1;
            state.resetGiocatore();
            state.dialogoShopkeeper.reset();
            state.dialogoNarrazione.pulisci();
        } else {
            // Prossimo boss
            state.bossRushIndice++;
            preparaStanzaBossRush();
        }
    }

    private void applicaTipoPowerUp(int tipo) {
        switch (tipo) {
            case 1 -> { // Cura +2 vita max
                state.viteMaxGiocatore += 2;
                state.vite = Math.min(state.vite + 2, state.viteMaxGiocatore);
            }
            case 2 -> { // Velocità
                state.velocita += 1.5f;
            }
            case 3 -> { // Danno
                state.dannoPugno += 2;
            }
            case 4 -> { // Melee
                state.meleeUnlocked = true;
            }
        }
    }

}

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
    private Nota notaCasa = null;  // spawna random in Casa, unica per run
    // Ostacoli inagibili per stanza: lista di [col, rig] in coordinate tile
    public final List<int[][]>         ostacoliPerStanza          = new ArrayList<>();
    public       int[][]               ostacoliStanzaCorrente     = null;

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
        ostacoliPerStanza.add(null); // stanza 1: nessun ostacolo
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
        memoriaColoriMondo1.clear();
        memoriaColoriMondo2.clear();
        resetShop();
        reset62();
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

        // Ostacoli: solo stanze normali dopo la prima — generati PRIMA dei nemici
        int[][] ostacoli = null;
        if (state.stanzaNelMondo < GameState.STANZA_BOSS && state.stanzaNelMondo > 1) {
            ostacoli = generaOstacoli();
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
    }

    private void generaStanzaCasa(List<ShopItem> items) {
        String pu = state.powerUpCasa;
        int cx = GameState.COL_TOTALI / 2;
        int ry = GameState.OFFSET + GameState.RIG_GIOCO / 2;

        switch (pu) {
            case "VELOCITA" -> {
                ShopItem si = new ShopItem(cx, ry, GameState.TILE_SIZE, "VELOCITA", 0, res.imgItemSpeed);
                si.setMostraPrezzo(false); items.add(si);
            }
            case "DANNO" -> {
                ShopItem si = new ShopItem(cx, ry, GameState.TILE_SIZE, "DANNO", 0, res.imgItemDamage);
                si.setMostraPrezzo(false); items.add(si);
            }
            case "TUTTO" -> {
                ShopItem s1 = new ShopItem(cx - 2, ry, GameState.TILE_SIZE, "VELOCITA", 0, res.imgItemSpeed);
                ShopItem s2 = new ShopItem(cx,     ry, GameState.TILE_SIZE, "DANNO",    0, res.imgItemDamage);
                s1.setMostraPrezzo(false); items.add(s1);
                s2.setMostraPrezzo(false); items.add(s2);
            }
            // CURA e NESSUNO: nessun power-up in Casa
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
        // Burn callback — si attiva quando boss 3 tocca il giocatore durante carica
        b.setOnBurnPlayer(() -> {
            state.burnAttivo = true;
            state.burnTimer  = GameState.BURN_DURATA;
            state.burnTick   = 0;
        });
        // Pugni ref per la schivata del boss 4 — impostati da GameLoop dopo
        if (pugniAttiviRef != null) b.setPugniAttiviRef(pugniAttiviRef);
        nemici.add(b);
        state.bossSpawnato       = true;
        state.bossSconfitto      = false;
        state.tempoRimanenteBoss = GameState.TEMPO_BOSS_DEFAULT;

        // ── Dialogo pre-boss ───────────────────────────────────────────────────
        int tipoBoss = (state.mondoAttuale - 1) % 4; // 0-3
        BufferedImage sprPg   = res.getImgGiocatorePerIndice(state.indicePersonaggioSelezionato);
        BufferedImage sprBoss = res.imgBossPerMondo[tipoBoss];
        String nomePg = state.nomePersonaggioCorrente();

        state.dialogoNarrazione.pulisci();
        switch (tipoBoss) {
            case 0 -> { // MANNIE
                state.dialogoNarrazione.aggiungi(nomePg, sprPg,
                        "Dai, non ho tempo - sono gia in ritardo!",
                        true);
                state.dialogoNarrazione.aggiungi("MANNIE", sprBoss,
                        "Nemmeno io.",
                        false);
            }
            case 1 -> { // PRESAGIO
                state.dialogoNarrazione.aggiungi(nomePg, sprPg,
                        "Quindi e qui che si finisce a forza di giocare ai videogiochi...",
                        true);
            }
            case 2 -> { // RE FORNO
                state.dialogoNarrazione.aggiungi(nomePg, sprPg,
                        "Levati di mezzo. Non mi interessa se sei un re.",
                        true);
                state.dialogoNarrazione.aggiungi(nomePg, sprPg,
                        "Saro io a spodestarti!",
                        true);
                state.dialogoNarrazione.aggiungi("RE FORNO", sprBoss,
                        "*rumori arrabbiati da teglia*",
                        false);
                state.dialogoNarrazione.aggiungi(nomePg, sprPg,
                        "...",
                        true);
            }
            case 3 -> { // YABBADUHLON
                state.dialogoNarrazione.aggiungi("YABBADUHLON", sprBoss,
                        "Si... sono io.",
                        false);
                state.dialogoNarrazione.aggiungi(nomePg, sprPg,
                        "Sei tu il responsabile di tutto questo casino?",
                        true);
                state.dialogoNarrazione.aggiungi(nomePg, sprPg,
                        "Cosa?!",
                        true);
                state.dialogoNarrazione.aggiungi("YABBADUHLON", sprBoss,
                        "So gia tutto quello che stai per dire, fare o qualsiasi cosa tu voglia tentare.",
                        false);
                state.dialogoNarrazione.aggiungi(nomePg, sprPg,
                        "Hai qualche potere?",
                        true);
                state.dialogoNarrazione.aggiungi(nomePg, sprPg,
                        "Beh... il mio cervello sta per fondere a questo ritmo.",
                        true);
                state.dialogoNarrazione.aggiungi("YABBADUHLON", sprBoss,
                        "Non arriverai MAI in ufficio!!!",
                        false);
                state.dialogoNarrazione.aggiungi(nomePg, sprPg,
                        "Chi lo spieghera al mio capo...",
                        true);
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

        for (int i = 0; i < quanti; i++) {
            int[] pos  = trovaPosizioneSicura(ostacoli);
            boolean forte = random.nextFloat() < probForte;

            if (forte) {
                Nemico nf = new NemicoForte(pos[0], pos[1], GameState.TILE_SIZE,
                        StatNemico.vitaNemicoForte(m));
                nf.velocita = StatNemico.velocitaNemicoForte(m);
                nemici.add(nf);
            } else {
                Nemico n = new Nemico(pos[0], pos[1], GameState.TILE_SIZE,
                        StatNemico.vitaNemico(m));
                n.velocita = StatNemico.velocitaNemico(m);
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
            int[] pos = trovaPosizioneSicura(ostacoli);
            boolean forte = random.nextFloat() < 0.5f;

            if (forte) {
                Nemico nf = new NemicoForte(pos[0], pos[1], GameState.TILE_SIZE,
                        StatNemico.vitaNemicoForte(mondoNemico));
                nf.velocita = StatNemico.velocitaNemicoForte(mondoNemico) * 1.2f;
                nemici.add(nf);
            } else {
                Nemico n = new Nemico(pos[0], pos[1], GameState.TILE_SIZE,
                        StatNemico.vitaNemico(mondoNemico));
                n.velocita = StatNemico.velocitaNemico(mondoNemico) * 1.2f;
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
            state.arduaRicompensaMsg   = "MELEE SBLOCCATO!";
            state.arduaRicompensaTimer = 180;
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
            items62.add(drop);
        }

        // 40% chance cura extra in posizione laterale
        if (random.nextFloat() < 0.40f) {
            int[] pos = trovaPosizioneSicura(null);
            cure62.add(new Cura(pos[0], pos[1], GameState.TILE_SIZE, res.imgCura));
        }
    }

    /**
     * Chiamato quando lo shopkeeper viene sconfitto.
     * Se melee gia sbloccato da ardua, buffra il danno pugno invece di sbloccare melee.
     */
    public void onShopkeeperSconfitto() {
        if (!state.meleeUnlocked) {
            state.meleeUnlocked = true;
        } else {
            // Melee gia presente: +3 danno pugno mediamente alto
            state.dannoPugno += 3;
            state.arduaRicompensaMsg   = "+3 DANNO ARMA!";
            state.arduaRicompensaTimer = 180;
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
    }

    /**
     * Torna alla stanza precedente (movimento verso sinistra).
     */
    public void tornaAllaStanzaPrecedente() {
        int maxX = (GameState.COL_TOTALI - GameState.OFFSET - 1) * GameState.TILE_SIZE - 20;
        state.x = maxX;
        state.indiceStanzaMemoria--;
        state.stanzaNelMondo--;
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

    // ── Stanza 6-2 (porta a sud dalla stanza 6) ───────────────────────────────
    public boolean inStanza62 = false;

    private final java.util.List<Nemico>   nemici62  = new java.util.ArrayList<>();
    private final java.util.List<ShopItem> items62   = new java.util.ArrayList<>();
    private final java.util.List<Cura>     cure62    = new java.util.ArrayList<>();
    private boolean stanza62Generata = false;

    public java.util.List<Nemico>   getNemici62()  { return nemici62; }
    public java.util.List<ShopItem> getItems62()   { return items62; }
    public java.util.List<Cura>     getCure62()    { return cure62; }

    public void assicura62Generata() {
        if (stanza62Generata) return;
        nemici62.clear();
        items62.clear();
        cure62.clear();

        // Genera nemici ardui misti da tutti i mondi
        int[][] ostacoli62 = generaOstacoli();
        generaStanzaArdua(nemici62, ostacoli62);
        stanza62Generata = true;
    }

    public void entraIn62() {
        assicura62Generata();
        inStanza62 = true;
        // Entra dall'alto della stanza (viene da sud della stanza 6)
        state.y = GameState.OFFSET * GameState.TILE_SIZE + 20f;
        pugniAttiviRef.clear();
    }

    public void esciDa62() {
        // Rimuovi malus quando si esce
        rimuoviMalusArdua();
        inStanza62 = false;
        // Torna nella stanza 6, in fondo
        state.y = (GameState.RIG_GIOCO) * GameState.TILE_SIZE - GameState.PG_SIZE - 10f;
        pugniAttiviRef.clear();
    }

    public void reset62() {
        stanza62Generata = false;
        nemici62.clear();
        items62.clear();
        cure62.clear();
        inStanza62 = false;
        // Azzera anche malus
        state.arduaMalusDanno    = 0;
        state.arduaMalusVelocita = 0f;
        state.arduaMalusFireRate = 0;
    }

    /**
     * Entra nella stanza shop (porta a nord dalla stanza 1 del nuovo mondo).
     * Memorizza la posizione precedente per il ritorno.
     */
    public void entraNelloShop() {
        inStanzaShop = true;
        state.shopSbloccato = false; // Consumato: non riappare finché non batti il prossimo boss
        state.y = (GameState.RIG_GIOCO) * GameState.TILE_SIZE - GameState.PG_SIZE - 10f; // Entra dal basso della stanza shop
        pugniAttiviRef.clear();
    }

    /**
     * Esce dalla stanza shop (porta a sud, torna alla stanza 1).
     */
    public void esciDalloShop() {
        inStanzaShop = false;
        state.y = GameState.OFFSET * GameState.TILE_SIZE + 20f; // Torna nella stanza 1 in cima
        pugniAttiviRef.clear();
    }

    // Riferimento ai pugni attivi passato da GameLoop per pulirli al cambio stanza shop
    private java.util.List<Pugno> pugniAttiviRef = new java.util.ArrayList<>();
    public void setPugniAttiviRef(java.util.List<Pugno> pugni) { this.pugniAttiviRef = pugni; }

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

    public List<Nemico>     getNemiciCorrenti()      { return nemiciPerStanza.get(state.indiceStanzaMemoria); }
    public List<Cura>       getCureCorrenti()         { return curePerStanza.get(state.indiceStanzaMemoria); }
    public List<Moneta>     getMoneteCorrenti()       { return monetePerStanza.get(state.indiceStanzaMemoria); }
    public List<Shopkeeper> getShopkeepersCorrenti()  { return shopkeepersPerStanza.get(state.indiceStanzaMemoria); }
    public List<ShopItem>   getShopItemsCorrenti()    { return shopItemsPerStanza.get(state.indiceStanzaMemoria); }
    public int[][]          getOstacoliCorrenti() {
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
        int safeX, safeY;
        int pgTileX = GameState.OFFSET + 1;
        int pgTileY = GameState.OFFSET + GameState.RIG_GIOCO / 2;
        int maxTentativi = 300;
        do {
            safeX = random.nextInt(GameState.COL_GIOCO) + GameState.OFFSET;
            safeY = random.nextInt(GameState.RIG_GIOCO) + GameState.OFFSET;
            if (TileSet.isMuro(safeX, safeY)) { maxTentativi--; continue; }
            // Ostacoli passati esplicitamente
            if (isOstacoloIn(safeX, safeY, ostacoli)) { maxTentativi--; continue; }
            // Buffer 5 tile dalla porta sinistra
            if (safeX <= GameState.OFFSET + 4) { maxTentativi--; continue; }
            // Buffer 5 tile dalla porta destra
            if (safeX >= GameState.COL_TOTALI - GameState.OFFSET - 5) { maxTentativi--; continue; }
            // Distanza minima 5 tile dal giocatore
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

    // ── Interfaccia callback ──────────────────────────────────────────────────
    public interface RoomEventListener {
        void onCambioMondo(int nuovoMondo);
        void onVittoriaStoria();
    }
}
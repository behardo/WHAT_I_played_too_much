import java.awt.Color;
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
        memoriaColoriMondo1.clear();
        memoriaColoriMondo2.clear();
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

        if (state.stanzaNelMondo == GameState.STANZA_BOSS && !state.bossSpawnato) {
            generaStanzaBoss(nuoviNemici);

        } else if (state.stanzaNelMondo == GameState.STANZA_BOSS
                && state.bossSpawnato && !state.bossSconfitto) {
            // Boss già spawnato ma non sconfitto: ricrea stanza (torna indietro e rientra)
            generaStanzaBoss(nuoviNemici);

        } else if (state.stanzaNelMondo == 4) {
            generaStanzaShop(nuoviShopkeepers, nuoveItems);

        } else if (state.stanzaNelMondo < GameState.STANZA_BOSS) {
            generaStanzaNormale(nuoviNemici);
        }

        nemiciPerStanza.add(nuoviNemici);
        curePerStanza.add(nuoveCure);
        monetePerStanza.add(nuoveMonete);
        shopkeepersPerStanza.add(nuoviShopkeepers);
        shopItemsPerStanza.add(nuoveItems);
    }

    private void generaStanzaBoss(List<Nemico> nemici) {
        int vitaBoss = calcolaVitaBoss();
        Boss b = new Boss(7, 2, GameState.TILE_SIZE, vitaBoss);
        b.caricaProiettile(res.imgBossProjectile);
        nemici.add(b);
        state.bossSpawnato    = true;
        state.bossSconfitto   = false;
        state.tempoRimanenteBoss = GameState.TEMPO_BOSS_DEFAULT;
    }

    private void generaStanzaShop(List<Shopkeeper> shopkeepers, List<ShopItem> items) {
        shopkeepers.add(new Shopkeeper(7, 1, GameState.TILE_SIZE, res.imgShopkeeper));
        items.add(new ShopItem(5, 2, GameState.TILE_SIZE, "CURA",     2, res.imgCura));
        items.add(new ShopItem(7, 2, GameState.TILE_SIZE, "VELOCITA", 5, res.imgItemSpeed));
        items.add(new ShopItem(9, 2, GameState.TILE_SIZE, "DANNO",    7, res.imgItemDamage));
    }

    private void generaStanzaNormale(List<Nemico> nemici) {
        int quantiBase = random.nextInt(2)
                + (state.stanzaNelMondo / 2)
                + (state.mondoAttuale * 2);
        int vitaNemico = (state.mondoAttuale % 2 == 0) ? 5 : 3;

        if (state.modalitaScelta == GameState.Modalita.INFINITA && state.mondoAttuale > 1) {
            vitaNemico  += ((state.mondoAttuale - 1) / 2) * 2;
            quantiBase  += (state.mondoAttuale - 1);
        }

        for (int i = 0; i < quantiBase; i++) {
            int[] pos = trovaPosizioneSicura();
            int safeX = pos[0];
            int safeY = pos[1];

            if (state.mondoAttuale % 2 != 0) {
                nemici.add(new Nemico(safeX, safeY, GameState.TILE_SIZE, vitaNemico));
            } else {
                if (random.nextFloat() < 0.60f) {
                    nemici.add(new NemicoForte(safeX, safeY, GameState.TILE_SIZE, vitaNemico + 3));
                } else {
                    nemici.add(new Nemico(safeX, safeY, GameState.TILE_SIZE, vitaNemico));
                }
            }
        }
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
    public void avanzaAlMondoSuccessivo() {
        if (state.modalitaScelta == GameState.Modalita.STORIA
                && state.mondoAttuale == GameState.MONDI_STORIA_MAX) {
            state.statoGioco = GameState.StatoGioco.VITTORIA_STORIA;
            if (eventListener != null) eventListener.onVittoriaStoria();
            return;
        }

        state.mondoAttuale++;
        state.stanzaNelMondo      = 1;
        state.indiceStanzaMemoria = 0;
        state.bossSpawnato        = false;
        state.bossSconfitto       = false;

        resetCompleto();
        state.resetGiocatore();

        if (eventListener != null) eventListener.onCambioMondo(state.mondoAttuale);
    }

    // ── Getters stanza corrente ───────────────────────────────────────────────

    public List<Nemico>     getNemiciCorrenti()      { return nemiciPerStanza.get(state.indiceStanzaMemoria); }
    public List<Cura>       getCureCorrenti()         { return curePerStanza.get(state.indiceStanzaMemoria); }
    public List<Moneta>     getMoneteCorrenti()       { return monetePerStanza.get(state.indiceStanzaMemoria); }
    public List<Shopkeeper> getShopkeepersCorrenti()  { return shopkeepersPerStanza.get(state.indiceStanzaMemoria); }
    public List<ShopItem>   getShopItemsCorrenti()    { return shopItemsPerStanza.get(state.indiceStanzaMemoria); }

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
    private int[] trovaPosizioneSicura() {
        int safeX, safeY;
        boolean safe;
        do {
            safeX = random.nextInt(GameState.COL_GIOCO) + GameState.OFFSET;
            safeY = random.nextInt(GameState.RIG_GIOCO) + GameState.OFFSET;
            safe  = true;
            // Zona porta sinistra
            if (safeX >= GameState.OFFSET && safeX <= GameState.OFFSET + 2 && safeY == 3) safe = false;
            // Zona porta destra
            if (safeX >= GameState.COL_TOTALI - 4 && safeX <= GameState.COL_TOTALI - 1 && safeY == 3) safe = false;
        } while (!safe);
        return new int[]{safeX, safeY};
    }

    // ── Interfaccia callback ──────────────────────────────────────────────────
    public interface RoomEventListener {
        void onCambioMondo(int nuovoMondo);
        void onVittoriaStoria();
    }
}
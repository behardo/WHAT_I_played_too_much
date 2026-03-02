import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * UIManager.java
 * Gestisce:
 *  - La lista dei personaggi selezionabili
 *  - Tutti i MenuButton di ogni schermata (menu, pausa, game over, impostazioni, controlli)
 *  - Le hitbox di selezione PG e Modalità
 *
 * Il metodo aggiornaMouse(x,y) va chiamato ogni volta che il mouse si muove,
 * in modo che tutti i bottoni aggiornino il loro stato hover.
 */
public class UIManager {

    // ── Lista personaggi ──────────────────────────────────────────────────────
    public final List<DatiPersonaggio> listaPersonaggi = new ArrayList<>();

    // ── Hitbox selezione personaggio / modalità ───────────────────────────────
    public final Rectangle[] rectsSelezionePG       = new Rectangle[4];
    public final Rectangle[] rectsSelezioneModalita = new Rectangle[2];

    // ── Bottoni Menu Principale ───────────────────────────────────────────────
    public final MenuButton btnGioca;
    public final MenuButton btnImpostazioni;
    public final MenuButton btnControlli;
    public final MenuButton btnEsciMenu;

    // ── Bottoni Pausa ─────────────────────────────────────────────────────────
    public final MenuButton btnRiprendi;
    public final MenuButton btnImpostazioniPausa;
    public final MenuButton btnMenuPrincipalePausa;
    public final MenuButton btnEsciPausa;

    // ── Bottoni Game Over ─────────────────────────────────────────────────────
    public final MenuButton btnRiprova;
    public final MenuButton btnMenuPrincipaleGO;
    public final MenuButton btnEsciGO;

    // ── Bottoni Vittoria ──────────────────────────────────────────────────────
    public final MenuButton btnMenuPrincipaleVittoria;

    // ── Bottoni Impostazioni ──────────────────────────────────────────────────
    public final MenuButton btnMusMeno;
    public final MenuButton btnMusPiu;
    public final MenuButton btnEffMeno;
    public final MenuButton btnEffPiu;
    public final MenuButton btnDifficolta;
    public final MenuButton btnChiudiImpostazioni;

    // ── Bottone Chiudi Controlli ──────────────────────────────────────────────
    public final MenuButton btnChiudiControlli;

    // ── Tutti i bottoni raggruppati per aggiornaMouse ─────────────────────────
    private final MenuButton[][] tuttiBottoni;

    // ─────────────────────────────────────────────────────────────────────────
    public UIManager(ResourceLoader res) {
        inizializzaPersonaggi(res);
        inizializzaRectsSelezionePG();
        inizializzaRectsSelezioneModalita();

        final int W  = GameState.LARGHEZZA_GIOCO;
        final int H  = GameState.ALTEZZA_GIOCO;
        final int BW = 260;
        final int BH = 55;
        final int CX = W / 2 - BW / 2;

        // ── Palette colori ────────────────────────────────────────────────────
        Color normaleDark  = new Color(30,  30,  55);
        Color hoverBlu     = new Color(60,  80,  160);
        Color bordoBlu     = new Color(100, 110, 180);
        Color bordoBluHov  = new Color(160, 180, 255);

        Color normaleRosso = new Color(55,  20,  20);
        Color hoverRosso   = new Color(140, 40,  40);
        Color bordoRosso   = new Color(160, 80,  80);
        Color bordoRossoHv = new Color(220, 100, 100);

        Color normaleVerde = new Color(20,  55,  20);
        Color hoverVerde   = new Color(40,  130, 40);
        Color bordoVerde   = new Color(80,  160, 80);
        Color bordoVerdeHv = new Color(120, 220, 120);

        // ── Menu Principale ───────────────────────────────────────────────────
        int myMenu = H / 2 - 30;
        btnGioca        = btn("GIOCA",        CX, myMenu,       BW, BH, normaleVerde, hoverVerde, bordoVerde, bordoVerdeHv);
        btnImpostazioni = btn("IMPOSTAZIONI", CX, myMenu + 70,  BW, BH, normaleDark,  hoverBlu,   bordoBlu,   bordoBluHov);
        btnControlli    = btn("CONTROLLI",    CX, myMenu + 140, BW, BH, normaleDark,  hoverBlu,   bordoBlu,   bordoBluHov);
        btnEsciMenu     = btn("ESCI",         CX, myMenu + 210, BW, BH, normaleRosso, hoverRosso, bordoRosso, bordoRossoHv);

        // ── Pausa ─────────────────────────────────────────────────────────────
        int myPausa = H / 2 - 10;
        btnRiprendi            = btn("RIPRENDI",     CX, myPausa,       BW, BH, normaleVerde, hoverVerde, bordoVerde, bordoVerdeHv);
        btnImpostazioniPausa   = btn("IMPOSTAZIONI", CX, myPausa + 70,  BW, BH, normaleDark,  hoverBlu,   bordoBlu,   bordoBluHov);
        btnMenuPrincipalePausa = btn("MENU",         CX, myPausa + 140, BW, BH, normaleDark,  hoverBlu,   bordoBlu,   bordoBluHov);
        btnEsciPausa           = btn("ESCI",         CX, myPausa + 210, BW, BH, normaleRosso, hoverRosso, bordoRosso, bordoRossoHv);

        // ── Game Over ─────────────────────────────────────────────────────────
        int bwGO = 200;
        int myGO = H / 2 + 50;
        int gap  = 15;
        int tot  = bwGO * 3 + gap * 2;
        int x0   = W / 2 - tot / 2;
        btnRiprova          = btn("RIPROVA", x0,              myGO, bwGO, BH, normaleVerde, hoverVerde, bordoVerde, bordoVerdeHv);
        btnMenuPrincipaleGO = btn("MENU",    x0 + bwGO + gap, myGO, bwGO, BH, normaleDark,  hoverBlu,   bordoBlu,   bordoBluHov);
        btnEsciGO           = btn("ESCI",    x0+bwGO*2+gap*2, myGO, bwGO, BH, normaleRosso, hoverRosso, bordoRosso, bordoRossoHv);

        // ── Vittoria ──────────────────────────────────────────────────────────
        btnMenuPrincipaleVittoria = btn("MENU PRINCIPALE", W/2 - 150, H/2 + 100, 300, BH,
                normaleDark, hoverBlu, bordoBlu, bordoBluHov);

        // ── Impostazioni ──────────────────────────────────────────────────────
        int sx = W / 2 + 30;
        int sy1 = H / 2 - 80;
        int sy2 = H / 2;
        int sw  = 50, sh = 44;
        btnMusMeno = btn("-", sx,      sy1, sw, sh, normaleRosso, hoverRosso, bordoRosso, bordoRossoHv);
        btnMusPiu  = btn("+", sx+sw+5, sy1, sw, sh, normaleVerde, hoverVerde, bordoVerde, bordoVerdeHv);
        btnEffMeno = btn("-", sx,      sy2, sw, sh, normaleRosso, hoverRosso, bordoRosso, bordoRossoHv);
        btnEffPiu  = btn("+", sx+sw+5, sy2, sw, sh, normaleVerde, hoverVerde, bordoVerde, bordoVerdeHv);
        btnDifficolta         = btn("NORMALE", W/2-120, H/2+80,  240, sh, normaleDark, hoverBlu, bordoBlu, bordoBluHov);
        btnChiudiImpostazioni = btn("INDIETRO", W/2-100, H/2+160, 200, BH, normaleDark, hoverBlu, bordoBlu, bordoBluHov);

        // ── Controlli ─────────────────────────────────────────────────────────
        btnChiudiControlli = btn("INDIETRO", W/2-100, H-110, 200, BH, normaleDark, hoverBlu, bordoBlu, bordoBluHov);

        // ── Raggruppamento per hover ───────────────────────────────────────────
        tuttiBottoni = new MenuButton[][] {
                { btnGioca, btnImpostazioni, btnControlli, btnEsciMenu },
                { btnRiprendi, btnImpostazioniPausa, btnMenuPrincipalePausa, btnEsciPausa },
                { btnRiprova, btnMenuPrincipaleGO, btnEsciGO },
                { btnMenuPrincipaleVittoria },
                { btnMusMeno, btnMusPiu, btnEffMeno, btnEffPiu, btnDifficolta, btnChiudiImpostazioni },
                { btnChiudiControlli }
        };
    }

    // ── Factory helper ────────────────────────────────────────────────────────
    private MenuButton btn(String label, int x, int y, int w, int h,
                           Color norm, Color hov, Color bNorm, Color bHov) {
        return new MenuButton(label, x, y, w, h).setColori(norm, hov, bNorm, bHov);
    }

    // ── Hover update ─────────────────────────────────────────────────────────
    public void aggiornaMouse(int mouseX, int mouseY) {
        for (MenuButton[] gruppo : tuttiBottoni) {
            for (MenuButton btn : gruppo) {
                btn.aggiornaMouse(mouseX, mouseY);
            }
        }
    }

    // ── Inizializzazione ──────────────────────────────────────────────────────

    private void inizializzaPersonaggi(ResourceLoader res) {
        listaPersonaggi.add(new DatiPersonaggio("BELLGERD", 3, 6.0f, 1,
                res.getIconaPerIndice(0), res.imgPersonaggioDefault,  "Equilibrato."));
        listaPersonaggi.add(new DatiPersonaggio("VLAD",     2, 8.5f, 1,
                res.getIconaPerIndice(1), res.imgPersonaggioVeloce,   "Veloce ma fragile."));
        listaPersonaggi.add(new DatiPersonaggio("PAUL",     3, 4.5f, 2,
                res.getIconaPerIndice(2), res.imgPersonaggioForte,    "Lento ma potente."));
        listaPersonaggi.add(new DatiPersonaggio("JUICY",    5, 3.5f, 1,
                res.getIconaPerIndice(3), res.imgPersonaggioTank,     "Lentissimo, molta vita."));
    }

    private void inizializzaRectsSelezionePG() {
        int startX = GameState.LARGHEZZA_GIOCO / 2 - 320;
        int startY = GameState.ALTEZZA_GIOCO   / 2 - 100;
        int rectW = 150, rectH = 200, gap = 10;
        for (int i = 0; i < 4; i++) {
            rectsSelezionePG[i] = new Rectangle(startX + i * (rectW + gap), startY, rectW, rectH);
        }
    }

    private void inizializzaRectsSelezioneModalita() {
        int startX = GameState.LARGHEZZA_GIOCO / 2 - 250;
        int startY = GameState.ALTEZZA_GIOCO   / 2 - 120;
        int rectW = 240, rectH = 240, gap = 20;
        for (int i = 0; i < 2; i++) {
            rectsSelezioneModalita[i] = new Rectangle(startX + i * (rectW + gap), startY, rectW, rectH);
        }
    }

    // ── Accesso ───────────────────────────────────────────────────────────────
    public DatiPersonaggio getPersonaggioSelezionato(int i) { return listaPersonaggi.get(i); }
    public int             getNumPersonaggi()               { return listaPersonaggi.size(); }
}
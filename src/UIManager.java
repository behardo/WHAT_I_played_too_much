import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * UIManager.java
 * Tutti i MenuButton di ogni schermata con posizioni calcolate
 * per lo schermo logico 1088x448px.
 */
public class UIManager {

    public final List<DatiPersonaggio> listaPersonaggi       = new ArrayList<>();
    public final Rectangle[]           rectsSelezionePG      = new Rectangle[5];
    public final Rectangle[]           rectsSelezioneModalita = new Rectangle[2];

    // ── Bottoni ───────────────────────────────────────────────────────────────
    public final MenuButton btnGioca, btnImpostazioni, btnControlli, btnEsciMenu;
    public final MenuButton btnRiprendi, btnImpostazioniPausa, btnMenuPrincipalePausa, btnEsciPausa;
    public final MenuButton btnRiprova, btnMenuPrincipaleGO, btnEsciGO;
    public final MenuButton btnMenuPrincipaleVittoria;
    public final MenuButton btnMusMeno, btnMusPiu, btnEffMeno, btnEffPiu;
    public final MenuButton btnDifficolta, btnChiudiImpostazioni;
    public final MenuButton btnChiudiControlli;

    // Parametri layout impostazioni usati da RenderEngine per allineare testo/slider
    public int _impLabelX, _impCtrlX, _impSw, _impStartY, _impRigaH, _impSh;

    private final MenuButton[][] tuttiBottoni;

    // ─────────────────────────────────────────────────────────────────────────
    public UIManager(ResourceLoader res) {
        inizializzaPersonaggi(res);
        inizializzaRectsSelezionePG();
        inizializzaRectsSelezioneModalita();

        final int W   = GameState.LARGHEZZA_GIOCO;   // 1088
        final int H   = GameState.ALTEZZA_GIOCO;     // 448
        final int BW  = 250;
        final int BH  = 44;
        final int GAP = 10;
        final int CX  = W / 2 - BW / 2;

        // ── Palette colori ────────────────────────────────────────────────────
        Color nDark  = new Color(30,  30,  55);
        Color hBlu   = new Color(60,  80, 160);
        Color bBlu   = new Color(100, 110, 180);
        Color bBluH  = new Color(160, 180, 255);
        Color nRosso = new Color(55,  20,  20);
        Color hRosso = new Color(140, 40,  40);
        Color bRosso = new Color(160, 80,  80);
        Color bRosH  = new Color(220, 100, 100);
        Color nVerde = new Color(20,  55,  20);
        Color hVerde = new Color(40,  130, 40);
        Color bVerde = new Color(80,  160, 80);
        Color bVerH  = new Color(120, 220, 120);

        // ── Menu Principale ───────────────────────────────────────────────────
        {
            int s = 170;
            btnGioca        = b("GIOCA",        CX, s,             BW, BH, nVerde, hVerde, bVerde, bVerH);
            btnImpostazioni = b("IMPOSTAZIONI", CX, s+(BH+GAP),   BW, BH, nDark,  hBlu,   bBlu,   bBluH);
            btnControlli    = b("CONTROLLI",    CX, s+(BH+GAP)*2, BW, BH, nDark,  hBlu,   bBlu,   bBluH);
            btnEsciMenu     = b("ESCI",         CX, s+(BH+GAP)*3, BW, BH, nRosso, hRosso, bRosso, bRosH);
        }

        // ── Pausa ─────────────────────────────────────────────────────────────
        {
            int s = 154;
            btnRiprendi            = b("RIPRENDI",     CX, s,             BW, BH, nVerde, hVerde, bVerde, bVerH);
            btnImpostazioniPausa   = b("IMPOSTAZIONI", CX, s+(BH+GAP),   BW, BH, nDark,  hBlu,   bBlu,   bBluH);
            btnMenuPrincipalePausa = b("MENU",         CX, s+(BH+GAP)*2, BW, BH, nDark,  hBlu,   bBlu,   bBluH);
            btnEsciPausa           = b("ESCI",         CX, s+(BH+GAP)*3, BW, BH, nRosso, hRosso, bRosso, bRosH);
        }

        // ── Game Over ─────────────────────────────────────────────────────────
        {
            int bw3 = 185, g3 = 12;
            int x0  = W/2 - (bw3*3 + g3*2)/2;
            int y0  = 265;
            btnRiprova          = b("RIPROVA", x0,            y0, bw3, BH, nVerde, hVerde, bVerde, bVerH);
            btnMenuPrincipaleGO = b("MENU",    x0+bw3+g3,     y0, bw3, BH, nDark,  hBlu,   bBlu,   bBluH);
            btnEsciGO           = b("ESCI",    x0+(bw3+g3)*2, y0, bw3, BH, nRosso, hRosso, bRosso, bRosH);
        }

        // ── Vittoria ──────────────────────────────────────────────────────────
        btnMenuPrincipaleVittoria = b("MENU PRINCIPALE",
                W/2 - 150, H*3/4 - BH/2, 300, BH, nDark, hBlu, bBlu, bBluH);

        // ── Impostazioni ──────────────────────────────────────────────────────
        {
            final int SH  = 38;
            final int RH  = 55;
            final int SW  = 48;
            final int LX  = W/2 - 220;
            final int CX2 = W/2 + 5;
            int s = 145;

            btnMusMeno            = b("-",        CX2,         s,         SW,  SH, nRosso, hRosso, bRosso, bRosH);
            btnMusPiu             = b("+",        CX2+SW+155,  s,         SW,  SH, nVerde, hVerde, bVerde, bVerH);
            btnEffMeno            = b("-",        CX2,         s+RH,      SW,  SH, nRosso, hRosso, bRosso, bRosH);
            btnEffPiu             = b("+",        CX2+SW+155,  s+RH,      SW,  SH, nVerde, hVerde, bVerde, bVerH);
            btnDifficolta         = b("NORMALE",  W/2-110,     s+RH*2,    220, SH, nDark,  hBlu,   bBlu,   bBluH);
            btnChiudiImpostazioni = b("INDIETRO", W/2-100,     s+RH*3+10, 200, BH, nDark,  hBlu,   bBlu,   bBluH);

            _impLabelX = LX;   _impCtrlX = CX2;  _impSw = SW;
            _impStartY = s;    _impRigaH = RH;   _impSh = SH;
        }

        // ── Controlli ─────────────────────────────────────────────────────────
        btnChiudiControlli = b("INDIETRO", W/2-100, H-75, 200, BH, nDark, hBlu, bBlu, bBluH);

        tuttiBottoni = new MenuButton[][] {
                { btnGioca, btnImpostazioni, btnControlli, btnEsciMenu },
                { btnRiprendi, btnImpostazioniPausa, btnMenuPrincipalePausa, btnEsciPausa },
                { btnRiprova, btnMenuPrincipaleGO, btnEsciGO },
                { btnMenuPrincipaleVittoria },
                { btnMusMeno, btnMusPiu, btnEffMeno, btnEffPiu, btnDifficolta, btnChiudiImpostazioni },
                { btnChiudiControlli }
        };
    }

    // ── Factory ───────────────────────────────────────────────────────────────
    private MenuButton b(String label, int x, int y, int w, int h,
                         Color n, Color hov, Color bn, Color bh) {
        return new MenuButton(label, x, y, w, h).setColori(n, hov, bn, bh);
    }

    // ── Hover update ─────────────────────────────────────────────────────────
    public void aggiornaMouse(int mx, int my) {
        for (MenuButton[] g : tuttiBottoni)
            for (MenuButton btn : g)
                btn.aggiornaMouse(mx, my);
    }

    // ── Inizializzazione ──────────────────────────────────────────────────────

    private void inizializzaPersonaggi(ResourceLoader res) {
        listaPersonaggi.add(new DatiPersonaggio("BELLGERD", 3, 6.0f, 1,
                res.getIconaPerIndice(0), res.imgPersonaggioDefault, "Equilibrato."));
        listaPersonaggi.add(new DatiPersonaggio("VLAD",     2, 8.5f, 1,
                res.getIconaPerIndice(1), res.imgPersonaggioVeloce,  "Veloce ma fragile."));
        listaPersonaggi.add(new DatiPersonaggio("PAUL",     3, 4.5f, 2,
                res.getIconaPerIndice(2), res.imgPersonaggioForte,   "Lento ma potente."));
        listaPersonaggi.add(new DatiPersonaggio("JUICY",    5, 3.5f, 1,
                res.getIconaPerIndice(3), res.imgPersonaggioTank,    "Lentissimo, molta vita."));
        // Segreto — indice 4, visibile solo con combo B x5
        listaPersonaggi.add(new DatiPersonaggio(
                "G.O.D.", 99, 12.0f, 25,
                null,
                res.imgPersonaggioDefault,
                "???"));
    }

    private void inizializzaRectsSelezionePG() {
        // 5 card da 128x200px, gap 10 → tot 680px, startX = (1088-680)/2 = 204
        int rectW = 128, rectH = 200, gap = 10;
        int startX = GameState.LARGHEZZA_GIOCO / 2 - (rectW * 5 + gap * 4) / 2;
        for (int i = 0; i < 5; i++)
            rectsSelezionePG[i] = new Rectangle(startX + i * (rectW + gap), 100, rectW, rectH);
    }

    private void inizializzaRectsSelezioneModalita() {
        // 2 card da 260x240px, gap 20 → tot 540px, startX = (1088-540)/2 = 274
        int rectW = 260, rectH = 240, gap = 20;
        int startX = GameState.LARGHEZZA_GIOCO / 2 - (rectW * 2 + gap) / 2;
        for (int i = 0; i < 2; i++)
            rectsSelezioneModalita[i] = new Rectangle(startX + i * (rectW + gap), 124, rectW, rectH);
    }

    // ── Accesso ───────────────────────────────────────────────────────────────
    public DatiPersonaggio getPersonaggioSelezionato(int i) { return listaPersonaggi.get(i); }
    public int             getNumPersonaggi()               { return listaPersonaggi.size(); }
}
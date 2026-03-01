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

    public final List<DatiPersonaggio> listaPersonaggi    = new ArrayList<>();
    public final Rectangle[]           rectsSelezionePG   = new Rectangle[4];
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

        // Schermo logico: W=1088  H=448
        final int W   = GameState.LARGHEZZA_GIOCO;   // 1088
        final int H   = GameState.ALTEZZA_GIOCO;     // 448
        final int BW  = 250;  // larghezza bottone standard
        final int BH  = 44;   // altezza bottone
        final int GAP = 10;   // spazio verticale tra bottoni
        final int CX  = W / 2 - BW / 2;  // 419 — centrato

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

        // ─────────────────────────────────────────────────────────────────────
        // MENU PRINCIPALE
        // Titolo: Y=60–90. Sottotitolo: Y=105. Separatore: Y=118.
        // Area disponibile per bottoni: 118..428 = 310px
        // Blocco 4 btn: 4*44 + 3*10 = 206px
        // startY = 118 + (310-206)/2 = 170
        // ─────────────────────────────────────────────────────────────────────
        {
            int s = 170;
            btnGioca        = b("GIOCA",        CX, s,              BW, BH, nVerde, hVerde, bVerde, bVerH);
            btnImpostazioni = b("IMPOSTAZIONI", CX, s+(BH+GAP),    BW, BH, nDark,  hBlu,   bBlu,   bBluH);
            btnControlli    = b("CONTROLLI",    CX, s+(BH+GAP)*2,  BW, BH, nDark,  hBlu,   bBlu,   bBluH);
            btnEsciMenu     = b("ESCI",         CX, s+(BH+GAP)*3,  BW, BH, nRosso, hRosso, bRosso, bRosH);
        }

        // ─────────────────────────────────────────────────────────────────────
        // PAUSA (overlay su gioco)
        // Pannello: altezza 300px, centrato → py = H/2-150 = 74
        // Titolo "PAUSA" dentro pannello a py+45=119
        // Bottoni partono da py+80 = 154, blocco 206px → fine a 360
        // ─────────────────────────────────────────────────────────────────────
        {
            int s = 154;
            btnRiprendi            = b("RIPRENDI",     CX, s,             BW, BH, nVerde, hVerde, bVerde, bVerH);
            btnImpostazioniPausa   = b("IMPOSTAZIONI", CX, s+(BH+GAP),   BW, BH, nDark,  hBlu,   bBlu,   bBluH);
            btnMenuPrincipalePausa = b("MENU",         CX, s+(BH+GAP)*2, BW, BH, nDark,  hBlu,   bBlu,   bBluH);
            btnEsciPausa           = b("ESCI",         CX, s+(BH+GAP)*3, BW, BH, nRosso, hRosso, bRosso, bRosH);
        }

        // ─────────────────────────────────────────────────────────────────────
        // GAME OVER
        // Titolo: Y=H/2-55=169. Stats: Y=H/2+10=234, +35=259.
        // 3 bottoni affiancati a Y=265.
        // Larghezza tot: 3*185+2*12 = 579px → x0 = (1088-579)/2 = 254
        // Fine bottoni: 265+44 = 309 < 448 ✓
        // ─────────────────────────────────────────────────────────────────────
        {
            int bw3 = 185, g3 = 12;
            int x0  = W/2 - (bw3*3 + g3*2)/2;  // 254
            int y0  = 265;
            btnRiprova          = b("RIPROVA", x0,            y0, bw3, BH, nVerde, hVerde, bVerde, bVerH);
            btnMenuPrincipaleGO = b("MENU",    x0+bw3+g3,     y0, bw3, BH, nDark,  hBlu,   bBlu,   bBluH);
            btnEsciGO           = b("ESCI",    x0+(bw3+g3)*2, y0, bw3, BH, nRosso, hRosso, bRosso, bRosH);
        }

        // ─────────────────────────────────────────────────────────────────────
        // VITTORIA
        // Testo titolo: H/2-48=176. Stats: H/2+25=249, +60=284.
        // Bottone: H*3/4 - BH/2 = 336-22 = 314 ✓
        // ─────────────────────────────────────────────────────────────────────
        btnMenuPrincipaleVittoria = b("MENU PRINCIPALE",
                W/2 - 150, H*3/4 - BH/2,
                300, BH, nDark, hBlu, bBlu, bBluH);

        // ─────────────────────────────────────────────────────────────────────
        // IMPOSTAZIONI
        // Titolo: Y=70. Separatore: Y=82.
        // 3 righe (SH=38, RH=55) + bottone INDIETRO (BH=44) + gap 10
        // Blocco: 3*55 + 44 + 10 = 219px
        // startY = 82 + (H-82-20-219)/2 = 82 + 63 = 145
        // Fine: 145+219 = 364 < 448 ✓
        // ─────────────────────────────────────────────────────────────────────
        {
            final int SH   = 38;
            final int RH   = 55;
            final int SW   = 48;
            final int LX   = W/2 - 220;
            final int CX2  = W/2 + 5;
            int s = 145;

            btnMusMeno = b("-", CX2,       s,      SW, SH, nRosso, hRosso, bRosso, bRosH);
            btnMusPiu  = b("+", CX2+SW+155, s,     SW, SH, nVerde, hVerde, bVerde, bVerH);
            btnEffMeno = b("-", CX2,       s+RH,   SW, SH, nRosso, hRosso, bRosso, bRosH);
            btnEffPiu  = b("+", CX2+SW+155, s+RH,  SW, SH, nVerde, hVerde, bVerde, bVerH);
            btnDifficolta         = b("NORMALE",  W/2-110, s+RH*2,    220, SH, nDark, hBlu, bBlu, bBluH);
            btnChiudiImpostazioni = b("INDIETRO", W/2-100, s+RH*3+10, 200, BH, nDark, hBlu, bBlu, bBluH);

            _impLabelX = LX;  _impCtrlX = CX2;  _impSw = SW;
            _impStartY = s;   _impRigaH = RH;   _impSh = SH;
        }

        // ─────────────────────────────────────────────────────────────────────
        // CONTROLLI
        // 4 righe di tasti: altezza 4*52=208px, startY=120, fine 328.
        // Bottone indietro: Y=H-75=373 ✓
        // ─────────────────────────────────────────────────────────────────────
        btnChiudiControlli = b("INDIETRO", W/2-100, H-75, 200, BH,
                nDark, hBlu, bBlu, bBluH);

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
        listaPersonaggi.add(new DatiPersonaggio("BELLGERD", 13, 20.0f, 99,
                res.getIconaPerIndice(0), res.imgPersonaggioDefault,  "Equilibrato."));
        listaPersonaggi.add(new DatiPersonaggio("VLAD",     2, 8.5f, 1,
                res.getIconaPerIndice(1), res.imgPersonaggioVeloce,   "Veloce ma fragile."));
        listaPersonaggi.add(new DatiPersonaggio("PAUL",     3, 4.5f, 2,
                res.getIconaPerIndice(2), res.imgPersonaggioForte,    "Lento ma potente."));
        listaPersonaggi.add(new DatiPersonaggio("JUICY",    5, 3.5f, 1,
                res.getIconaPerIndice(3), res.imgPersonaggioTank,     "Lentissimo, molta vita."));
    }

    private void inizializzaRectsSelezionePG() {
        // 4 card da 155x210px, totale 4*155+3*12 = 656px, startX = (1088-656)/2 = 216
        int rectW = 155, rectH = 210, gap = 12;
        int tot   = rectW * 4 + gap * 3;                      // 656
        int startX = GameState.LARGHEZZA_GIOCO / 2 - tot / 2; // 216
        // Centrato verticalmente con intestazione: startY = 100
        int startY = 100;
        for (int i = 0; i < 4; i++)
            rectsSelezionePG[i] = new Rectangle(startX + i*(rectW+gap), startY, rectW, rectH);
    }

    private void inizializzaRectsSelezioneModalita() {
        // 2 card da 260x240px, gap 20px, totale 540px, startX = (1088-540)/2 = 274
        int rectW = 260, rectH = 240, gap = 20;
        int tot   = rectW * 2 + gap;                           // 540
        int startX = GameState.LARGHEZZA_GIOCO / 2 - tot / 2; // 274
        // Centrato: startY = (448-240)/2 + 20 = 124 (spazio per header)
        int startY = 124;
        for (int i = 0; i < 2; i++)
            rectsSelezioneModalita[i] = new Rectangle(startX + i*(rectW+gap), startY, rectW, rectH);
    }

    public DatiPersonaggio getPersonaggioSelezionato(int i) { return listaPersonaggi.get(i); }
    public int             getNumPersonaggi()               { return listaPersonaggi.size(); }
}
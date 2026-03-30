package data;

/**
 * Lang.java — Sistema di localizzazione IT / EN
 * Usato da: RenderEngine, UIManager, RoomManager, GameLoop
 *
 * Uso:  Lang.t("chiave")   →  stringa nella lingua corrente
 *       Lang.setLingua(Lingua.EN)
 */
public class Lang {

    public enum Lingua { IT, EN }

    /** Lingua corrente — modificata dal bottone nel menu */
    public static Lingua lingua = Lingua.IT;

    /** Traduce una chiave nella lingua corrente. */
    public static String t(String chiave) {
        if (lingua == Lingua.EN) {
            String en = EN(chiave);
            return en != null ? en : chiave; // fallback: chiave stessa
        }
        String it = IT(chiave);
        return it != null ? it : chiave;
    }

    /** Shortcut: true se lingua è EN */
    public static boolean isEN() { return lingua == Lingua.EN; }

    // ── Italiano ─────────────────────────────────────────────────────────────
    private static String IT(String k) {
        return switch (k) {

            // ── Menu principale ──────────────────────────────────────────────
            case "btn.gioca"          -> "GIOCA";
            case "btn.impostazioni"   -> "IMPOSTAZIONI";
            case "btn.controlli"      -> "CONTROLLI";
            case "btn.esci"           -> "ESCI";
            case "btn.lingua"         -> "LINGUA";
            case "btn.menu.principale"   -> "MENU PRINCIPALE";
            case "footer.fullscreen"  -> "F11 = Schermo intero";

            // ── Pausa ────────────────────────────────────────────────────────
            case "pausa.titolo"       -> "PAUSA";
            case "btn.riprendi"       -> "RIPRENDI";
            case "btn.menu"           -> "MENU";
            case "btn.riprova"        -> "RIPROVA";

            // ── Impostazioni ─────────────────────────────────────────────────
            case "imp.titolo"         -> "IMPOSTAZIONI";
            case "imp.musica"         -> "MUSICA";
            case "imp.effetti"        -> "EFFETTI";
            case "btn.indietro"       -> "INDIETRO";

            // ── Controlli ────────────────────────────────────────────────────
            case "ctrl.titolo"        -> "CONTROLLI";
            case "ctrl.sezione.gioco" -> "GIOCO";
            case "ctrl.sezione.tetris"-> "TETRIS  (stanza iniziale)";
            case "ctrl.wasd"          -> "Movimento";
            case "ctrl.frecce"        -> "Sparo direzionale";
            case "ctrl.z"             -> "Attacco corpo a corpo";
            case "ctrl.esc"           -> "Pausa";
            case "ctrl.f11"           -> "Schermo intero";
            case "ctrl.click"         -> "Interagisci / Acquista";
            case "ctrl.invio"         -> "Conferma dialogo";
            case "ctrl.q"             -> "Esci (in pausa)";
            case "ctrl.ad"            -> "Muovi pezzo";
            case "ctrl.w"             -> "Ruota pezzo";
            case "ctrl.s"             -> "Scendi veloce";
            case "ctrl.spazio"        -> "Caduta istantanea";
            case "ctrl.escsalta"      -> "Salta Tetris";
            case "ctrl.punteggi"      -> "CURA | 1500: VEL | 3000: DANNO | 5000: MELEE | 6000: TUTTO";

            // ── Selezione personaggio ─────────────────────────────────────────
            case "pg.scegli"          -> "SCEGLI IL TUO PERSONAGGIO";
            case "pg.hint"            -> "[Frecce/Mouse] naviga    [INVIO/Click] conferma    [ESC] indietro";

            // ── Selezione modalità ────────────────────────────────────────────
            case "mod.seleziona"      -> "SELEZIONA LA SFIDA";
            case "mod.storia"         -> "STORIA CLASSICA";
            case "mod.infinita"       -> "MODALITA INFINITA";
            case "mod.hint"           -> "[Frecce/Mouse] naviga    [INVIO/Click] conferma    [ESC] indietro";

            // ── Game Over ─────────────────────────────────────────────────────
            case "go.titolo"          -> "NON CE L'HAI FATTA.";
            case "go.sub"             -> "L'ufficio aspettera ancora.";
            case "go.stats"           -> "Mondo %d  |  Stanza %d  |  Monete %d";
            case "go.stanze"          -> "Stanze totali: %d";

            // ── Vittoria ──────────────────────────────────────────────────────
            case "win.titolo"         -> "SEI ARRIVATO.";
            case "win.sub.ritardo"    -> "Con un po' di ritardo.";
            case "win.sub.molto"      -> "Con ritardo.";
            case "win.monete"         -> "Monete guadagnate: %d";
            case "win.codice"         -> "Codice trovato: %s";

            // ── Ufficio ───────────────────────────────────────────────────────
            case "ufficio.banner"     -> "UFFICIO";
            case "ufficio.continua"   -> "[ INVIO per continuare ]";

            // ── HUD ───────────────────────────────────────────────────────────
            case "hud.stanza"         -> "Stanza %d/%d";
            case "hud.mondo"          -> "M%d";
            case "hud.bonus"          -> "BONUS";
            case "hud.freeze"         -> "FREEZE";
            case "hud.lento"          -> "LENTO";

            // ── Tetris ────────────────────────────────────────────────────────
            case "tet.logo"           -> "WHAT TETRIS";
            case "tet.prossimo"       -> "PROSSIMO";
            case "tet.premio"         -> "PREMIO";
            case "tet.tempo"          -> "TEMPO";
            case "tet.istr"           -> "A/D muovi   W ruota   S scendi   SPAZIO caduta   ESC salta";
            case "tet.gameover"       -> "GAME OVER";
            case "tet.timeout"        -> "TEMPO SCADUTO!";
            case "tet.punteggio"      -> "Punteggio: %d";
            case "tet.hint"           -> "INVIO per continuare";
            case "tet.cura"           -> "+VITA";
            case "tet.velocita"       -> "+VEL";
            case "tet.danno"          -> "+DANNO";
            case "tet.tutto"          -> "TUTTO!";
            case "tet.next.vel"       -> "3000: DANNO";
            case "tet.next.danno"     -> "5000: MELEE";
            case "tet.next.melee"     -> "6000: TUTTO";
            case "tet.next.cura"      -> "1500: VEL";

            // ── Negozio ───────────────────────────────────────────────────────
            case "shop.banner"        -> "NEGOZIO";
            case "shop.nome"          -> "NEGOZIANTE";
            case "shop.domanda"       -> "Vuoi attaccare il negoziante?";
            case "shop.avviso"        -> "Otterrai 20 monete, ma perderai lo shop.";
            case "shop.hint"          -> "A/D per scegliere   INVIO per confermare   ESC per annullare";

            // ── Dialogo JRPG ─────────────────────────────────────────────────
            case "dial.continua"      -> "[ INVIO per continuare ]";
            case "dial.inizia"        -> "[ INVIO per iniziare ]";

            // ── Nota di servizio ──────────────────────────────────────────────
            case "nota.titolo"        -> "NOTA DI SERVIZIO";
            case "nota.firma"         -> "- Il Capo";
            case "nota.chiudi"        -> "[ INVIO per chiudere ]";

            // ── Banner stanze ─────────────────────────────────────────────────
            case "banner.casa"        -> "CASA";

            // ── Popup ricompense tile ─────────────────────────────────────────
            case "popup.veleno"       -> "VELENO!";
            case "popup.congelato"    -> "CONGELATO!";
            case "popup.cannone"      -> "CANNONE!";
            case "popup.melee"        -> "MELEE SBLOCCATO!";
            case "popup.danno"        -> "+3 DANNO ARMA!";
            case "popup.vita"         -> "+1 SLOT VITA!";
            case "popup.velocita"     -> "+1.5 VELOCITA!";
            case "popup.danno2"       -> "+2 DANNO!";
            case "popup.firerate"     -> "FIRE RATE UP!";

            // ── Modalità selezione desc ───────────────────────────────────────
            case "mod.s.desc"         -> "S";
            case "mod.inf.desc"       -> "INF";

            // ── Schermata intro casa ──────────────────────────────────────────
            case "casa.intro"         -> "WHAT? I'VE PLAYED TOO MUCH, IM GONNA BE LATE!!!";


            // ── Descrizioni personaggi ────────────────────────────────────────
            case "pg.bellgerd.desc"   -> "Tuttofare. Niente di speciale.";
            case "pg.vlad.desc"       -> "Veloce. Molto veloce. Troppo.";
            case "pg.paul.desc"       -> "Lento. Ogni colpo vale tre.";
            case "pg.juicy.desc"      -> "Non si ferma. Mai.";
            case "pg.ditto.desc"      -> "???";

            // ── Sblocco personaggi ────────────────────────────────────────────
            case "pg.sblocco.1.r1"    -> "Sconfiggi il Boss";
            case "pg.sblocco.1.r2"    -> "del Mondo 1";
            case "pg.sblocco.2.r1"    -> "Sconfiggi il Boss";
            case "pg.sblocco.2.r2"    -> "del Mondo 2";
            case "pg.sblocco.3.r1"    -> "Sconfiggi il Boss";
            case "pg.sblocco.3.r2"    -> "del Mondo 3";
            case "pg.sblocco.4.r1"    -> "???";
            case "pg.sblocco.4.r2"    -> "";

            // ── Shop item label ───────────────────────────────────────────────
            case "item.cura.label"    -> "CURA";
            case "item.vel.label"     -> "VEL";
            case "item.danno.label"   -> "DMG";

            // ── Nota di servizio corpo ────────────────────────────────────────
            case "nota.riga0"  -> "A chiunque trovi questo foglio,";
            case "nota.riga1"  -> "";
            case "nota.riga2"  -> "Se il sistema smette di rispondere,";
            case "nota.riga3"  -> "usa questo codice per il pannello";
            case "nota.riga4"  -> "di diagnostica:";
            case "nota.riga5"  -> "";

            // ── Dialoghi boss ─────────────────────────────────────────────────
            case "boss.m1.pg0"  -> "Dai, non ho tempo - sono gia in ritardo!";
            case "boss.m1.b0"   -> "Nemmeno io.";
            case "boss.m2.pg0"  -> "Quindi e qui che si finisce a forza di giocare ai videogiochi...";
            case "boss.m3.pg0"  -> "Levati di mezzo. Non mi interessa se sei un re.";
            case "boss.m3.pg1"  -> "Saro io a spodestarti!";
            case "boss.m3.b0"   -> "*rumori arrabbiati da teglia*";
            case "boss.m3.pg2"  -> "...";
            case "boss.m4.pg0"  -> "Che freddo. Come si vive qua?";
            case "boss.m4.b0"   -> "Non ci si vive. Ci si congela.";
            case "boss.m4.pg1"  -> "Simpatico.";
            case "boss.m5.b0"   -> "Si... sono io.";
            case "boss.m5.pg0"  -> "Sei tu il responsabile di tutto questo casino?";
            case "boss.m5.pg1"  -> "Cosa?!";
            case "boss.m5.b1"   -> "So gia tutto quello che stai per dire, fare o qualsiasi cosa tu voglia tentare.";
            case "boss.m5.pg2"  -> "Hai qualche potere?";
            case "boss.m5.pg3"  -> "Beh... il mio cervello sta per fondere a questo ritmo.";
            case "boss.m5.b2"   -> "Non arriverai MAI in ufficio!!!";
            case "boss.m5.pg4"  -> "Chi lo spieghera al mio capo...";

  
  
            // ── Stanza bonus panel ────────────────────────────────────────────
            case "bonus.titolo"     -> "STANZA BONUS";
            case "bonus.danno"      -> "DANNO";
            case "bonus.velocita"   -> "VELOCITA";
            case "bonus.fuocolento" -> "FUOCO LENTO";
            case "bonus.nessunmalus"-> "nessun malus";

            // ── Attacchi melee ────────────────────────────────────────────────
            case "melee.0"       -> "COLPO DI VALIGIA";
            case "melee.1"       -> "CHIAVE INGLESE";
            case "melee.2"       -> "ACCETTA";
            case "melee.3"       -> "COLPO DI GAMEPAD";
            case "melee.4"       -> "CANCELLAZIONE";
            case "melee.default" -> "MELEE";


            // ── Descrizioni modalità ──────────────────────────────────────────
            case "mod.storia.d1"    -> "Sconfiggi 4 Boss";
            case "mod.storia.d2"    -> "per arrivare in ufficio.";
            case "mod.infinita.d1"  -> "Sopravvivi all'infinito!";
            case "mod.infinita.d2"  -> "Nemici sempre piu forti.";


            // ── Dialogo shopkeeper ────────────────────────────────────────────
            case "dial.shop.nome"  -> "NEGOZIANTE";
            case "dial.shop.sk0"   -> "Ehi! Non toccare la merce se non hai intenzione di comprare.";
            case "dial.shop.pg0"   -> "Rilassati, sto solo dando un'occhiata...";
            case "dial.shop.sk1"   -> "Vuoi attaccarmi?!";

            // ── Dialogo finale ufficio ────────────────────────────────────────
            case "dial.capo.nome"  -> "CAPO";
            case "dial.capo.sk0"   -> "Finalmente sei arrivato. Ti aspettavo.";
            case "dial.capo.pg0"   -> "Ho attraversato quattro mondi per arrivare qui.";
            case "dial.capo.sk1"   -> "Lo so. Ho visto tutto.";
            case "dial.capo.pg1"   -> "Allora sai anche perche sono in ritardo.";
            case "dial.capo.sk2"   -> "Sai cosa mi fa piu ridere? La riunione e stata spostata a domani.";
            case "dial.capo.pg2"   -> "...";
            case "dial.capo.sk3"   -> "Siediti. Hai l'aria di qualcuno che ha bisogno di un caffe.";

          // ── Nomi mondi ────────────────────────────────────────────────────
            case "mondo.casa"   -> "CASA";
            case "mondo.1"      -> "IL CANTIERE";
            case "mondo.2"      -> "LE FOGNE";
            case "mondo.3"      -> "LA FORNACE";
            case "mondo.4"      -> "IL GHIACCIAIO";
            case "mondo.5"      -> "IL CASTELLO";
            case "mondo.abisso" -> "L'ABISSO";

          // ── Nomi boss ─────────────────────────────────────────────────────
            case "boss.m1.nome"  -> "MANNIE";
            case "boss.m2.nome"  -> "OMEN";
            case "boss.m3.nome"  -> "KING OVEN";
            case "boss.m4.nome"  -> "FROST";
            case "boss.m5.nome"  -> "YABBADUHLON";


            // ── Boss Rush ─────────────────────────────────────────────────────
            case "bossrush.titolo"     -> "BOSS RUSH";
            case "bossrush.sottotit"   -> "Sconfiggi i boss per arrivare al Castello";
            case "bossrush.scegli"     -> "Scegli un potenziamento";
            case "bossrush.prossimo"   -> "Prossimo Boss";
            case "bossrush.completa"   -> "Accesso al Castello sbloccato!";
            case "bossrush.tombino"    -> "[ S ] Boss Rush";
            case "bossrush.pw.cura"    -> "+2 Vita Max";
            case "bossrush.pw.vel"     -> "+Velocita";
            case "bossrush.pw.danno"   -> "+Danno";
            case "bossrush.pw.melee"   -> "Melee";
            case "bossrush.boss2"      -> "PRESAGIO";
            case "bossrush.boss3"      -> "RE DEL FORNO";
            case "bossrush.boss4"      -> "GELO";

            default -> null;
        };
    }

    // ── English ───────────────────────────────────────────────────────────────
    private static String EN(String k) {
        return switch (k) {

            // ── Main menu ────────────────────────────────────────────────────
            case "btn.gioca"          -> "PLAY";
            case "btn.impostazioni"   -> "SETTINGS";
            case "btn.controlli"      -> "CONTROLS";
            case "btn.esci"           -> "QUIT";
            case "btn.lingua"         -> "LANGUAGE";
            case "btn.menu.principale"   -> "MAIN MENU";
            case "footer.fullscreen"  -> "F11 = Fullscreen";

            // ── Pause ────────────────────────────────────────────────────────
            case "pausa.titolo"       -> "PAUSE";
            case "btn.riprendi"       -> "RESUME";
            case "btn.menu"           -> "MENU";
            case "btn.riprova"        -> "RETRY";

            // ── Settings ─────────────────────────────────────────────────────
            case "imp.titolo"         -> "SETTINGS";
            case "imp.musica"         -> "MUSIC";
            case "imp.effetti"        -> "EFFECTS";
            case "btn.indietro"       -> "BACK";

            // ── Controls ─────────────────────────────────────────────────────
            case "ctrl.titolo"        -> "CONTROLS";
            case "ctrl.sezione.gioco" -> "GAME";
            case "ctrl.sezione.tetris"-> "TETRIS  (first room)";
            case "ctrl.wasd"          -> "Movement";
            case "ctrl.frecce"        -> "Directional shot";
            case "ctrl.z"             -> "Melee attack";
            case "ctrl.esc"           -> "Pause";
            case "ctrl.f11"           -> "Fullscreen";
            case "ctrl.click"         -> "Interact / Buy";
            case "ctrl.invio"         -> "Confirm dialogue";
            case "ctrl.q"             -> "Quit (in pause)";
            case "ctrl.ad"            -> "Move piece";
            case "ctrl.w"             -> "Rotate piece";
            case "ctrl.s"             -> "Soft drop";
            case "ctrl.spazio"        -> "Hard drop";
            case "ctrl.escsalta"      -> "Skip Tetris";
            case "ctrl.punteggi"      -> "HEAL | 1500: SPD | 3000: DMG | 5000: MELEE | 6000: ALL";

            // ── Character select ─────────────────────────────────────────────
            case "pg.scegli"          -> "CHOOSE YOUR CHARACTER";
            case "pg.hint"            -> "[Arrows/Mouse] navigate    [ENTER/Click] confirm    [ESC] back";

            // ── Mode select ──────────────────────────────────────────────────
            case "mod.seleziona"      -> "SELECT THE CHALLENGE";
            case "mod.storia"         -> "CLASSIC STORY";
            case "mod.infinita"       -> "ENDLESS MODE";
            case "mod.hint"           -> "[Arrows/Mouse] navigate    [ENTER/Click] confirm    [ESC] back";

            // ── Game Over ─────────────────────────────────────────────────────
            case "go.titolo"          -> "YOU DIDN'T MAKE IT.";
            case "go.sub"             -> "The office will wait.";
            case "go.stats"           -> "World %d  |  Room %d  |  Coins %d";
            case "go.stanze"          -> "Total rooms: %d";

            // ── Victory ──────────────────────────────────────────────────────
            case "win.titolo"         -> "YOU MADE IT.";
            case "win.sub.ritardo"    -> "A little late, but still.";
            case "win.sub.molto"      -> "Better late than never.";
            case "win.monete"         -> "Coins earned: %d";
            case "win.codice"         -> "Code found: %s";

            // ── Office ───────────────────────────────────────────────────────
            case "ufficio.banner"     -> "OFFICE";
            case "ufficio.continua"   -> "[ ENTER to continue ]";

            // ── HUD ──────────────────────────────────────────────────────────
            case "hud.stanza"         -> "Room %d/%d";
            case "hud.mondo"          -> "W%d";
            case "hud.bonus"          -> "BONUS";
            case "hud.freeze"         -> "FREEZE";
            case "hud.lento"          -> "SLOW";

            // ── Tetris ───────────────────────────────────────────────────────
            case "tet.logo"           -> "WHAT TETRIS";
            case "tet.prossimo"       -> "NEXT";
            case "tet.premio"         -> "REWARD";
            case "tet.tempo"          -> "TIME";
            case "tet.istr"           -> "A/D move   W rotate   S soft drop   SPACE hard drop   ESC skip";
            case "tet.gameover"       -> "GAME OVER";
            case "tet.timeout"        -> "TIME'S UP!";
            case "tet.punteggio"      -> "Score: %d";
            case "tet.hint"           -> "ENTER to continue";
            case "tet.cura"           -> "+HEAL";
            case "tet.velocita"       -> "+SPD";
            case "tet.danno"          -> "+DMG";
            case "tet.tutto"          -> "ALL!";
            case "tet.next.vel"       -> "3000: DMG";
            case "tet.next.danno"     -> "5000: MELEE";
            case "tet.next.melee"     -> "6000: ALL";
            case "tet.next.cura"      -> "1500: SPD";

            // ── Shop ─────────────────────────────────────────────────────────
            case "shop.banner"        -> "SHOP";
            case "shop.nome"          -> "SHOPKEEPER";
            case "shop.domanda"       -> "Do you want to attack the shopkeeper?";
            case "shop.avviso"        -> "You'll get 20 coins, but lose the shop.";
            case "shop.hint"          -> "A/D to choose   ENTER to confirm   ESC to cancel";

            // ── JRPG dialogue ────────────────────────────────────────────────
            case "dial.continua"      -> "[ ENTER to continue ]";
            case "dial.inizia"        -> "[ ENTER to start ]";

            // ── Service note ─────────────────────────────────────────────────
            case "nota.titolo"        -> "MEMO";
            case "nota.firma"         -> "- The Boss";
            case "nota.chiudi"        -> "[ ENTER to close ]";

            // ── Room banners ─────────────────────────────────────────────────
            case "banner.casa"        -> "HOME";

            // ── Tile reward popups ───────────────────────────────────────────
            case "popup.veleno"       -> "POISON!";
            case "popup.congelato"    -> "FROZEN!";
            case "popup.cannone"      -> "CANNON!";
            case "popup.melee"        -> "MELEE UNLOCKED!";
            case "popup.danno"        -> "+3 WEAPON DMG!";
            case "popup.vita"         -> "+1 LIFE SLOT!";
            case "popup.velocita"     -> "+1.5 SPEED!";
            case "popup.danno2"       -> "+2 DAMAGE!";
            case "popup.firerate"     -> "FIRE RATE UP!";

            // ── Mode select desc ─────────────────────────────────────────────
            case "mod.s.desc"         -> "S";
            case "mod.inf.desc"       -> "INF";

            // ── Casa intro ───────────────────────────────────────────────────
            case "casa.intro"         -> "WHAT? I'VE PLAYED TOO MUCH, I'M GONNA BE LATE!!!";


            // ── Character descriptions ────────────────────────────────────────
            case "pg.bellgerd.desc"   -> "Jack of all trades. Master of none.";
            case "pg.vlad.desc"       -> "Fast. Very fast. Too fast.";
            case "pg.paul.desc"       -> "Slow. But each hit counts for three.";
            case "pg.juicy.desc"      -> "Won't stop. Ever.";
            case "pg.ditto.desc"      -> "???";

            // ── Character unlock ──────────────────────────────────────────────
            case "pg.sblocco.1.r1"    -> "Defeat the Boss";
            case "pg.sblocco.1.r2"    -> "of World 1";
            case "pg.sblocco.2.r1"    -> "Defeat the Boss";
            case "pg.sblocco.2.r2"    -> "of World 2";
            case "pg.sblocco.3.r1"    -> "Defeat the Boss";
            case "pg.sblocco.3.r2"    -> "of World 3";
            case "pg.sblocco.4.r1"    -> "???";
            case "pg.sblocco.4.r2"    -> "";

            // ── Shop item labels ──────────────────────────────────────────────
            case "item.cura.label"    -> "HEAL";
            case "item.vel.label"     -> "SPD";
            case "item.danno.label"   -> "DMG";

            // ── Memo body ─────────────────────────────────────────────────────
            case "nota.riga0"  -> "To whoever finds this note,";
            case "nota.riga1"  -> "";
            case "nota.riga2"  -> "If the system stops responding,";
            case "nota.riga3"  -> "use this code for the diagnostic";
            case "nota.riga4"  -> "panel:";
            case "nota.riga5"  -> "";

            // ── Boss dialogues ────────────────────────────────────────────────
            case "boss.m1.pg0"  -> "Come on, I have no time - I\'m already late!";
            case "boss.m1.b0"   -> "Neither do I.";
            case "boss.m2.pg0"  -> "So this is where you end up playing too many video games...";
            case "boss.m3.pg0"  -> "Get out of the way. I don\'t care if you\'re a king.";
            case "boss.m3.pg1"  -> "I\'ll dethrone you!";
            case "boss.m3.b0"   -> "*angry baking tray noises*";
            case "boss.m3.pg2"  -> "...";
            case "boss.m4.pg0"  -> "So cold. How do people live here?";
            case "boss.m4.b0"   -> "They don\'t live here. They freeze.";
            case "boss.m4.pg1"  -> "Charming.";
            case "boss.m5.b0"   -> "Yes... it\'s me.";
            case "boss.m5.pg0"  -> "You\'re behind all this mess?";
            case "boss.m5.pg1"  -> "What?!";
            case "boss.m5.b1"   -> "I already know everything you\'re about to say, do or attempt.";
            case "boss.m5.pg2"  -> "Do you have some kind of power?";
            case "boss.m5.pg3"  -> "Well... my brain is about to melt at this rate.";
            case "boss.m5.b2"   -> "You will NEVER make it to the office!!!";
            case "boss.m5.pg4"  -> "Who\'s gonna explain this to my boss...";



            // ── Bonus room panel ──────────────────────────────────────────────
            case "bonus.titolo"     -> "BONUS ROOM";
            case "bonus.danno"      -> "DAMAGE";
            case "bonus.velocita"   -> "SPEED";
            case "bonus.fuocolento" -> "SLOW FIRE";
            case "bonus.nessunmalus"-> "no penalty";

            // ── Melee attacks ─────────────────────────────────────────────────
            case "melee.0"       -> "SUITCASE SLAM";
            case "melee.1"       -> "WRENCH STRIKE";
            case "melee.2"       -> "AXE CHOP";
            case "melee.3"       -> "GAMEPAD BASH";
            case "melee.4"       -> "DELETION";
            case "melee.default" -> "MELEE";


            // ── Mode descriptions ─────────────────────────────────────────────
            case "mod.storia.d1"    -> "Defeat 4 Bosses";
            case "mod.storia.d2"    -> "and make it to the office.";
            case "mod.infinita.d1"  -> "Survive forever!";
            case "mod.infinita.d2"  -> "Enemies keep getting harder.";


            // ── Shopkeeper dialogue ───────────────────────────────────────────
            case "dial.shop.nome"  -> "SHOPKEEPER";
            case "dial.shop.sk0"   -> "Hey! Don't touch the goods unless you're buying.";
            case "dial.shop.pg0"   -> "Relax, I'm just browsing...";
            case "dial.shop.sk1"   -> "You wanna fight me?!";

            // ── Final office dialogue ─────────────────────────────────────────
            case "dial.capo.nome"  -> "BOSS";
            case "dial.capo.sk0"   -> "You finally made it. I was waiting.";
            case "dial.capo.pg0"   -> "I crossed four worlds to get here.";
            case "dial.capo.sk1"   -> "I know. I saw everything.";
            case "dial.capo.pg1"   -> "Then you know why I'm late.";
            case "dial.capo.sk2"   -> "You know what's funny? The meeting got moved to tomorrow.";
            case "dial.capo.pg2"   -> "...";
            case "dial.capo.sk3"   -> "Sit down. You look like someone who needs a coffee.";


            // ── Boss Rush ─────────────────────────────────────────────────────
            case "bossrush.titolo"     -> "BOSS RUSH";
            case "bossrush.sottotit"   -> "Defeat the bosses to reach the Castle";
            case "bossrush.scegli"     -> "Choose a power-up";
            case "bossrush.prossimo"   -> "Next Boss";
            case "bossrush.completa"   -> "Castle access unlocked!";
            case "bossrush.tombino"    -> "[ S ] Boss Rush";
            case "bossrush.pw.cura"    -> "+2 Max HP";
            case "bossrush.pw.vel"     -> "+Speed";
            case "bossrush.pw.danno"   -> "+Damage";
            case "bossrush.pw.melee"   -> "Melee";
            case "bossrush.boss2"      -> "OMEN";
            case "bossrush.boss3"      -> "KING OVEN";
            case "bossrush.boss4"      -> "FROST";

            // ── World names ───────────────────────────────────────────────────
            case "mondo.casa"   -> "HOME";
            case "mondo.1"      -> "THE WORKSITE";
            case "mondo.2"      -> "THE SEWERS";
            case "mondo.3"      -> "THE FURNACE";
            case "mondo.4"      -> "THE GLACIER";
            case "mondo.5"      -> "THE CASTLE";
            case "mondo.abisso" -> "THE ABYSS";

            // ── Boss names ────────────────────────────────────────────────────
            case "boss.m1.nome"  -> "MANNIE";
            case "boss.m2.nome"  -> "OMEN";
            case "boss.m3.nome"  -> "KING OVEN";
            case "boss.m4.nome"  -> "FROST";
            case "boss.m5.nome"  -> "YABBADUHLON";

            default -> null;
        };
    }
}

# WHAT I'VE PLAYED TOO MUCH

Un roguelike 2D in Java. Sei in ritardo al lavoro. Invece di andarci, hai passato la notte a giocare. Adesso devi attraversare cinque mondi di nemici e boss per arrivare in ufficio — se ci arrivi.

---

## Come si gioca

| Tasto | Azione |
|-------|--------|
| W A S D | Muovi il personaggio |
| Frecce direzionali | Spara |
| Spazio | Attacco melee (se sbloccato) |
| ESC | Pausa |
| F11 | Schermo intero |

---

## Struttura di una run

**1. Tetris**
Prima di entrare si gioca una partita a Tetris da 40 secondi. Il punteggio determina il power-up iniziale.

**2. Casa**
La prima stanza. Contiene il power-up guadagnato col Tetris e una nota raccoglibile con codice segreto. Raccoglili e parti.

**3. Stanze**
Ogni mondo ha stanze con nemici. Eliminali tutti per aprire la porta verso la stanza successiva.

**4. Stanza Ardua** *(opzionale)*
Prima del boss appare una stanza di sfida con malus attivi (danno ridotto, velocita ridotta, cadenza di fuoco lenta). Completarla sblocca un bonus permanente per il resto della run.

**5. Stanza Bonus** *(opzionale, mondi 3–5)*
Accessibile da una porta a sud in alcune stanze. Contiene un potenziamento extra.

**6. Boss**
L'ultima stanza di ogni mondo. Sconfiggi il boss per avanzare. La porta si blocca all'ingresso.

**7. Negozio**
Appare dopo alcune stanze. Spendi le monete raccolte per comprare potenziamenti. Puoi anche attaccare il negoziante per 20 monete extra, ma perdi lo shop per il resto della run.

---

## Power-up Tetris

| Punteggio | Ricompensa |
|-----------|------------|
| 500+ | Cura extra |
| 1500+ | Velocita aumentata |
| 3000+ | Danno aumentato |
| 5000+ | Melee sbloccato |
| 6000+ | Tutto quanto |

---

## Modalita

**Storia** — 5 mondi con boss. Completali tutti per finire il gioco e arrivare in ufficio.

**Infinita** — I mondi si ripetono all'infinito con difficolta crescente.

---

## Personaggi

| Nome | Vita | Velocita | Danno | Stile |
|------|------|----------|-------|-------|
| BELLGERD | 6 | Media | Medio | Equilibrato, jack of all trades |
| VLAD | 3 | Alta | Basso | Velocissimo ma fragile |
| PAUL | 5 | Bassa | Alto | Lento, ogni colpo conta tre |
| JUICY | 12 | Bassa | Basso | Moltissima vita, non si ferma mai |
| D.I.T.T.O. | ??? | ??? | ??? | Sbloccabile con una combinazione segreta |

I personaggi VLAD, PAUL, JUICY e D.I.T.T.O. si sbloccano sconfiggendo i boss dei rispettivi mondi.

Ogni personaggio ha un attacco melee unico, sbloccabile sconfiggendo il negoziante:

| Personaggio | Melee |
|-------------|-------|
| BELLGERD | Colpo di valigia |
| VLAD | Chiave inglese |
| PAUL | Accetta |
| JUICY | Colpo di gamepad |
| D.I.T.T.O. | Cancellazione |

---

## I Mondi e i Boss

**Mondo 1 — Il Cantiere**
Tileset industriale. Il boss ti insegue e spara un ventaglio a 3 proiettili.

**Mondo 2 — Le Fogne** *(veleno)*
Calpestare le piastrelle avvelenate infligge danno continuativo. Il boss si muove a scatti improvvisi e spara raffiche in 8 direzioni.

**Mondo 3 — La Fornace** *(fuoco)*
Le piastrelle di fuoco bruciano. Il boss si carica verso di te — proiettili di fuoco e contatto infliggono bruciatura continuativa per alcuni secondi.

**Mondo 4 — Il Ghiacciaio** *(ghiaccio)*
Le piastrelle di ghiaccio rallentano. Le piastrelle di ghiaccio forte congelano temporaneamente. Il boss GELO usa meccaniche di freeze e tiro incrociato.

**Mondo 5 — Il Castello** *(cannoni)*
Cannoni automatici sparano ogni 6 secondi indipendentemente dalla posizione del giocatore. Il boss YABBADUHLON ha tre fasi:
- *Fase 1*: movimento specchio rispetto al centro stanza, spirale rotante
- *Fase 2*: piu veloce, alterna spirale e burst di proiettili mirati
- *Fase 3*: inseguimento diretto, spirale e burst simultanei, schiva i tuoi proiettili

---

## Effetti Tile

| Effetto | Conseguenza |
|---------|-------------|
| Veleno | Danno continuo finche si resta sulla tile |
| Fuoco | Bruciatura: 2 cuori totali in 3 secondi |
| Ghiaccio | Velocita ridotta per alcuni secondi |
| Ghiaccio forte | Congelamento temporaneo |
| Cannone | Proiettile lanciato ogni 6 secondi |

---

## Schermata di sconfitta

Monitor CRT rotto con statico animato, effetti glitch, linee di scansione e distorsioni orizzontali. Un controller pixel-art in basso accompagna la schermata. Mostra il mondo, la stanza e le monete raggiunte al momento della morte.

---

## Localizzazione

Il gioco supporta italiano e inglese. Il tasto LINGUA nel menu principale alterna le due lingue in tempo reale, aggiornando dialoghi, nomi boss, descrizioni personaggi e tutti i testi dell'interfaccia senza riavviare.

---

## Compilazione

Richiede Java 17 o superiore.

```bash
javac *.java
java WhatIvePlayedTooMuch
```

---

## File principali

| File | Ruolo |
|------|-------|
| `WhatIvePlayedTooMuch.java` | Entry point e finestra |
| `GameLoop.java` | Logica di gioco, collisioni, melee, cannoni |
| `GameState.java` | Stato della run |
| `RenderEngine.java` | Rendering di tutti gli stati |
| `RoomManager.java` | Generazione stanze e dialoghi boss |
| `Boss.java` | Comportamento e fasi dei boss |
| `BossProjectile.java` | Proiettili boss tipizzati |
| `TetrisGame.java` | Minigame Tetris pre-partita |
| `AudioManager.java` | Musica e SFX |
| `ResourceLoader.java` | Caricamento asset e sprite |
| `Lang.java` | Sistema di localizzazione IT/EN |
| `UIManager.java` | Layout bottoni e lista personaggi |
| `InputHandler.java` | Input mouse e tastiera |
| `MenuButton.java` | Bottone pixel-art 3D con hover glow |
| `SistemaPersonaggi.java` | Sblocco personaggi e combo segreto |
| `DatiPersonaggio.java` | Statistiche e descrizione personaggio |
| `DialogoNarrazione.java` | Sistema dialogo JRPG multi-pagina |
| `DialogoShopkeeper.java` | Dialogo scelta attacco negoziante |
| `TileSet.java` | Tileset per mondo con effetti speciali |
| `FullscreenManager.java` | Gestione fullscreen e ridimensionamento |

---

## Easter egg

**Nota debug** — Nella stanza Casa spawna in posizione casuale una nota raccoglibile. Raccogliendola appare un popup in stile carta trovata con il codice **WIPT-4269**, che compare anche sulla schermata finale della storia.

**D.I.T.T.O.** — Il quinto personaggio non ha statistiche visibili. Si sblocca con una combinazione segreta di tasti non documentata.

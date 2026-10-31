# Comprendre le code (notes pour l'oral)

Chaque classe ou méthode citée est suivie de son fichier et de sa ligne, sous la forme `fichier:ligne`. Les sorties montrées viennent de vraies commandes, lancées depuis la racine en locale UTF-8 (`export LC_ALL=C.UTF-8`).

Mots utiles :
- **regex** : le motif cherché, par exemple `Gr(e|a)*g.r`.
- **AST** (arbre de syntaxe) : la regex rangée en arbre. Chaque nœud est une opération (concaténer, choisir, répéter) ou une lettre.
- **automate** : une machine à états. Elle lit un caractère, change d'état, et dit « trouvé » quand elle arrive dans un état final.
- **AFN-ε** : automate non déterministe. Depuis un état, une même lettre peut mener à plusieurs états, et les flèches ε se prennent sans rien lire.
- **AFD** : automate déterministe. Depuis un état, chaque lettre mène à un seul état. C'est facile à exécuter.

## 1. Vue d'ensemble : le chemin d'une regex

1. **Lecture de la commande.** `Main.executer` (`src/Main.java:123`) lit les options, la regex et le fichier, puis choisit l'algorithme.
2. **Analyse de la regex.** `Parser.parse` (`src/parser/Parser.java:46`) transforme le texte de la regex en AST.
3. **Choix de la méthode.** Si la regex n'est qu'une suite de lettres, `Kmp.motifLitteral` (`src/kmp/Kmp.java:148`) la renvoie et on utilise KMP. Sinon, on construit un automate.
4. **Construction de l'automate.** `ConstructionAutomate` (`src/automate/ConstructionAutomate.java:26`) enchaîne AFN-ε, AFD, AFD minimal, puis table de transitions (`AfdCompile`, `src/automate/AfdCompile.java:59`).
5. **Recherche.** `FileSearcher.rechercher` (`src/experimentation/FileSearcher.java:33`) lit le texte ligne par ligne et demande au matcher si chaque ligne contient le motif.
6. **Affichage.** `Main.executer` écrit les lignes trouvées (ou leur nombre avec `-c`) et renvoie le code de sortie : 0 si trouvé, 1 sinon, 2 en cas d'erreur.

## 2. Le trajet de `Gr(e|a)*g.r` sur une ligne de *Metamorphosis*

La ligne 39 de `data/metamorphosis-5200.txt` :

```
One morning, when Gregor Samsa woke from troubled dreams, he found
```

Commande : `java -jar egrep-clone.jar --debug 'Gr(e|a)*g.r' data/metamorphosis-5200.txt`. Sur tout le livre, elle trouve 294 lignes, comme `grep -E -c`.

### Étape 1 : `Parser` — du texte à l'arbre

- **Reçoit** la chaîne `Gr(e|a)*g.r`.
- **Rend** l'AST. Une méthode par niveau de priorité : `parseExpression` (le `|`, `src/parser/Parser.java:60`), `parseConcat` (mettre bout à bout, `:78`), `parseRepeat` (l'étoile, `:96`), `parseAtom` (lettre, point ou parenthèses, `:108`).

Sortie de `--debug --matcher=naif` (arbre de R) :

```
└── CONCAT
    ├── CONCAT
    │   ├── CONCAT
    │   │   ├── CONCAT
    │   │   │   ├── CONCAT
    │   │   │   │   ├── CHAR("G")
    │   │   │   │   └── CHAR("r")
    │   │   │   └── STAR(*)
    │   │   │       └── OR
    │   │   │           ├── CHAR("e")
    │   │   │           └── CHAR("a")
    │   │   └── CHAR("g")
    │   └── DOT(.)
    └── CHAR("r")
```

Par défaut, on utilise le matcher une passe. `MatcherUnePasse.envelopper` (`src/experimentation/MatcherUnePasse.java:46`) ajoute alors `.*` devant : la racine devient `CONCAT(STAR(DOT), …)`. C'est l'arbre que montre `--debug` sans option.

### Étape 2 : `AstToAfnVisitor` — de l'arbre à l'AFN-ε

- **Reçoit** l'AST.
- **Rend** un AFN-ε (`Afn`, `src/automate/Afn.java:12`). Chaque nœud donne un petit automate, et on les relie par des flèches ε :
  - une lettre donne 2 états et 1 flèche (`visit(AstChar)`, `src/automate/AstToAfnVisitor.java:74`) ;
  - l'étoile ajoute 2 états et des flèches ε pour « entrer », « recommencer », « sortir » et « zéro fois » (`visit(AstRepeat)`, `:259`).

`--debug` : **AFN-ε de R : 18 états, 20 transitions**. Avec `.*R` : 22 états, 26 transitions.

### Étape 3 : `Determinisation` — de l'AFN-ε à l'AFD

- **Reçoit** l'AFN-ε.
- **Rend** un AFD (`Afd`, `src/automate/Afd.java:16`).
- **Méthode** : construction des sous-ensembles. Un état de l'AFD représente l'ensemble des états de l'AFN où l'on peut être après avoir lu un même mot.
  - `epsilonClosure` (`src/automate/Determinisation.java:45`) ajoute tous les états atteints par des flèches ε ;
  - `move` (`:78`) suit les flèches d'une lettre ;
  - `convertir` (`:109`) répète ces deux opérations jusqu'à ce qu'aucun nouvel ensemble n'apparaisse.
- Si l'ensemble obtenu est vide, aucune transition n'est créée (`:164`) : l'AFD n'a donc pas d'état puits.
- Comme la regex contient un point, l'alphabet reçoit le symbole `AUTRE` (`Afd.AUTRE`, `src/automate/Afd.java:22`). Il représente « tout caractère qui n'est pas dans la regex ».

`--debug` : **AFD de R : 8 états**, alphabet `[AUTRE, G, a, e, g, r]`. Avec `.*R` : 11 états.

### Étape 4 : `Minimisation` — l'AFD le plus petit possible

- **Reçoit** l'AFD.
- **Rend** l'AFD minimal, sans état puits. `minimiser` (`src/automate/Minimisation.java:357`) enchaîne :
  1. enlever les états qu'on ne peut pas atteindre ;
  2. compléter avec un puits ;
  3. séparer les états qui se comportent différemment, jusqu'à ce que plus rien ne change (algorithme de Moore) ;
  4. fusionner chaque groupe en un état ;
  5. retirer le puits (`retirerPuits`, `:387`).
- Pour R, les états « après Gr », « après Gr…a » et « après Gr…e » font la même chose : ils sont fusionnés.

`--debug` : **AFD minimal de R : 6 états**. Avec `.*R` : **8 états**.

AFD minimal de `.*R`, celui qu'on utilise. L'état initial est q2 et les états finaux sont q0 et q1. Quelques transitions :

```
q2 --G--> q3      q2 --AUTRE--> q2     (on attend un G)
q3 --r--> q4      q4 --e--> q4         (la boucle (e|a)*)
q4 --g--> q5      q5 --AUTRE--> q6     (le point)
q6 --r--> q0                           (q0 est final : trouvé)
```

### Étape 5 : `AfdCompile` — la table de transitions

- **Reçoit** l'AFD minimal.
- **Rend** un tableau `table[état][caractère]` de 257 colonnes : 0 à 255 pour les caractères de code inférieur à 256, et 256 pour AUTRE. Les états sont numérotés comme dans `--debug`.
- Pour une lettre absente de la regex, le constructeur recopie la transition sur AUTRE dans sa colonne (`src/automate/AfdCompile.java:106`).
- La méthode `suivant` (`:138`) lit une case, en O(1).

Quelques cases, déduites de la sortie de `--debug` et de cette règle :

| Case | Valeur | Pourquoi |
|---|---|---|
| `table[2]['G']` | 3 | `q2 --G--> q3` |
| `table[3]['r']` | 4 | `q3 --r--> q4` |
| `table[5][256]` (colonne AUTRE) | 6 | `q5 --AUTRE--> q6` |
| `table[5]['o']` | 6 | `o` n'est pas dans la regex : copie de la colonne AUTRE |
| `table[6]['r']` | 0 | `q6 --r--> q0`, état final |

Un caractère de code ≥ 256 absent de la regex, comme `’`, va dans la colonne 256 (`:153`).

### Étape 6 : `MatcherUnePasse` — la lecture de la ligne

- **Reçoit** la ligne.
- **Rend** vrai ou faux.
- `contientMotif` (`src/experimentation/MatcherUnePasse.java:60`) part de l'état initial, lit un caractère à la fois avec `suivant`, et s'arrête au premier état final.

Lecture de la ligne 39 (position, caractère, colonne utilisée, état) :

```
0..17  "One morning, when "   q2 → q2 à chaque caractère (O, n, espace… : AUTRE ; e, r, g : leur colonne)
18     G                      q2 → q3
19     r                      q3 → q4
20     e                      q4 → q4
21     g                      q4 → q5
22     o   (AUTRE)            q5 → q6     le point lit « o »
23     r                      q6 → q0     état final : la ligne est trouvée
```

On a lu 24 caractères, une seule fois chacun, sans jamais revenir en arrière.

## 3. Le trajet de KMP : `anas` sur « ananas »

Commande : `java -jar egrep-clone.jar --debug anas data/candide-4650.txt`. Comme `anas` n'est qu'une suite de lettres, `Kmp.motifLitteral` (`src/kmp/Kmp.java:148`) renvoie `"anas"`, et `Main` crée un `Kmp` (`src/Main.java:253`) au lieu d'un automate.

### La table π

`calculerEchec` (`src/kmp/Kmp.java:58`) calcule π[i] : la longueur du plus long début du motif qui est aussi une fin de `P[0..i]`, sans être `P[0..i]` lui-même.

| i | 0 | 1 | 2 | 3 |
|---|---|---|---|---|
| P[i] | a | n | a | s |
| π[i] | 0 | 0 | 1 | 0 |

- π[2] = 1, car `ana` commence et finit par `a`.
- π[3] = 0, car aucun début de `anas` ne finit par `s`.

C'est la sortie de `--debug`, et aussi celle de `java -cp bin:bin-bench TraceKmp anas`. Sous la forme du cours, CarryOver vaut [−1, 0, −1, 1, 0].

### La recherche pas à pas

`contientMotif` (`src/kmp/Kmp.java:90`) garde un seul nombre, q : combien de lettres du motif sont déjà reconnues. Ligne 3658 de Candide : « limons, des ananas, des dattes, … ».

| position | caractère | q avant | ce qui se passe | q après |
|---|---|---|---|---|
| 12 | a | 0 | `a` = P[0] | 1 |
| 13 | n | 1 | `n` = P[1] | 2 |
| 14 | a | 2 | `a` = P[2] | 3 |
| 15 | n | 3 | on attend `s`, on lit `n` : **q = π[2] = 1** ; puis `n` = P[1] | 2 |
| 16 | a | 2 | `a` = P[2] | 3 |
| 17 | s | 3 | `s` = P[3] : **trouvé** | 4 |

En position 15, on ne repart pas de zéro : π[2] = 1 dit que le dernier `a` lu (position 14) peut commencer une nouvelle occurrence. C'est la boucle `while` de `src/kmp/Kmp.java:105`. On compare le même `n` une deuxième fois, sans reculer dans le texte. Si on repartait de zéro, on manquerait `anas`, qui commence en 14. Le banc le vérifie : un KMP faux qui fait cela se trompe exactement sur cette ligne.

## 4. Une fiche par classe

**`Main`** (`src/Main.java:51`)
- Rôle : point d'entrée. Lit les options, choisit KMP ou automate, lance la recherche, affiche le résultat. Les messages de debug et d'erreur vont sur la sortie d'erreur.
- Méthode principale : `executer` (`:123`), appelée par `main` (`:86`).
- Complexité : pas d'algorithme propre.
- Q : pourquoi `executer` est-elle séparée de `main` ? R : `executer` ne quitte pas la JVM et renvoie le code de sortie : on peut l'appeler sans arrêter le programme.

**`Parser`** (`src/parser/Parser.java:25`)
- Rôle : transforme la regex en AST par descente récursive, une méthode par niveau de priorité. Tout caractère qui n'est pas `( ) | * .` est une lettre.
- Méthode principale : `parse` (`:46`).
- Complexité : O(|R|), chaque caractère est lu une fois.
- Q : comment gérez-vous la priorité de `*` sur la concaténation, et de la concaténation sur `|` ? R : par l'ordre des appels. `parseExpression` appelle `parseConcat`, qui appelle `parseRepeat` : le plus prioritaire est traité au plus profond.

**`AstChar`, `AstDot`, `AstConcat`, `AstOr`, `AstRepeat`, `AstVide`** (`src/ast/AstChar.java:10`, `AstDot.java:11`, `AstConcat.java:11`, `AstOr.java:9`, `AstRepeat.java:10`, `AstVide.java:11`)
- Rôle : les six sortes de nœuds de l'arbre. `AstVide` représente le mot vide (motif vide, `a|`, `()`).
- Méthode principale : `accept` (par exemple `src/ast/AstChar.java:42`), qui appelle la bonne méthode du visiteur.
- Complexité : O(1) par nœud.
- Q : pourquoi un nœud `AstVide` ? R : grep accepte `a|` ou `()`, qui reconnaissent le mot vide. Il fallait un nœud pour le représenter.

**`AstFactory`** (`src/ast/AstFactory.java:10`)
- Rôle : crée les nœuds, par exemple `createConcat` (`:61`) ou `createRepeat` (`:50`). Elle sert au `Parser` et à `MatcherUnePasse.envelopper`.
- Méthode principale : `createConcat`.
- Complexité : O(1).
- Q : à quoi sert l'interface `AstFactoryI` (`src/ast/interfaces/AstFactoryI.java:12`) ? R : seulement à documenter les signatures. Ses méthodes `static` ne sont pas héritées et ne contraignent rien.

**`AstVisitor`** (`src/ast/interfaces/AstVisitor.java:17`)
- Rôle : le patron visiteur, une méthode `visit` par sorte de nœud. On peut ajouter un traitement sur l'arbre sans modifier les nœuds.
- Méthode principale : `visit`.
- Complexité : sans objet.
- Q : qui l'implémente ? R : `AstToAfnVisitor`, le seul visiteur du pipeline.

**`AstToAfnVisitor`** (`src/automate/AstToAfnVisitor.java:31`)
- Rôle : construction de Thompson (présentation d'Aho-Ullman). Chaque nœud devient un petit AFN, relié aux autres par des ε-transitions.
- Méthodes principales : `visit(AstConcat)` (`:154`), `visit(AstOr)` (`:195`), `visit(AstRepeat)` (`:259`).
- Complexité (Javadoc) : O(|R|) états et transitions ; temps O(k³) au pire pour k lettres concaténées, à cause des copies.
- Q : pourquoi O(k³) ? R : chaque nœud recopie les automates de ses fils, et `ajouterEtat` vérifie en temps linéaire que l'état est nouveau. C'est négligeable pour des motifs courts : 50 ms pour 400 lettres.

**`Afn`, `Afd`, `Etat`, `Transition`** (`src/automate/Afn.java:12`, `Afd.java:16`, `Etat.java:8`, `Transition.java:8`)
- Rôle : les structures de données des automates. Un `Etat` n'a aucun attribut : il est reconnu par son identité. Une `Transition` relie deux états par un symbole (une lettre, `"."`, `"EPSILON"` ou `AUTRE`).
- Méthodes principales : `Afn.destinations` (`src/automate/Afn.java:55`), `Afd.transition` (`src/automate/Afd.java:89`).
- Complexité : accès par table de hachage.
- Q : pourquoi des symboles en `String` ? R : pour avoir `"EPSILON"` et `AUTRE` à côté des lettres. Pendant la recherche, on n'utilise pas ces chaînes, mais la table d'entiers d'`AfdCompile`.

**`Determinisation`** (`src/automate/Determinisation.java:23`)
- Rôle : construction des sous-ensembles. Chaque état de l'AFD est un ensemble d'états de l'AFN. Ajoute AUTRE à l'alphabet si la regex a un point.
- Méthode principale : `convertir` (`:109`).
- Complexité (Javadoc) : O(s · (q + t)) par état de l'AFD, et jusqu'à 2^q états dans le pire cas.
- Q : comment le point est-il traité ? R : `move` suit les flèches `"."` pour toute lettre, et seulement elles pour AUTRE. Tout se décide dans l'automate, sans retour arrière.

**`Minimisation`** (`src/automate/Minimisation.java:26`)
- Rôle : algorithme de Moore. On sépare les états qui se comportent différemment jusqu'à ce que plus rien ne change, puis on fusionne et on retire le puits.
- Méthode principale : `minimiser` (`:357`).
- Complexité (Javadoc) : O(n³ · s) au pire, avec n états et s symboles ; le retrait du puits coûte O(n · s).
- Q : pourquoi compléter avec un puits avant Moore ? R : Moore compare les transitions de chaque état sur chaque symbole : il faut qu'elles existent toutes.

**`ConstructionAutomate`** (`src/automate/ConstructionAutomate.java:13`)
- Rôle : enchaîne AFN-ε, AFD, AFD minimal et table, et garde chaque étape pour `--debug` et `--stats`.
- Méthode principale : le constructeur (`:26`).
- Complexité : la somme des étapes.
- Q : pourquoi garder les étapes intermédiaires ? R : pour afficher leur taille avec `--stats` et leur contenu avec `--debug`.

**`AfdCompile`** (`src/automate/AfdCompile.java:33`)
- Rôle : met l'AFD minimal dans un tableau `int[états][257]`. Une case vaut -1 (`AUCUNE`) s'il n'y a pas de transition. Une petite table triée garde les caractères ≥ 256 qui sont dans la regex.
- Méthode principale : `suivant` (`:138`).
- Complexité (Javadoc) : construction en O(n · 256 · log s) ; O(1) par caractère de code inférieur à 256, O(log h) sinon.
- Q : pourquoi pas une HashMap ? R : une case de tableau se lit en une instruction, sans créer d'objet. C'est la boucle la plus chaude du programme.

**`Matcher`** (`src/experimentation/Matcher.java:9`)
- Rôle : l'interface commune aux trois stratégies de recherche, avec une seule méthode, `contientMotif` (`:18`).
- Méthode principale : `contientMotif`.
- Complexité : celle de l'implémentation.
- Q : à quoi sert l'interface ? R : `FileSearcher` ne connaît qu'elle. On change d'algorithme sans toucher à la lecture du fichier.

**`MatcherNaif`** (`src/experimentation/MatcherNaif.java:15`)
- Rôle : lance l'AFD de R depuis chaque position de la ligne. Chaque essai s'arrête dès qu'il n'y a plus de transition, grâce au puits retiré.
- Méthode principale : `contientMotif` (`:37`).
- Complexité (Javadoc) : O(n²) par ligne dans le pire cas.
- Q : quand est-il très lent ? R : quand `.*` fait lire toute la fin de la ligne à chaque essai et que rien n'est trouvé. Avec `e.*zq` sur un livre en une seule ligne : 362,5 ms, contre 0,161 ms en une passe.

**`MatcherUnePasse`** (`src/experimentation/MatcherUnePasse.java:26`)
- Rôle : lit la ligne une seule fois avec l'AFD de `.*R` et s'arrête au premier état final.
- Méthodes principales : `contientMotif` (`:60`) et `envelopper` (`:46`).
- Complexité (Javadoc) : O(n) par ligne.
- Q : quel est le prix à payer ? R : l'AFD de `.*R` peut avoir plus d'états que celui de R : 8 au lieu de 6 pour `Gr(e|a)*g.r`.

**`Kmp`** (`src/kmp/Kmp.java:39`)
- Rôle : recherche d'un motif littéral sans automate, grâce à la table π. Ne recule jamais dans le texte.
- Méthode principale : `contientMotif` (`:90`), avec `calculerEchec` (`:58`).
- Complexité (Javadoc) : O(m) pour la table, O(n) par ligne, donc O(n + m).
- Q : quel lien avec les automates ? R : l'AFD minimal de `.*P` est l'automate de KMP, avec m + 1 états. KMP le simule sans le construire.

**`FileSearcher`** (`src/experimentation/FileSearcher.java:12`)
- Rôle : lit le texte ligne par ligne, demande au matcher, et range le résultat dans un `ResultatRecherche`. Il n'affiche rien.
- Méthode principale : `rechercher` (`:33`).
- Complexité : un appel du matcher par ligne.
- Q : pourquoi ne pas afficher directement ? R : pour séparer la recherche de l'affichage. `Main` décide seul : lignes, `-n` ou `-c`.

**`ResultatRecherche`** (`src/experimentation/ResultatRecherche.java:12`)
- Rôle : garde les lignes trouvées avec leur numéro, et compte les lignes et les caractères lus (pour `--stats`).
- Méthode principale : `ajouterLigneLue` (`:66`).
- Complexité : O(1) par ligne.
- Q : pourquoi garder le numéro ? R : pour l'option `-n`, qui affiche `numéro:ligne` comme `grep -n`.

**`Affichage`** (`src/affichage/Affichage.java:30`)
- Rôle : tout l'affichage de `--debug` (AST, automates, table π, bilan), sur le flux qu'on lui donne. Il ne fait pas partie de la recherche.
- Méthodes principales : `afficherAst` (`:45`), `afficherAutomate` (`:260` et `:312`), `afficherKmp` (`:363`).
- Complexité : sans objet.
- Q : pourquoi sur la sortie d'erreur ? R : pour que la sortie standard reste identique à celle de grep, même avec `--debug`.

## 5. Les 10 questions les plus probables

**1. Pourquoi chercher `.*R` et pas R ?**
On veut savoir si R apparaît n'importe où dans la ligne. L'automate de `.*R` arrive dans un état final exactement à la fin de la première occurrence de R. On lit donc la ligne une seule fois, de gauche à droite, en O(n).

**2. Pourquoi retirer l'état puits ?**
Le puits est un état d'où l'on ne peut plus jamais gagner. Sans lui, une transition absente veut dire « c'est perdu », et le matcher naïf arrête son essai tout de suite au lieu de lire la fin de la ligne pour rien.

**3. À quoi sert AUTRE ?**
Le point accepte n'importe quel caractère, mais on ne peut pas créer une transition par caractère Unicode. AUTRE regroupe tous les caractères absents de la regex, qui se comportent tous pareil. Dans la table, leur colonne reçoit une copie de la transition sur AUTRE.

**4. Pourquoi minimiser ?**
Moins d'états donne une table plus petite, qui tient mieux dans le cache du processeur. Pour `Gr(e|a)*g.r`, on passe de 8 à 6 états. La minimisation sert aussi à repérer le puits, qu'on retire ensuite.

**5. Pourquoi le matcher naïf est-il souvent plus rapide sur de vrais textes ?**
Dans un vrai texte, la plupart des essais meurent dès le premier caractère, puisque le puits est retiré : `S(a|g|r)*on` sur Don Quixote prend 4,5 ms en naïf contre 7,1 ms en une passe. Notre hypothèse, non vérifiée par profilage : les essais du naïf sont indépendants, alors que la une passe enchaîne des lectures de table qui dépendent les unes des autres. Mais le naïf peut devenir quadratique, d'où notre choix de la une passe par défaut.

**6. Pourquoi KMP pour les motifs littéraux ?**
Pour un mot simple, construire un automate est inutile. La table π se calcule en O(m), en moins de 0,04 ms, contre jusqu'à 10,5 ms pour l'automate d'un littéral de 70 caractères. La recherche est aussi plus rapide : 3,3 à 5,8 ms contre 7,1 à 9,5 ms sur Don Quixote.

**7. Pourquoi notre programme est-il plus lent que grep ?**
Le démarrage de la JVM coûte à lui seul 58,5 ms, contre 1,4 ms pour grep. Ensuite, on décode chaque ligne en `String` Java, alors que grep travaille directement sur les octets avec des optimisations très poussées. Au total, grep est 5 à 75 fois plus rapide : 5,2 avec `a.c` sur 32 Mio, 75 avec `S(a|g|r)*on` sur The Yellow Wallpaper.

**8. Comment savez-vous que vous affichez les mêmes lignes que grep ?**
`tests/verifier.sh` compare octet par octet notre sortie et notre code de sortie à ceux de `grep -E`, sur 41 regex et 9 textes, dans 5 configurations : 1 611 comparaisons, 0 erreur. `tests/TestKmp.java` vérifie KMP sur 20 012 cas.

**9. Que se passe-t-il si l'AFD explose ?**
Avec `(a|b)*a(a|b)…(a|b)` (n fois `(a|b)`), l'AFD a 2^(n+1) états : 512 pour n = 8, comme le prévoit la théorie. La construction prend alors 22,5 ms, mais la recherche reste en O(n), parce qu'il n'y a toujours qu'une case de table à lire par caractère.

**10. Comment gérez-vous les accents et l'UTF-8 ?**
On lit le texte en UTF-8 strict : un fichier non UTF-8 donne une erreur, code 2. En Java, un caractère est un `char`, comme `.` dans grep en locale UTF-8. Les caractères de code inférieur à 256 (`é`, `à`) ont leur colonne dans la table. Les autres (`’`, `“`) passent par une petite table triée ou par la colonne AUTRE.

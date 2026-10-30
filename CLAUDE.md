# CLAUDE.md

Contexte du projet pour toutes les sessions de travail. À lire avant toute modification.

## But

Clone de la commande `egrep` en Java, projet 1 de l'UE DAAR.

Syntaxe acceptée (ERE simplifiée) :

- parenthèses `( )` ;
- alternative `|` ;
- concaténation (juxtaposition) ;
- étoile `*` ;
- point `.` : caractère universel ;
- lettres ASCII (caractères littéraux).

Exigence : pour un motif et un fichier donnés, afficher **exactement les mêmes lignes** que `grep -E` (mêmes lignes, même ordre, même contenu).

## Pipeline

```
motif ─► Parser ─► AST ─► AFN-ε ─► AFD ─► AFD minimal ─► recherche ligne par ligne
```

| Étape | Classe(s) | Rôle |
|---|---|---|
| Analyse syntaxique | `parser/Parser` | Descente récursive : `parseExpression` (`\|`) → `parseConcat` → `parseRepeat` (`*`) → `parseAtom` (lettre, `.`, `( )`). Une concaténation vide (motif vide, `a\|`, `\|a`, `a\|\|b`, `()`) produit `AstVide`, comme `grep -E` qui accepte tous ces cas. |
| AST | `ast/` : `AstChar`, `AstDot`, `AstVide`, `AstConcat`, `AstOr`, `AstRepeat`, créés par `AstFactory` | Arbre binaire, parcouru avec le patron visiteur (`ast/interfaces/AstVisitor`). |
| AFN-ε | `automate/AstToAfnVisitor` | Construction d'Aho-Ullman : un AFN par nœud, reliés par des ε-transitions. `AstVide` donne un seul état, à la fois initial et final. |
| AFD | `automate/Determinisation` | Méthode des sous-ensembles (fermeture ε + `move`). |
| AFD minimal | `automate/Minimisation` | Algorithme de Moore : suppression des états inaccessibles, complétion par un état puits, raffinement des partitions jusqu'au point fixe, puis **retrait du puits** (états depuis lesquels aucun état final n'est atteignable) et des transitions qui y mènent. L'AFD minimal n'est donc pas complet, et le nombre d'états affiché n'inclut pas le puits. |
| Table de transitions | `automate/AfdCompile` | `int[nbEtats][257]` : colonnes 0 à 255 pour les caractères de code < 256, colonne 256 pour `AUTRE`, -1 s'il n'y a pas de transition. S'y ajoutent `boolean[] finaux` et l'indice de l'état initial. Un caractère ≥ 256 de l'alphabet (« ’ » dans la regex) a sa propre transition, dans une table de secours triée (recherche dichotomique, sans HashMap) ; tout autre caractère ≥ 256 va dans la colonne `AUTRE`. |
| Construction | `automate/ConstructionAutomate` | Enchaîne AFN → AFD → AFD minimal → `AfdCompile` à partir d'un AST, et garde chaque étape pour `--debug` et `--stats`. |
| Recherche | `experimentation/Matcher` (interface), `MatcherNaif`, `MatcherUnePasse`, `FileSearcher`, `ResultatRecherche` | Voir « Matchers » ci-dessous. `FileSearcher` dépend de l'interface `Matcher`, lit un `BufferedReader` (fichier ou entrée standard) ligne par ligne et renvoie un `ResultatRecherche` (lignes trouvées avec leur numéro, compteurs). Il n'imprime rien. |
| KMP | `kmp/Kmp` | 2e stratégie, pour les motifs littéraux (voir « KMP » ci-dessous). Implémente `Matcher` et remplace tout le pipeline d'automates. |

Automates (`automate/Afn`, `Afd`, `Etat`, `Transition`) : les symboles sont des `String`. Dans l'AFN, l'ε-transition est le symbole `"EPSILON"` et le point est le symbole `"."`.

Gestion du point dans l'AFD : l'alphabet (`Afd.getAlphabet()`, calculé par `Determinisation`, recopié par `Minimisation`) contient les lettres de la regex, plus `Afd.AUTRE` si la regex contient un point. `AUTRE` représente « n'importe quel caractère absent de l'alphabet ». `move(S, c)` suit les transitions sur `c` et sur `"."` ; `move(S, AUTRE)` ne suit que celles sur `"."`. `AfdCompile` envoie dans la colonne `AUTRE` tout caractère qui n'est pas dans l'alphabet. Il n'y a pas de retour arrière : tout est dans l'automate.

### Matchers

Les deux matchers lisent `ligne.charAt(i)` et la table d'`AfdCompile` : ni `String.valueOf`, ni HashMap dans la boucle.

- `MatcherNaif` (`--matcher=naif`) : automate de R, relancé depuis chaque position de départ. Il s'arrête au premier état final, et chaque essai s'arrête dès qu'il n'y a plus de transition (puits retiré). Complexité O(n²) par ligne dans le pire cas.
- `MatcherUnePasse` (`--matcher=unepasse`, par défaut) : automate de `.*R`. `MatcherUnePasse.envelopper` transforme l'AST en `Concat(Repeat(Dot), R)` avant la construction d'Aho-Ullman, puis on déterminise et on minimise comme d'habitude. La ligne est lue une seule fois de gauche à droite, avec arrêt au premier état final (ou tout de suite si l'état initial est final, c'est-à-dire si R accepte le mot vide). Complexité O(n) par ligne. C'est équivalent à chercher R n'importe où : le préfixe `ligne[0..j[` est dans `.*R` si et seulement si une occurrence de R se termine en j. Contrepartie : l'AFD de `.*R` peut avoir plus d'états que celui de R (exponentiellement plus en théorie).

### KMP

- **Quand** : `Kmp.motifLitteral(ast)` renvoie le mot si l'AST ne contient que des `AstChar` et des `AstConcat`, sinon `null`. `AstVide`, `AstDot`, `AstOr` et `AstRepeat` excluent KMP. Les parenthèses n'existent pas dans l'AST : `(Eli)za(beth)` est le littéral `Elizabeth`.
- **Table d'échec** : `π[i]` est la longueur du plus long bord propre de `P[0..i]`. Elle se calcule en O(m) (par exemple, `ababc` donne `[0, 0, 1, 2, 0]`, visible avec `--debug`).
- **Recherche** : `contientMotif` est en O(n) et ne recule jamais dans le texte. En cas d'échec après q caractères reconnus, on passe à `q = π[q-1]` et on compare de nouveau le même caractère. Le coût total est O(n + m).
- **Lien avec les automates** : l'AFD minimal de `.*P` est l'automate de recherche de KMP, avec m + 1 états (vérifié pour les six motifs du tableau de l'état du projet). KMP le simule sans le construire.
- **Tests** : `tests/TestKmp.java` (sans JUnit) compare `Kmp` à `String.contains` sur les cas pièges (`aab`/`aaab`, `aa`/`aaa`, `abab`/`abaabab`, `ababc`/`abababc`, `aaaab`/`aaaaaaab`), sur des cas limites (motif d'une lettre, motif plus long que la ligne, ligne vide, « ’ ») et sur 20 000 couples aléatoires sur {a, b} (graine fixe). Il s'arrête avec le code 1 au premier échec. Une version volontairement fausse de `Kmp` (q remis à 0 au lieu de π[q-1]) est bien détectée.

`Main` lit les arguments, choisit l'algorithme (`--algo`), puis construit soit la table KMP, soit l'automate (de R ou de `.*R` selon le matcher) avec `ConstructionAutomate`. Il lance ensuite la recherche et décide seul de l'affichage. `affichage/Affichage` contient tout le code d'affichage de l'AST, des automates et de la table KMP (pour `--debug`), et écrit sur le flux qu'on lui passe.

## Conventions

- Code et commentaires en français.
- Java 11 minimum (le code utilise `Path.of` et `Set.of`).
- Aucune bibliothèque externe : uniquement la bibliothèque standard Java.
- Javadoc courte sur chaque classe et chaque méthode publique.

## Compilation et lancement

```bash
javac --release 11 -encoding UTF-8 -d bin $(find src tests -name '*.java')

java -cp bin TestKmp                                # tests de KMP (code 1 au premier échec)
java -cp bin Main [options] "regex" [fichier]      # sans fichier (ou avec -) : entrée standard
cat data/text1.txt | java -cp bin Main 'a.c'
```

| Option | Effet |
|---|---|
| (aucune) | lignes qui contiennent le motif, telles quelles, comme `grep -E` |
| `-c` | nombre de lignes trouvées seulement (prioritaire sur `-n`, comme grep) |
| `-n` | préfixe `numéro:` devant chaque ligne, comme `grep -n` |
| `--algo=auto` | KMP si le motif est littéral, sinon l'automate avec le matcher de `--matcher` (**par défaut**) |
| `--algo=automate` | toujours l'automate |
| `--algo=kmp` | toujours KMP ; sur une regex non littérale : erreur, code 2 |
| `--matcher=unepasse` | (automate) recherche en une passe avec l'automate de `.*R` (**par défaut**) |
| `--matcher=naif` | (automate) automate de R relancé depuis chaque position de la ligne |
| `--debug` | AST, puis soit la table π de KMP, soit AFN-ε, AFD, AFD minimal (avec alphabet) et bilan des tailles **de l'automate réellement utilisé** (celui de `.*R` avec `unepasse`), **sur stderr** |
| `--stats` | algorithme réellement utilisé. Pour KMP : longueur du motif et temps de construction de la table. Pour l'automate : matcher, nombre d'états (AFN-ε, AFD, AFD minimal sans puits) et temps de construction. Puis temps de recherche en ms et compteurs. Le tout **sur stderr** ; les temps de construction incluent l'analyse de la regex. |
| `--` | fin des options (motif qui commence par `-`) |

Les options courtes peuvent être groupées (`-cn`) et placées avant ou après la regex. La sortie standard ne contient que le résultat, même avec `--debug` et `--stats`. Codes de sortie : 0 si au moins une ligne est trouvée, 1 si aucune, 2 en cas d'erreur (regex invalide, fichier introuvable, texte non UTF-8, regex manquante, option inconnue, matcher ou algorithme inconnu, `--algo=kmp` sur une regex non littérale, trop d'arguments). En cas d'erreur, le message et l'usage vont sur stderr. Lecture en UTF-8 strict ; `System.out` et `System.err` sont des `PrintStream` UTF-8, et les fins de ligne sont des `\n`, quelle que soit la locale.

Il faut un `javac` de version 11 ou plus. Sur cette machine, `openjdk-21-jdk` est installé : `javac` 21 est le compilateur par défaut, et Ant utilise ce même JDK. `--release 11` garantit que le code reste compatible Java 11.

`bin/` et `*.jar` sont dans `.gitignore`.

## Commande de référence

```bash
LC_ALL=C.UTF-8 grep -E 'MOTIF' FICHIER        # Linux
LC_ALL=en_US.UTF-8 grep -E 'MOTIF' FICHIER    # macOS
```

La locale UTF-8 est indispensable : elle fait correspondre `.` à un **caractère** et non à un octet, comme un `char` en Java. Les fichiers de `data/` sont en UTF-8 (« ’ », « “ »…). Ils ne contiennent ni caractère hors du plan multilingue de base (un caractère = un `char`), ni `\r`.

Dans le shell de Claude Code, `grep` peut être une fonction qui redirige vers ugrep. Utiliser `command grep -E` (ou `/usr/bin/grep -E`, GNU grep 3.11) pour obtenir la vraie référence.

## Règle de vérification

Après **toute** modification du moteur (`parser/`, `ast/`, `automate/`, `experimentation/`, `kmp/`) :

1. recompiler, puis lancer `java -cp bin TestKmp` ;
2. comparer la sortie du programme avec la commande de référence sur **chaque** fichier `data/*.txt`, avec des motifs qui couvrent chaque opérateur ;
3. traiter toute différence comme un bug et la corriger avant de continuer.

Boucle de comparaison. Elle compte quatre passes, chacune en sortie par défaut puis avec `-n` : automate avec le matcher naïf, automate avec le matcher une passe, KMP sur les motifs littéraux seulement, puis les options par défaut (`--algo=auto`).

```bash
compare() {  # $1 = options de Main, $2 = motif, $3 = fichier
  diff <(java -cp bin Main $1 "$2" "$3") <(LC_ALL=C.UTF-8 command grep -E "$2" "$3") > /dev/null \
    && echo "OK     [$1] $3  $2" || echo "ECHEC  [$1] $3  $2"
  diff <(java -cp bin Main $1 -n "$2" "$3") <(LC_ALL=C.UTF-8 command grep -n -E "$2" "$3") > /dev/null \
    && echo "OK -n  [$1] $3  $2" || echo "ECHEC -n  [$1] $3  $2"
}
litteraux=('the' 'Elizabeth' 'Darcy' 'Sargon' 'abab' 'aab' 'Danatoff’s' 'é' \
           'husband in the_ Spectator, _who describes, with all the gusto and all the')
motifs=('S.on' 'a.c' 'D.r.y' 'm.n' '.' 'a.*b' '(a|.)b' '(th|wh)e*n' 'S(a|g|r)*on' \
        '(a|b)*c(d|e)*f' 'ab*c' '(Dar|Bing)ley' 'th(e|a)*t' \
        '' 'a|' '|a' '()' '(a|)b' '“.' '(’|“)(s|M)' 'n’*t' "${litteraux[@]}")
for f in data/*.txt; do
  for m in "${motifs[@]}"; do
    compare "--algo=automate --matcher=naif" "$m" "$f"
    compare "--algo=automate --matcher=unepasse" "$m" "$f"
    compare "" "$m" "$f"
  done
  for m in "${litteraux[@]}"; do
    compare "--algo=kmp" "$m" "$f"
  done
done
```

Les motifs qui contiennent « ’ », « “ » ou « é » testent la table de secours d'`AfdCompile` et KMP sur des caractères de code ≥ 256. Le dernier littéral (73 caractères) est la plus longue ligne de prose de `text1.txt` sans caractère spécial pour nous ni pour `grep -E`.

En cas de changement de l'interface, tester aussi : les codes de sortie (0, 1 et 2, comparés à grep), `-c`, la lecture sur l'entrée standard (`cat f | java -cp bin Main m`) et le fait que stdout reste identique à grep avec `--debug` et `--stats`. Pour tester une autre locale (`LC_ALL=C`), copier `bin/` et `data/` dans un chemin en ASCII : le « é » de `Téléchargements` empêche la JVM de trouver ses classes en locale C.

## État du projet (vérifié le 30/10/2026)

Conformes (396 comparaisons, toutes avec un diff identique) :
- les 30 motifs de la boucle ci-dessus, sur les deux fichiers, avec l'automate et le matcher naïf, avec l'automate et le matcher une passe, et avec les options par défaut ;
- les 9 motifs littéraux avec `--algo=kmp` ;
- à chaque fois en sortie par défaut et avec `-n`.

`TestKmp` réussit ses 20 012 tests. C'est aussi le cas de `-c`, de l'entrée standard, de `--stats` et `--debug` (stdout inchangé), de `LC_ALL=C` et des codes de sortie 0, 1 et 2. `S(a|g|r)*on` trouve 0 ligne dans les deux fichiers, donc il ne teste rien en pratique.

Écarts connus avec GNU `grep -E`, hors de l'ERE simplifiée :

- `)` sans parenthèse ouvrante et `*` en début d'expression : grep les lit comme des caractères littéraux (avec un avertissement pour `*`), nous les refusons (code 2).
- `+ ? [ ] { } ^ $ \` : ce sont des opérateurs pour `grep -E`, mais des caractères littéraux pour notre parser (et donc pour KMP). Les motifs de test doivent les éviter.
- Texte non UTF-8 : nous refusons (code 2), alors que grep affiche « binary file matches ».

Performances : temps de recherche sur `data/text1.txt` (`--stats`, médiane de 5 lancements). « Avant » correspond au commit `a2df701` (AFD en HashMap, `String.valueOf` par caractère, puits conservé, relance depuis chaque position).

| regex | états min. R | états min. `.*R` | avant | naïf | une passe |
|---|---:|---:|---:|---:|---:|
| `the` | 4 | 4 | 86 ms | 37 ms | 39 ms |
| `Elizabeth` | 10 | 10 | 120 ms | 46 ms | 54 ms |
| `S(a\|g\|r)*on` | 4 | 4 | 116 ms | 50 ms | 49 ms |
| `a.*b` | 3 | 3 | 552 ms | 45 ms | 46 ms |
| `(a\|.)b` | 3 | 3 | 594 ms | 47 ms | 54 ms |
| `m.n` | 4 | 6 | 758 ms | 62 ms | 42 ms |

Un motif qui ne trouve rien (`zzzq`) donne déjà environ 46 ms : sur ce fichier, la lecture, le décodage UTF-8 et le démarrage du JIT dominent, et l'écart entre naïf et une passe se perd dans le bruit. La différence de complexité se voit sur un pire cas : sur une ligne de 50 000 « a » avec `a.*b`, le naïf prend 3 570 ms et l'une-passe 12,6 ms.

KMP contre automate une passe sur `data/text1.txt` (`--stats`, médiane de 5 lancements). Ces temps incluent la lecture et le décodage du fichier (environ 35 à 45 ms à eux seuls), donc **on ne peut pas encore en tirer de conclusion** : les vraies mesures se feront à l'étape des benchmarks.

| motif | m | lignes trouvées | KMP | automate une passe | états min. `.*P` |
|---|---:|---:|---:|---:|---:|
| `the` | 3 | 5688 | 33 ms | 40 ms | 4 |
| `Elizabeth` | 9 | 644 | 37 ms | 47 ms | 10 |
| `Darcy` | 5 | 429 | 37 ms | 46 ms | 6 |
| `Sargon` | 6 | 0 | 37 ms | 48 ms | 7 |
| `abab` | 4 | 0 | 36 ms | 48 ms | 5 |
| ligne de 73 caractères (voir la boucle) | 73 | 1 | 35 ms | 45 ms | 74 |

Reste à faire : `build.xml`, `.jar`, tests des autres classes (seul `tests/TestKmp.java` existe), Javadoc (présente sur `Main`, `Affichage`, `Parser`, `FileSearcher`, `ResultatRecherche`, `Matcher`, `MatcherNaif`, `MatcherUnePasse`, `AfdCompile`, `ConstructionAutomate`, `Kmp`, `TestKmp`, `AstVide`, `AstFactory`, `Etat`, `Minimisation.minimiser`, et sur les ajouts dans `Afd` et `Determinisation` ; absente ailleurs), README (vide), rapport.

À savoir : les méthodes `static` de `AstFactoryI` ne sont pas héritées par `AstFactory`, donc l'interface ne contraint rien. Elle sert seulement de documentation des signatures, alignées depuis le 30/10/2026 (`createChar(Character)`, `createVide()`).

## Livrables

À rendre sur Moodle le **31/10/2026 à 23h59** :

- `build.xml` (Ant) ;
- `.jar` exécutable ;
- `README` ;
- code commenté ;
- dossier de tests ;
- rapport de 5 à 10 pages (12 au maximum) ;
- le tout dans l'archive `daar-assignment-algorithm-NOM1-NOM2.zip`, de moins de 10 Mo.

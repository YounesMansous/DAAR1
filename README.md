# egrep-clone

Clone de la commande `egrep` en Java (projet 1 de l'UE DAAR). Il accepte l'ERE simplifiée du sujet : parenthèses, alternative `|`, concaténation, étoile `*`, point `.` et lettres. Pour un motif et un fichier donnés, il affiche **exactement les mêmes lignes que `grep -E`**, ce que vérifie un banc de 1 611 comparaisons. La recherche passe par un automate (AFN-ε, AFD, puis AFD minimal), ou par KMP quand le motif est un simple mot.

## Prérequis

- Java 11 ou plus (le jar est compilé en `--release 11`) ;
- Apache Ant (ou `make`, qui appelle Ant) ;
- une locale UTF-8 (`LC_ALL=C.UTF-8` sous Linux, `en_US.UTF-8` sous macOS) : sans elle, Java abîme les caractères non ASCII du motif ;
- Python 3 et matplotlib : seulement pour les graphiques des benchmarks, qui ne sont pas encore écrits (voir plus bas).

## Compilation

```bash
ant jar          # ou : make     → egrep-clone.jar à la racine
ant clean        # supprime bin/, bin-tests/, doc/ et le jar
ant doc          # Javadoc HTML dans doc/
```

Sans Ant :

```bash
javac --release 11 -encoding UTF-8 -d bin $(find src -name '*.java')
java -cp bin Main 'S(a|g|r)*on' data/babylon-56667.txt
```

## Utilisation

```bash
java -jar egrep-clone.jar [options] "regex" [fichier]   # sans fichier, ou avec - : entrée standard
```

Syntaxe : `( )`, `|`, `*`, `.` (n'importe quel caractère), et tout autre caractère est une lettre. `+ ? [ ] { } ^ $ \` ne sont pas des opérateurs ici. Le motif vide, `a|` et `()` sont acceptés, comme par grep.

Exemple du sujet (30 lignes, comme `grep -E`) :

```bash
java -jar egrep-clone.jar 'S(a|g|r)*on' data/babylon-56667.txt
```

| Option | Effet | Exemple |
|---|---|---|
| `-c` | nombre de lignes trouvées | `java -jar egrep-clone.jar -c Sargon data/babylon-56667.txt` → `29` |
| `-n` | préfixe `numéro:` | `java -jar egrep-clone.jar -n Sargon data/babylon-56667.txt` → `432:    state--Sargon and…` |
| `--algo=auto` | KMP si le motif est littéral, sinon automate (défaut) | `java -jar egrep-clone.jar --algo=auto Elizabeth data/text1.txt` |
| `--algo=automate` | toujours l'automate | `java -jar egrep-clone.jar --algo=automate Elizabeth data/text1.txt` |
| `--algo=kmp` | toujours KMP (motif littéral obligatoire, sinon code 2) | `java -jar egrep-clone.jar --algo=kmp Elizabeth data/text1.txt` |
| `--matcher=unepasse` | automate de `.*R`, une seule lecture de la ligne (défaut) | `java -jar egrep-clone.jar --matcher=unepasse 'a.*b' data/text1.txt` |
| `--matcher=naif` | automate de R, relancé depuis chaque position | `java -jar egrep-clone.jar --matcher=naif 'a.*b' data/text1.txt` |
| `--stats` | algorithme, taille des automates, temps (sur stderr) | `java -jar egrep-clone.jar --stats 'S.*on' data/babylon-56667.txt > /dev/null` |
| `--debug` | AST, AFN-ε, AFD, AFD minimal ou table KMP (sur stderr) | `java -jar egrep-clone.jar --debug 'ab*c' data/alice-11.txt > /dev/null` |
| `--` | fin des options (motif qui commence par `-`) | `java -jar egrep-clone.jar -c -- '--' data/text1.txt` → `444` |

Les options courtes se groupent (`-cn`). La sortie standard ne contient que le résultat, même avec `--debug` et `--stats`. Codes de sortie, comme grep : **0** au moins une ligne trouvée, **1** aucune, **2** erreur (regex invalide, fichier introuvable, texte non UTF-8, option inconnue).

## Tests

```bash
ant test         # ou : make test
```

Lance `TestKmp` (KMP comparé à `String.contains` sur 20 012 cas), puis `tests/verifier.sh`, qui compare octet par octet la sortie et le code de sortie du jar à ceux de `grep -E` : 41 regex × 9 textes de `data/`, dans 5 configurations (défaut, naïf, une passe, KMP, `-n`), soit 1 611 comparaisons. La cible échoue au moindre écart. Détails, locale et provenance des textes : [tests/README.md](tests/README.md).

## Benchmarks

Pas encore écrits : `ant bench` affiche seulement « à venir ». Ils seront décrits dans `bench/README.md`.

## Architecture

```
regex ─► Parser ─► AST ─► AFN-ε ─► AFD ─► AFD minimal ─► table ─► matcher (naïf ou une passe)
                    └──── motif littéral ? ──────────────────────► KMP
```

| Paquet (`src/`) | Rôle |
|---|---|
| `Main` | ligne de commande : options, choix de l'algorithme, affichage, codes de sortie |
| `parser/` | descente récursive de la regex, produit l'AST |
| `ast/` | nœuds de l'AST (lettre, point, mot vide, concaténation, alternative, étoile) et visiteur |
| `automate/` | Thompson (AFN-ε), sous-ensembles (AFD), Moore et retrait du puits (AFD minimal), table `int[états][257]` |
| `experimentation/` | matchers naïf (O(n²) par ligne) et une passe sur `.*R` (O(n)), lecture du texte ligne par ligne |
| `kmp/` | Knuth-Morris-Pratt pour les motifs littéraux, en O(n + m) |
| `affichage/` | affichage de l'AST, des automates et de la table KMP pour `--debug` |

Le point est géré dans l'automate lui-même : le symbole `AUTRE` représente tous les caractères absents de la regex, donc il n'y a jamais de retour arrière. La Javadoc (`ant doc`) détaille chaque étape et sa complexité.

## Écarts connus avec `grep -E`

Tous sont hors de l'ERE simplifiée du sujet :

- `)` sans parenthèse ouvrante, ou `*` en début de motif : grep les lit comme des lettres, nous les refusons (code 2) ;
- `+ ? [ ] { } ^ $ \` sont des opérateurs pour grep, mais des lettres pour nous ;
- texte non UTF-8 : nous le refusons (code 2), alors que grep affiche « binary file matches » ;
- locale non UTF-8 : Java décode mal un motif non ASCII (`é`, `’`) ; le programme l'affiche en avertissement sur stderr. Lancez-le avec `LC_ALL=C.UTF-8`.

## Auteurs

NOM1, NOM2

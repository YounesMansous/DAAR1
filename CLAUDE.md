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
| Analyse syntaxique | `parser/Parser` | Descente récursive : `parseExpression` (`\|`) → `parseConcat` → `parseRepeat` (`*`) → `parseAtom` (lettre, `.`, `( )`). |
| AST | `ast/` : `AstChar`, `AstDot`, `AstConcat`, `AstOr`, `AstRepeat`, créés par `AstFactory` | Arbre binaire, parcouru avec le patron visiteur (`ast/interfaces/AstVisitor`). |
| AFN-ε | `automate/AstToAfnVisitor` | Construction d'Aho-Ullman : un AFN par nœud, reliés par des ε-transitions. |
| AFD | `automate/Determinisation` | Méthode des sous-ensembles (fermeture ε + `move`). |
| AFD minimal | `automate/Minimisation` | Algorithme de Moore : suppression des états inaccessibles, complétion par un état puits, raffinement des partitions jusqu'au point fixe. |
| Recherche | `experimentation/AfdMatcher`, `experimentation/FileSearcher` | `AfdMatcher` teste chaque position de départ de la ligne ; `FileSearcher` lit le fichier en UTF-8, ligne par ligne. |
| KMP (prévu) | à créer | Pour les motifs qui ne sont qu'une concaténation de lettres (ni `.`, ni `\|`, ni `*`, ni parenthèses). |

Automates (`automate/Afn`, `Afd`, `Etat`, `Transition`) : les symboles sont des `String`. Dans l'AFN, l'ε-transition est le symbole `"EPSILON"` et le point est le symbole `"."`.

Gestion du point dans l'AFD : l'alphabet (`Afd.getAlphabet()`, calculé par `Determinisation`, recopié par `Minimisation`) contient les lettres de la regex, plus `Afd.AUTRE` si la regex contient un point. `AUTRE` représente « n'importe quel caractère absent de l'alphabet ». `move(S, c)` suit les transitions sur `c` et sur `"."` ; `move(S, AUTRE)` ne suit que celles sur `"."`. `AfdMatcher` remplace par `AUTRE` tout caractère lu qui n'est pas dans l'alphabet. Il n'y a pas de retour arrière : tout est dans l'automate.

`Main` enchaîne tout le pipeline et affiche l'AST et les automates.

## Conventions

- Code et commentaires en français.
- Java 11 minimum (le code utilise `Path.of` et `Set.of`).
- Aucune bibliothèque externe : uniquement la bibliothèque standard Java.
- Javadoc courte sur chaque classe et chaque méthode publique.

## Compilation

```bash
javac --release 11 -encoding UTF-8 -d bin $(find src -name '*.java')
```

Il faut un `javac` de version 11 ou plus. Sur cette machine, `openjdk-21-jdk` est installé : `javac` 21 est le compilateur par défaut, et Ant utilise ce même JDK. `--release 11` garantit que le code reste compatible Java 11.

`bin/` ne doit pas être commité.

## Commande de référence

```bash
LC_ALL=C.UTF-8 grep -E 'MOTIF' FICHIER        # Linux
LC_ALL=en_US.UTF-8 grep -E 'MOTIF' FICHIER    # macOS
```

La locale UTF-8 est indispensable : elle fait correspondre `.` à un **caractère** et non à un octet, comme un `char` en Java. Les fichiers de `data/` sont en UTF-8 (« ’ », « “ »…). Ils ne contiennent ni caractère hors du plan multilingue de base (un caractère = un `char`), ni `\r`.

Dans le shell de Claude Code, `grep` peut être une fonction qui redirige vers ugrep. Utiliser `command grep -E` (ou `/usr/bin/grep -E`, GNU grep 3.11) pour obtenir la vraie référence.

## Règle de vérification

Après **toute** modification du moteur (`parser/`, `ast/`, `automate/`, `experimentation/`, KMP) :

1. recompiler ;
2. comparer la sortie du programme avec la commande de référence sur **chaque** fichier `data/*.txt`, avec des motifs qui couvrent chaque opérateur ;
3. traiter toute différence comme un bug et la corriger avant de continuer.

Tant que `Main` est interactif (il lit le motif, le chemin et « o/n » sur l'entrée standard, puis préfixe chaque ligne trouvée par « N: »), la comparaison se fait ainsi :

```bash
for f in data/*.txt; do
  for m in 'S.on' 'a.c' 'D.r.y' 'm.n' '.' 'a.*b' '(a|.)b' 'the' '(th|wh)e*n' 'S(a|g|r)*on' \
           '(a|b)*c(d|e)*f' 'Elizabeth' 'ab*c' '(Dar|Bing)ley' 'th(e|a)*t'; do
    diff <(printf '%s\n%s\no\n' "$m" "$f" | java -cp bin Main | sed -n 's/^\([0-9][0-9]*\): /\1:/p') \
         <(LC_ALL=C.UTF-8 command grep -n -E "$m" "$f") > /dev/null \
      && echo "OK     $f  $m" || echo "ECHEC  $f  $m"
  done
done
```

Une fois l'interface `egrep MOTIF FICHIER` en place, comparer directement les deux sorties avec `diff`, sans `-n`.

## État du projet (vérifié le 30/10/2026)

Conformes (diff ligne à ligne identique à la référence sur `data/text1.txt` et `data/text2.txt`) : les 15 motifs de la boucle ci-dessus. Le point « . » fonctionne comme caractère universel depuis le 30/10/2026 (symbole `AUTRE`). Avant, `m.n` donnait 0 ligne contre 1912 pour grep. `S(a|g|r)*on` trouve 0 ligne dans les deux fichiers, donc il ne teste rien en pratique.

Écarts restants avec `grep -E` :

- Motif vide : `Parser` lève « fin inattendue », alors que `grep -E ''` affiche toutes les lignes.
- Sortie : `Main` est interactif, affiche l'AST, les automates et des statistiques, et préfixe les lignes par leur numéro. Le programme final doit prendre `MOTIF FICHIER` en arguments et n'afficher que les lignes trouvées.

Reste à faire : KMP, `build.xml`, `.jar`, dossier de tests, Javadoc (absente partout), README (vide), rapport.

À nettoyer : `main` et `afficherAst` dupliqués dans `Parser` ; champs et setters inutilisés dans `Etat` ; `AstFactoryI.createChar(String)` ne correspond pas à `AstFactory.createChar(Character)`.

## Livrables

À rendre sur Moodle le **31/10/2026 à 23h59** :

- `build.xml` (Ant) ;
- `.jar` exécutable ;
- `README` ;
- code commenté ;
- dossier de tests ;
- rapport de 5 à 10 pages (12 au maximum) ;
- le tout dans l'archive `daar-assignment-algorithm-NOM1-NOM2.zip`, de moins de 10 Mo.

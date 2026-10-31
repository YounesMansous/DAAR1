# Banc de performances

**Relancer** : `ant bench` (ou `make bench`), depuis la racine. Durée : environ 4 minutes (230 s le 30/10/2026). Le banc construit le jar, puis :

1. lance `Benchmark` (mesures dans la JVM), une JVM par expérience et par algorithme ;
2. relit le CSV pour vérifier que tous les algorithmes trouvent le même nombre de lignes ;
3. lance `bench_bout_en_bout.py` ;
4. lance `graphiques.py`.

On peut aussi lancer chaque partie seule :
- `java -cp bin:bin-bench Benchmark <a|b|c|d> <kmp|unepasse|naif|thompson>`, qui ajoute ses mesures au CSV ;
- `java -cp bin:bin-bench Benchmark --verifier` ;
- `python3 bench/bench_bout_en_bout.py` ;
- `python3 bench/graphiques.py`.

**Protocole dans la JVM** (`Benchmark.java`)
- Chaque fichier est chargé une seule fois en mémoire (`List<String>`). On ne chronomètre donc ni la lecture ni le décodage, qui pèsent 35 à 45 ms sur les 50 ms d'une recherche de bout en bout dans `text1.txt`.
- Pour chaque cas : 5 itérations de chauffe, puis 20 mesures avec `System.nanoTime`. La construction (analyse, puis AFD minimal et table, ou table KMP) et la recherche sur toutes les lignes sont chronométrées à part.
- Chaque configuration (expérience × algorithme) tourne dans sa propre JVM (`-Xmx1g`), comme le programme réel, qui n'utilise qu'un algorithme par lancement. Dans une JVM commune, le JIT compile le code partagé (`AfdCompile.suivant`) d'après les mesures précédentes : le naïf devenait 2 fois plus lent quand KMP et l'une-passe avaient tourné avant lui.

**Protocole de bout en bout** (`bench_bout_en_bout.py`)
- 8 regex × 3 fichiers : 50 Kio, 2,2 Mio, et un fichier généré de 32 Mio.
- Pour chaque cas : un lancement non chronométré, puis 10 lancements de `java -jar egrep-clone.jar -c` et de `grep -E -c`, chronométrés avec `time.perf_counter`, démarrage de la JVM compris.
- Le script exporte pour java comme pour grep la même locale UTF-8 que `tests/verifier.sh` : C.UTF-8, sinon en_US.UTF-8.

**Machine** : Intel Core i5-8350U (4 cœurs, 8 threads, 1,7 GHz), 7,6 Go de mémoire, Ubuntu 24.04 (Linux 6.17), OpenJDK 21.0.12 (bytecode Java 11), GNU grep 3.11, Python 3.12.3, matplotlib 3.6.3.

| Fichier | Contenu |
|---|---|
| `Benchmark.java` | expériences : a (KMP contre automate une passe, motifs littéraux de 2 à 70 caractères tirés du texte) ; b (naïf contre une passe, sur 6 regex × 3 textes et sur le pire cas `a.*b`) ; c (passage à l'échelle, de 1 à 32 Mio) ; d (taille et construction des automates) |
| `bench_bout_en_bout.py` | programme complet contre grep |
| `graphiques.py` | les 7 figures PNG en noir et blanc de `rapport/figures/` (moyenne ± écart-type de l'échantillon), et les chiffres clés sur la sortie standard |
| `../resultats/bench_jvm.csv` | une ligne par mesure : `experience, regex, fichier, taille_octets, algo, matcher, iteration, t_construction_ms, t_recherche_ms, nb_lignes, etats_afn, etats_afd, etats_min, etats_min_pointetoile`. Pour l'expérience d, `t_recherche_ms` et `nb_lignes` sont vides. |
| `../resultats/bench_bout_en_bout.csv` | une ligne par lancement : `regex, fichier, taille_octets, programme, iteration, t_ms, nb_lignes` |
| `ExportDot.java` | hors de `ant bench` : AFN-ε et AFD minimal d'une regex en `.dot` (`java -cp bin:bin-bench ExportDot <regex> <préfixe>`), états numérotés dans l'ordre de lecture (q0 initial, états finaux en dernier, sans titre), différemment de `--debug` |
| `TraceKmp.java` | hors de `ant bench` : π, CarryOver sous la forme du cours (m + 1 cases) et trace des retours de KMP sur des fichiers (`java -cp bin:bin-bench TraceKmp <motif> [fichier...]`) |
| `BenchUneLigne.java`, `graphique_une_ligne.py` | hors de `ant bench` : livre sur une seule ligne, matcher naïf contre une passe (même protocole, une JVM par matcher) ; commandes dans `../resultats/scenarios.md`, CSV `../resultats/bench_une_ligne.csv`, figure `fig_une_ligne.png` |
| `../resultats/demarrage_jvm.csv` | hors de `ant bench` : démarrage seul (`zzzq` sur `/dev/null`), jar contre grep, 10 lancements ; produit par une commande qui réutilise `choisir_locale` et `lancer` de `bench_bout_en_bout.py` (voir `../resultats/scenarios.md`, scénario 5) |
| `genere/` | fichiers de 1 à 32 Mio, formés des lignes de `data/*.txt` (par ordre de nom) répétées sans couper de ligne. Non versionné, supprimé à la fin de `ant bench` et par `ant clean`. |

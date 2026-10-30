# Tests

**Lancer** : `ant test` (ou `make test`) lance `TestKmp`, puis `tests/verifier.sh`. On peut aussi lancer `tests/verifier.sh` seul (`-v` affiche aussi les comparaisons réussies) ; il construit `egrep-clone.jar` s'il est absent. Les textes s'obtiennent avec `tests/telecharger_textes.sh`, qui saute les fichiers déjà présents.

**Ce qui est comparé** : pour chaque regex de `regex.txt` (41, par catégorie) et chaque texte de `data/`, la sortie de `java -jar egrep-clone.jar` est comparée octet par octet, ainsi que le code de sortie, à celle de `grep -E` en locale UTF-8 (`LC_ALL=C.UTF-8`, ou `en_US.UTF-8` sous macOS). Cinq configurations sont testées : options par défaut, `--algo=automate --matcher=naif`, `--algo=automate --matcher=unepasse`, `--algo=kmp` (motifs littéraux seulement) et `-n` contre `grep -n -E`. Le script renvoie 1 s'il y a au moins un écart. `TestKmp` compare KMP à `String.contains` sur des cas pièges et 20 000 couples aléatoires.

**Locale** : Java décode les arguments de la ligne de commande selon la locale du système, et ce décodage ne peut plus changer une fois la JVM lancée. Sans locale UTF-8 (`LANG` vide, `LC_ALL=C`), un motif comme `é`, `’` ou `“` arrive abîmé dans le programme (caractères U+FFFD), et la recherche est fausse. Le programme l'affiche alors en avertissement sur stderr. `verifier.sh` exporte donc `LC_ALL=C.UTF-8` (ou `en_US.UTF-8` si `C.UTF-8` n'existe pas) pour java comme pour grep, et affiche la locale utilisée. Pour lancer le jar à la main sur ces motifs : `LC_ALL=C.UTF-8 java -jar egrep-clone.jar …`.

**Textes** (`data/`, projet Gutenberg, UTF-8, `\r` supprimés, 5,3 Mo au total) :

| fichier | titre, auteur | n° | taille | langue |
|---|---|---:|---:|---|
| `yellow-wallpaper-1952.txt` | *The Yellow Wallpaper*, C. P. Gilman | 1952 | 51 Ko | anglais |
| `metamorphosis-5200.txt` | *Metamorphosis*, F. Kafka | 5200 | 140 Ko | anglais |
| `alice-11.txt` | *Alice's Adventures in Wonderland*, L. Carroll | 11 | 171 Ko | anglais |
| `candide-4650.txt` | *Candide, ou l'optimisme*, Voltaire | 4650 | 230 Ko | français |
| `text2.txt` | *The Double Thirteen*, A. Wynne | 79867 | 433 Ko | anglais |
| `tour-du-monde-800.txt` | *Le tour du monde en quatre-vingts jours*, J. Verne | 800 | 453 Ko | français |
| `babylon-56667.txt` | *A History of Babylon*, L. W. King (exemple du sujet ; commence par un BOM UTF-8) | 56667 | 750 Ko | anglais |
| `text1.txt` | *Pride and Prejudice*, J. Austen | 1342 | 757 Ko | anglais |
| `don-quixote-996.txt` | *Don Quixote*, M. de Cervantes (trad. anglaise) | 996 | 2,3 Mo | anglais |

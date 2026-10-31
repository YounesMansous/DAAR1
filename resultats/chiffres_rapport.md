# Chiffres des figures du rapport

Pour chaque figure de `rapport/figures/`, les valeurs qu'elle montre, avec le bloc de lignes du CSV source. Les numéros de ligne comptent l'en-tête comme ligne 1. Chaque bloc contient les 20 mesures (10 lancements pour le bout en bout) dont on donne la moyenne ± l'écart-type d'échantillon. Ce sont les mêmes regroupements que `bench/graphiques.py` et `bench/graphique_une_ligne.py`. Les nombres d'états sont sans l'état puits.

Fichier généré le 31/10/2026 à partir de `resultats/bench_jvm.csv`, `resultats/bench_bout_en_bout.csv`, `resultats/bench_une_ligne.csv` et `resultats/demarrage_jvm.csv`.

## fig_kmp_vs_automate.png

`bench_jvm.csv`, expérience `a`, texte `don-quixote-996.txt` (2 348 389 octets, en mémoire). Motifs triés par longueur m.

| m | motif | lignes trouvées | recherche KMP (ms) | recherche automate une passe (ms) | construction KMP (ms) | construction automate (ms) | lignes CSV KMP | lignes CSV automate |
|---|---|---|---|---|---|---|---|---|
| 2 | `fr` | 2262 | 3,913 ± 0,803 | 7,264 ± 0,285 | 0,012 ± 0,004 | 0,722 ± 0,314 | 2–21 | 162–181 |
| 3 | `III` | 52 | 3,305 ± 0,039 | 7,265 ± 0,067 | 0,012 ± 0,007 | 0,453 ± 0,127 | 22–41 | 182–201 |
| 5 | `t’s n` | 22 | 5,257 ± 0,311 | 7,147 ± 0,361 | 0,022 ± 0,012 | 0,553 ± 0,086 | 42–61 | 202–221 |
| 8 | `ire of m` | 3 | 5,230 ± 0,931 | 9,489 ± 1,622 | 0,013 ± 0,009 | 1,491 ± 1,368 | 62–81 | 222–241 |
| 12 | `urpasses all` | 3 | 3,805 ± 0,024 | 7,955 ± 0,154 | 0,008 ± 0,003 | 0,706 ± 0,075 | 82–101 | 242–261 |
| 20 | `ime when the city ga` | 1 | 4,601 ± 0,127 | 8,450 ± 0,495 | 0,014 ± 0,005 | 1,639 ± 0,800 | 102–121 | 262–281 |
| 40 | `ad and reserve himself for a more favour` | 1 | 5,068 ± 0,065 | 8,007 ± 0,116 | 0,022 ± 0,005 | 4,408 ± 0,782 | 122–141 | 282–301 |
| 70 | `ought in sending the acorns, for if she had not sent them I’d have bee` | 1 | 5,752 ± 0,106 | 8,511 ± 1,266 | 0,032 ± 0,009 | 10,490 ± 1,969 | 142–161 | 302–321 |

## fig_naif_vs_unepasse_textes.png

`bench_jvm.csv`, expérience `b_textes`. Temps de recherche en ms.

| texte | regex | lignes trouvées | matcher naïf | matcher une passe | lignes CSV naïf | lignes CSV une passe |
|---|---|---|---|---|---|---|
| yellow-wallpaper-1952.txt | `the` | 395 | 0,573 ± 0,931 | 0,628 ± 0,797 | 322–341 | 782–801 |
| yellow-wallpaper-1952.txt | `S(a\|g\|r)*on` | 0 | 0,224 ± 0,035 | 0,221 ± 0,009 | 342–361 | 802–821 |
| yellow-wallpaper-1952.txt | `(Mr\|Mrs) (Darcy\|Bennet)` | 0 | 0,188 ± 0,037 | 0,200 ± 0,019 | 362–381 | 822–841 |
| yellow-wallpaper-1952.txt | `h.*e.*r` | 558 | 0,214 ± 0,023 | 0,153 ± 0,009 | 382–401 | 842–861 |
| yellow-wallpaper-1952.txt | `((a\|b)*c)*d` | 696 | 0,189 ± 0,049 | 0,128 ± 0,009 | 402–421 | 862–881 |
| yellow-wallpaper-1952.txt | `.(Bab\|Ass)y` | 0 | 0,467 ± 0,146 | 0,210 ± 0,013 | 422–441 | 882–901 |
| text1.txt | `the` | 5688 | 2,861 ± 1,040 | 2,070 ± 0,097 | 442–461 | 902–921 |
| text1.txt | `S(a\|g\|r)*on` | 0 | 1,462 ± 0,097 | 2,340 ± 0,119 | 462–481 | 922–941 |
| text1.txt | `(Mr\|Mrs) (Darcy\|Bennet)` | 0 | 1,469 ± 0,155 | 2,440 ± 0,134 | 482–501 | 942–961 |
| text1.txt | `h.*e.*r` | 9197 | 1,508 ± 0,022 | 1,716 ± 0,030 | 502–521 | 962–981 |
| text1.txt | `((a\|b)*c)*d` | 9901 | 1,161 ± 0,016 | 1,397 ± 0,054 | 522–541 | 982–1001 |
| text1.txt | `.(Bab\|Ass)y` | 0 | 2,781 ± 0,098 | 2,593 ± 0,046 | 542–561 | 1002–1021 |
| don-quixote-996.txt | `the` | 21226 | 4,758 ± 0,219 | 5,325 ± 1,056 | 562–581 | 1022–1041 |
| don-quixote-996.txt | `S(a\|g\|r)*on` | 1 | 4,510 ± 0,242 | 7,147 ± 0,029 | 582–601 | 1042–1061 |
| don-quixote-996.txt | `(Mr\|Mrs) (Darcy\|Bennet)` | 0 | 4,789 ± 0,458 | 7,209 ± 0,110 | 602–621 | 1062–1081 |
| don-quixote-996.txt | `h.*e.*r` | 28043 | 5,496 ± 0,199 | 4,693 ± 0,027 | 622–641 | 1082–1101 |
| don-quixote-996.txt | `((a\|b)*c)*d` | 31052 | 3,723 ± 0,230 | 3,506 ± 0,021 | 642–661 | 1102–1121 |
| don-quixote-996.txt | `.(Bab\|Ass)y` | 1 | 9,345 ± 2,004 | 7,188 ± 0,176 | 662–681 | 1122–1141 |

## fig_pire_cas.png

`bench_jvm.csv`, expérience `b_pire_cas`, regex `a.*b` sur une ligne de n « a ». Temps de recherche en ms.

| n | matcher naïf | matcher une passe | lignes CSV naïf | lignes CSV une passe |
|---|---|---|---|---|
| 1000 | 2,229 ± 0,017 | 0,0067 ± 0,0034 | 682–701 | 1142–1161 |
| 5000 | 35,212 ± 0,256 | 0,0184 ± 0,0026 | 702–721 | 1162–1181 |
| 10000 | 140,360 ± 0,753 | 0,0348 ± 0,0033 | 722–741 | 1182–1201 |
| 20000 | 560,734 ± 1,610 | 0,0679 ± 0,0102 | 742–761 | 1202–1221 |
| 50000 | 3547,938 ± 39,825 | 0,1632 ± 0,0047 | 762–781 | 1222–1241 |

Pentes en échelle log-log entre n = 10 000 et n = 50 000 : log(t₅₀₀₀₀ / t₁₀₀₀₀) / log 5 = **2,01** pour le matcher naïf, **0,96** pour le matcher une passe.

Transitions du naïf à n = 50 000 : n(n+1)/2 = 1 250 025 000, en 3547,9 ms, soit 2,84 ns par transition.

## fig_passage_echelle.png

`bench_jvm.csv`, expérience `c_echelle`, fichiers `genere-NNMio.txt`. Temps de recherche en ms.

| regex | méthode | fichier | lignes trouvées | temps (ms) | lignes CSV |
|---|---|---|---|---|---|
| `together` | KMP | genere-01Mio.txt | 41 | 2,360 ± 0,096 | 1242–1261 |
| `together` | KMP | genere-02Mio.txt | 96 | 4,833 ± 0,111 | 1262–1281 |
| `together` | KMP | genere-04Mio.txt | 233 | 9,931 ± 0,801 | 1282–1301 |
| `together` | KMP | genere-08Mio.txt | 419 | 18,905 ± 0,072 | 1302–1321 |
| `together` | KMP | genere-16Mio.txt | 856 | 38,443 ± 0,069 | 1322–1341 |
| `together` | KMP | genere-32Mio.txt | 1705 | 79,581 ± 5,340 | 1342–1361 |
| `together` | matcher une passe | genere-01Mio.txt | 41 | 3,594 ± 0,326 | 1362–1381 |
| `S(a\|g\|r)*on` | matcher une passe | genere-01Mio.txt | 30 | 3,695 ± 0,296 | 1382–1401 |
| `together` | matcher une passe | genere-02Mio.txt | 96 | 9,397 ± 1,591 | 1402–1421 |
| `S(a\|g\|r)*on` | matcher une passe | genere-02Mio.txt | 31 | 7,460 ± 0,397 | 1422–1441 |
| `together` | matcher une passe | genere-04Mio.txt | 233 | 13,167 ± 0,195 | 1442–1461 |
| `S(a\|g\|r)*on` | matcher une passe | genere-04Mio.txt | 32 | 13,044 ± 0,106 | 1462–1481 |
| `together` | matcher une passe | genere-08Mio.txt | 419 | 27,570 ± 0,190 | 1482–1501 |
| `S(a\|g\|r)*on` | matcher une passe | genere-08Mio.txt | 85 | 30,111 ± 2,838 | 1502–1521 |
| `together` | matcher une passe | genere-16Mio.txt | 856 | 52,633 ± 0,928 | 1522–1541 |
| `S(a\|g\|r)*on` | matcher une passe | genere-16Mio.txt | 179 | 53,208 ± 1,313 | 1542–1561 |
| `together` | matcher une passe | genere-32Mio.txt | 1705 | 104,748 ± 0,979 | 1562–1581 |
| `S(a\|g\|r)*on` | matcher une passe | genere-32Mio.txt | 349 | 104,871 ± 0,675 | 1582–1601 |
| `together` | matcher naïf | genere-01Mio.txt | 41 | 3,115 ± 0,497 | 1602–1621 |
| `S(a\|g\|r)*on` | matcher naïf | genere-01Mio.txt | 30 | 2,029 ± 0,328 | 1622–1641 |
| `together` | matcher naïf | genere-02Mio.txt | 96 | 6,330 ± 0,533 | 1642–1661 |
| `S(a\|g\|r)*on` | matcher naïf | genere-02Mio.txt | 31 | 4,802 ± 0,445 | 1662–1681 |
| `together` | matcher naïf | genere-04Mio.txt | 233 | 11,642 ± 0,138 | 1682–1701 |
| `S(a\|g\|r)*on` | matcher naïf | genere-04Mio.txt | 32 | 7,913 ± 0,049 | 1702–1721 |
| `together` | matcher naïf | genere-08Mio.txt | 419 | 23,328 ± 0,195 | 1722–1741 |
| `S(a\|g\|r)*on` | matcher naïf | genere-08Mio.txt | 85 | 16,098 ± 0,329 | 1742–1761 |
| `together` | matcher naïf | genere-16Mio.txt | 856 | 46,059 ± 0,301 | 1762–1781 |
| `S(a\|g\|r)*on` | matcher naïf | genere-16Mio.txt | 179 | 32,398 ± 1,511 | 1782–1801 |
| `together` | matcher naïf | genere-32Mio.txt | 1705 | 101,239 ± 11,239 | 1802–1821 |
| `S(a\|g\|r)*on` | matcher naïf | genere-32Mio.txt | 349 | 68,027 ± 4,758 | 1822–1841 |

`S(a|g|r)*on`, matcher naif : de 1 à 32 Mio, × 33,5 (2,03 → 68,03 ms).
`S(a|g|r)*on`, matcher unepasse : de 1 à 32 Mio, × 28,4 (3,69 → 104,87 ms).
`together`, matcher naif : de 1 à 32 Mio, × 32,5 (3,11 → 101,24 ms).
`together`, matcher unepasse : de 1 à 32 Mio, × 29,1 (3,59 → 104,75 ms).

## fig_etats_automates.png

`bench_jvm.csv`, colonnes `etats_afn`, `etats_afd`, `etats_min`, `etats_min_pointetoile`, lues sur la première ligne de chaque regex (elles sont identiques sur toutes ses lignes). États sans puits.

### Regex du banc (expérience `d_regex`)

| regex | AFN-ε (R) | AFD (R) | AFD minimal (R) | AFD minimal (.*R) | ligne CSV |
|---|---|---|---|---|---|
| `a` | 2 | 2 | 2 | 2 | 1982 |
| `the` | 6 | 4 | 4 | 4 | 2002 |
| `Elizabeth` | 18 | 10 | 10 | 10 | 2022 |
| `Babylon` | 14 | 8 | 8 | 8 | 2042 |
| `Passepartout` | 24 | 13 | 13 | 13 | 2062 |
| `anas` | 8 | 5 | 5 | 5 | 2082 |
| `It is a truth universally acknowledged` | 76 | 39 | 39 | 39 | 2102 |
| `husband in the_ Spectator, _who describes, with all the gusto and all the` | 146 | 74 | 74 | 74 | 2122 |
| `Darcy\|Bingley` | 26 | 13 | 11 | 11 | 2142 |
| `cat\|dog\|mouse` | 26 | 12 | 10 | 10 | 2162 |
| `(Mr\|Mrs) (Darcy\|Bennet)` | 38 | 16 | 15 | 15 | 2182 |
| `((a\|b)\|(c\|d))e` | 16 | 6 | 3 | 3 | 2202 |
| `th(e\|(a\|i)n)k` | 18 | 8 | 6 | 6 | 2222 |
| `ab*c` | 8 | 4 | 3 | 3 | 2242 |
| `lo*k` | 8 | 4 | 3 | 3 | 2262 |
| `(a*)*` | 6 | 2 | 1 | 1 | 2282 |
| `(ab*)*c` | 10 | 4 | 3 | 2 | 2302 |
| `((a\|b)*c)*d` | 14 | 5 | 3 | 2 | 2322 |
| `(a*b*)*x` | 12 | 4 | 2 | 2 | 2342 |
| `.` | 2 | 2 | 2 | 2 | 2362 |
| `.*` | 4 | 2 | 1 | 1 | 2382 |
| `a.c` | 6 | 4 | 4 | 6 | 2402 |
| `S.on` | 8 | 5 | 5 | 7 | 2422 |
| `(a\|.)b` | 8 | 4 | 3 | 3 | 2442 |
| `h.*e.*r` | 14 | 7 | 4 | 4 | 2462 |
| `.(Bab\|Ass)y` | 18 | 9 | 8 | 8 | 2482 |
| `(vide)` | 1 | 1 | 1 | 1 | 2502 |
| `a\|` | 5 | 2 | 2 | 1 | 2522 |
| `()` | 1 | 1 | 1 | 1 | 2542 |
| `(a\|)b` | 7 | 3 | 3 | 2 | 2562 |
| `é` | 2 | 2 | 2 | 2 | 2582 |
| `déjà` | 8 | 5 | 5 | 5 | 2602 |
| `Ça` | 4 | 3 | 3 | 3 | 2622 |
| `’` | 2 | 2 | 2 | 2 | 2642 |
| `“.` | 4 | 3 | 3 | 4 | 2662 |
| `n’t` | 6 | 4 | 4 | 4 | 2682 |
| `l’(a\|e)` | 10 | 5 | 4 | 4 | 2702 |
| `zzzq` | 8 | 5 | 5 | 5 | 2722 |
| `Sargonzzz` | 18 | 10 | 10 | 10 | 2742 |
| `S(a\|g\|r)*on` | 18 | 7 | 4 | 4 | 2762 |
| `(Merodach\|Sargon).*wall` | 42 | 20 | 18 | 18 | 2782 |

### Explosion des sous-ensembles (expérience `d_explosion`, regex `(a|b)*a(a|b)ⁿ`)

| n | AFN-ε (R) | AFD (R) | AFD minimal (R) | AFD minimal (.*R) | 2^(n+1) | construction R (ms) | construction .*R (ms) | lignes CSV R | lignes CSV .*R |
|---|---|---|---|---|---|---|---|---|---|
| 1 | 16 | 5 | 4 | 4 | 4 | 0,115 ± 0,011 | 0,174 ± 0,017 | 2802–2821 | 3922–3941 |
| 2 | 22 | 9 | 8 | 8 | 8 | 0,235 ± 0,036 | 0,313 ± 0,032 | 2822–2841 | 3942–3961 |
| 3 | 28 | 17 | 16 | 16 | 16 | 0,475 ± 0,059 | 0,626 ± 0,043 | 2842–2861 | 3962–3981 |
| 4 | 34 | 33 | 32 | 32 | 32 | 1,677 ± 0,228 | 1,276 ± 0,068 | 2862–2881 | 3982–4001 |
| 5 | 40 | 65 | 64 | 64 | 64 | 1,868 ± 0,190 | 2,156 ± 0,311 | 2882–2901 | 4002–4021 |
| 6 | 46 | 129 | 128 | 128 | 128 | 3,778 ± 0,702 | 4,415 ± 0,388 | 2902–2921 | 4022–4041 |
| 7 | 52 | 257 | 256 | 256 | 256 | 9,601 ± 1,710 | 10,051 ± 0,746 | 2922–2941 | 4042–4061 |
| 8 | 58 | 513 | 512 | 512 | 512 | 18,398 ± 2,283 | 22,536 ± 2,923 | 2942–2961 | 4062–4081 |

## fig_construction_thompson.png

`bench_jvm.csv`, expérience `d_thompson`, littéraux aléatoires de k lettres. « Thompson seul » = analyse + AFN-ε (algo `thompson`) ; « construction complète » = jusqu'à la table de transitions de R (algo `automate`). Temps en ms.

| k | AFN-ε | AFD | AFD minimal | Thompson seul | construction complète | lignes CSV Thompson | lignes CSV complète |
|---|---|---|---|---|---|---|---|
| 10 | 20 | 11 | 11 | 0,117 ± 0,069 | 0,265 ± 0,026 | 1842–1861 | 2962–2981 |
| 25 | 50 | 26 | 26 | 0,455 ± 0,087 | 1,061 ± 0,051 | 1862–1881 | 2982–3001 |
| 50 | 100 | 51 | 51 | 1,385 ± 0,421 | 4,387 ± 0,228 | 1882–1901 | 3002–3021 |
| 100 | 200 | 101 | 101 | 2,681 ± 0,593 | 15,229 ± 0,722 | 1902–1921 | 3022–3041 |
| 200 | 400 | 201 | 201 | 11,114 ± 2,035 | 64,861 ± 2,905 | 1922–1941 | 3042–3061 |
| 300 | 600 | 301 | 301 | 26,037 ± 1,850 | 166,709 ± 35,262 | 1942–1961 | 3062–3081 |
| 400 | 800 | 401 | 401 | 50,311 ± 4,066 | 297,816 ± 8,726 | 1962–1981 | 3082–3101 |

Pentes log-log entre k = 100 et k = 400 : **2,11** pour Thompson seul, **2,14** pour la construction complète.

## fig_bout_en_bout.png

`bench_bout_en_bout.csv`, 10 lancements de `java -jar egrep-clone.jar -c` et de `grep -E -c`, démarrage compris. Temps en ms. Rapport = moyenne nous / moyenne grep.

| texte | regex | lignes trouvées | nous (ms) | grep (ms) | rapport | lignes CSV nous | lignes CSV grep |
|---|---|---|---|---|---|---|---|
| yellow-wallpaper-1952.txt | `the` | 395 | 80,1 ± 7,6 | 1,9 ± 0,3 | 42,8 | 2–11 | 12–21 |
| yellow-wallpaper-1952.txt | `Elizabeth` | 0 | 67,5 ± 4,8 | 1,3 ± 0,2 | 50,1 | 22–31 | 32–41 |
| yellow-wallpaper-1952.txt | `S(a\|g\|r)*on` | 0 | 99,7 ± 24,5 | 1,3 ± 0,0 | 75,5 | 42–51 | 52–61 |
| yellow-wallpaper-1952.txt | `(Mr\|Mrs) (Darcy\|Bennet)` | 0 | 97,0 ± 3,9 | 1,3 ± 0,0 | 73,4 | 62–71 | 72–81 |
| yellow-wallpaper-1952.txt | `a.c` | 71 | 84,0 ± 1,2 | 1,3 ± 0,0 | 62,4 | 82–91 | 92–101 |
| yellow-wallpaper-1952.txt | `h.*e.*r` | 558 | 87,2 ± 5,6 | 1,4 ± 0,0 | 60,9 | 102–111 | 112–121 |
| yellow-wallpaper-1952.txt | `((a\|b)*c)*d` | 696 | 89,3 ± 8,7 | 1,3 ± 0,0 | 67,4 | 122–131 | 132–141 |
| yellow-wallpaper-1952.txt | `zzzq` | 0 | 71,1 ± 4,2 | 1,4 ± 0,3 | 49,6 | 142–151 | 152–161 |
| don-quixote-996.txt | `the` | 21226 | 125,4 ± 12,1 | 5,4 ± 0,3 | 23,1 | 162–171 | 172–181 |
| don-quixote-996.txt | `Elizabeth` | 0 | 116,0 ± 8,0 | 3,3 ± 0,1 | 35,2 | 182–191 | 192–201 |
| don-quixote-996.txt | `S(a\|g\|r)*on` | 1 | 151,9 ± 8,8 | 5,7 ± 0,1 | 26,6 | 202–211 | 212–221 |
| don-quixote-996.txt | `(Mr\|Mrs) (Darcy\|Bennet)` | 0 | 161,5 ± 36,2 | 5,0 ± 0,0 | 32,2 | 222–231 | 232–241 |
| don-quixote-996.txt | `a.c` | 5062 | 140,2 ± 6,3 | 6,8 ± 0,0 | 20,7 | 242–251 | 252–261 |
| don-quixote-996.txt | `h.*e.*r` | 28043 | 149,0 ± 14,8 | 8,0 ± 0,8 | 18,7 | 262–271 | 272–281 |
| don-quixote-996.txt | `((a\|b)*c)*d` | 31052 | 134,2 ± 5,5 | 5,6 ± 0,1 | 24,1 | 282–291 | 292–301 |
| don-quixote-996.txt | `zzzq` | 0 | 124,3 ± 14,6 | 1,8 ± 0,1 | 68,6 | 302–311 | 312–321 |
| genere-32Mio.txt | `the` | 268314 | 358,1 ± 20,7 | 57,2 ± 0,5 | 6,3 | 322–331 | 332–341 |
| genere-32Mio.txt | `Elizabeth` | 3876 | 343,3 ± 11,3 | 28,0 ± 0,3 | 12,3 | 342–351 | 352–361 |
| genere-32Mio.txt | `S(a\|g\|r)*on` | 349 | 407,0 ± 18,2 | 62,7 ± 0,5 | 6,5 | 362–371 | 372–381 |
| genere-32Mio.txt | `(Mr\|Mrs) (Darcy\|Bennet)` | 0 | 421,0 ± 26,5 | 57,9 ± 0,6 | 7,3 | 382–391 | 392–401 |
| genere-32Mio.txt | `a.c` | 62639 | 415,7 ± 21,2 | 80,2 ± 1,5 | 5,2 | 402–411 | 412–421 |
| genere-32Mio.txt | `h.*e.*r` | 369320 | 403,9 ± 12,4 | 76,7 ± 7,4 | 5,3 | 422–431 | 432–441 |
| genere-32Mio.txt | `((a\|b)*c)*d` | 440951 | 402,6 ± 15,0 | 62,7 ± 1,5 | 6,4 | 442–451 | 452–461 |
| genere-32Mio.txt | `zzzq` | 0 | 334,1 ± 33,2 | 10,5 ± 1,0 | 32,0 | 462–471 | 472–481 |

Extrêmes : 5,18 (`a.c` sur genere-32Mio.txt) et 75,45 (`S(a|g|r)*on` sur yellow-wallpaper-1952.txt), d'où « 5 à 75 fois ».

### Démarrage (pas de figure ; cité avec fig_bout_en_bout)

`demarrage_jvm.csv`, regex `zzzq` sur `/dev/null`, 10 lancements.

| programme | temps (ms) | lignes CSV |
|---|---|---|
| egrep-clone | 58,5 ± 5,0 | 2–11 |
| grep | 1,4 ± 0,2 | 12–21 |

## fig_une_ligne.png

`bench_une_ligne.csv`, texte *The Yellow Wallpaper* (`data/yellow-wallpaper-1952.txt`) : 1 227 lignes, ou une seule ligne de 50 833 caractères. Temps de recherche en ms.

| regex | forme | matcher naïf | médiane naïf | matcher une passe | médiane une passe | lignes CSV naïf | lignes CSV une passe |
|---|---|---|---|---|---|---|---|
| `e.*zq` | lignes | 1,308 ± 2,896 | 0,646 | 0,629 ± 0,962 | 0,218 | 2–21 | 82–101 |
| `e.*zq` | une_ligne | 362,498 ± 1,794 | 362,262 | 0,161 ± 0,003 | 0,160 | 22–41 | 102–121 |
| `S(a\|g\|r)*on` | lignes | 0,198 ± 0,034 | 0,185 | 0,240 ± 0,061 | 0,218 | 42–61 | 122–141 |
| `S(a\|g\|r)*on` | une_ligne | 0,162 ± 0,009 | 0,159 | 0,161 ± 0,003 | 0,160 | 62–81 | 142–161 |

`e.*zq` en une seule ligne : rapport naïf / une passe = 2249. Aucune ligne de statut « > 10 s » dans le CSV.

Transitions du naïf (`e.*zq`, une ligne) : 116 288 082 (comptage dans `resultats/scenarios.md`), soit 3,12 ns par transition.

## Tableau sans figure : coût du matcher une passe par caractère

Temps moyen divisé par le nombre de caractères lus (lignes sans `\n`). Nombres de caractères : CSV pour `a^n` et la ligne unique ; comptage décrit dans `resultats/scenarios.md` pour Don Quixote et 32 Mio.

| regex | texte | caractères | temps (ms) | ns par caractère | lignes CSV |
|---|---|---|---|---|---|
| `a.*b` | une ligne de 1 000 « a » | 1 000 | 0,0067 ± 0,0034 | 6,7 | bench_jvm.csv, 1142–1161 |
| `a.*b` | une ligne de 10 000 « a » | 10 000 | 0,0348 ± 0,0033 | 3,5 | bench_jvm.csv, 1182–1201 |
| `a.*b` | une ligne de 50 000 « a » | 50 000 | 0,1632 ± 0,0047 | 3,3 | bench_jvm.csv, 1222–1241 |
| `e.*zq` | The Yellow Wallpaper, une ligne | 50 833 | 0,1612 ± 0,0033 | 3,2 | bench_une_ligne.csv, 102–121 |
| `S(a\|g\|r)*on` | The Yellow Wallpaper, une ligne | 50 833 | 0,1613 ± 0,0034 | 3,2 | bench_une_ligne.csv, 142–161 |
| `S(a\|g\|r)*on` | Don Quixote | 2 275 296 | 7,1471 ± 0,0294 | 3,1 | bench_jvm.csv, 1042–1061 |
| `S(a\|g\|r)*on` | genere-32Mio.txt | 32 429 599 | 104,8706 ± 0,6745 | 3,2 | bench_jvm.csv, 1582–1601 |
| `together` | genere-32Mio.txt | 32 429 599 | 104,7479 ± 0,9794 | 3,2 | bench_jvm.csv, 1562–1581 |

## Section Tests : chiffres sans figure, hors CSV

Ces chiffres ne viennent pas d'un CSV. La colonne « source » dit d'où ils viennent : une commande relancée le 31/10/2026, un fichier de `tests/`, ou seulement le CLAUDE.md (non revérifié aujourd'hui).

| chiffre | valeur | source |
|---|---|---|
| tests de `TestKmp` | 20 012 réussis, dont 20 000 aléatoires (graine 20261030) | `ant clean jar test` du 31/10/2026 ; `tests/TestKmp.java`, lignes 26–27 |
| comparaisons du banc | 1 611 OK, 0 KO (369 × 4 configurations + 135 pour KMP), en 100 s | `ant clean jar test` du 31/10/2026 |
| regex du banc | 41 : 8 littéraux, 5 alternatives, 6 étoiles, 7 points, 4 mots vides, 7 accents et typographie, 2 sans résultat, 2 du sujet | `tests/regex.txt` |
| littéraux testés avec `--algo=kmp` | 15 × 9 textes = 135 | `tests/regex.txt` (regex non vides, sans `\| * . ( )`) |
| tailles des textes | 51 344 à 2 348 389 octets, 5 333 057 au total ; aucun `\r` | `wc -c data/*.txt`, `grep -c $'\r'` |
| BOM de `babylon-56667.txt` | octets `ef bb bf` en tête | `head -c3 … \| od` |
| KMP faux, motif `anas` | 1 ligne fausse dans Candide, 1 dans Verne | `resultats/exemples_rapport.md` (TraceKmp) |
| deux erreurs combinées | 515 KO | **CLAUDE.md seulement** (« État du projet ») |
| sans locale UTF-8 | 146 faux écarts | **CLAUDE.md seulement** (« Écarts connus ») |

## Section Performances : protocole

| élément | valeur | source |
|---|---|---|
| processeur | Intel Core i5-8350U, 1,70 GHz, 8 threads (2 par cœur) | `LC_ALL=C lscpu`, 31/10/2026 |
| mémoire | 7 816 Mio | `free -m` |
| système | Ubuntu 24.04.5 LTS, Linux 6.17.0-35 | `lsb_release -d`, `uname -r` |
| Java | OpenJDK 21.0.12.1 (bytecode Java 11) | `java -version` |
| grep | GNU grep 3.11 | `grep -V` |
| protocole | 5 chauffes + 20 mesures en mémoire ; 1 + 10 lancements de bout en bout | `bench/Benchmark.java` (`CHAUFFE`, `MESURES`), `bench/bench_bout_en_bout.py` (`LANCEMENTS`) |

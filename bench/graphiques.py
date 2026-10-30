#!/usr/bin/env python3
"""Graphiques du rapport, à partir des CSV du banc de performances.

Lit resultats/bench_jvm.csv et resultats/bench_bout_en_bout.csv,
et écrit dans rapport/figures/ sept PNG lisibles en noir et blanc
(hachures et marqueurs différents, niveaux de gris). Chaque barre ou
point est une moyenne, avec l'écart-type (échantillon) en barre
d'erreur. Affiche aussi les chiffres clés de chaque figure.

Usage (depuis n'importe où) : python3 bench/graphiques.py
"""

import csv
import statistics
import sys
from collections import defaultdict
from pathlib import Path

try:
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
except ImportError:
    sys.exit("ERREUR : matplotlib absent (Ubuntu : "
             "sudo apt install python3-matplotlib)")

RACINE = Path(__file__).resolve().parent.parent
JVM = RACINE / "resultats" / "bench_jvm.csv"
BOUT = RACINE / "resultats" / "bench_bout_en_bout.csv"
FIGURES = RACINE / "rapport" / "figures"
MIO = 1 << 20

# Noir et blanc : remplissage en niveaux de gris + hachures
GRIS = ["white", "0.75", "0.45", "0.15"]
HACHURES = ["//", "..", "xx", "\\\\"]
MARQUEURS = ["o", "s", "^", "D", "v"]
TRAITS = ["-", "--", "-.", ":", (0, (5, 1))]

plt.rcParams.update({
    "font.size": 10,
    "axes.titlesize": 11,
    "figure.dpi": 150,
    "savefig.bbox": "tight",
    "hatch.linewidth": 0.6,
})


def lire(chemin):
    with open(chemin, encoding="utf-8", newline="") as f:
        return list(csv.DictReader(f))


def moyenne_ecart(valeurs):
    return statistics.mean(valeurs), statistics.stdev(valeurs)


def grouper(lignes, cles, valeur):
    """{tuple(cles): [valeurs float]}, dans l'ordre d'apparition."""
    groupes = defaultdict(list)
    for ligne in lignes:
        groupes[tuple(ligne[c] for c in cles)].append(float(ligne[valeur]))
    return groupes


def taille_lisible(octets):
    if octets < MIO:
        return f"{octets / 1024:.0f} Kio"
    return f"{octets / MIO:.1f} Mio".replace(".", ",")


def abreger(regex, n=22):
    if regex == "":
        return "(vide)"
    return regex if len(regex) <= n else regex[: n - 1] + "…"


def barres(ax, etiquettes, series, titre, ylabel, log=False):
    """series : liste de (nom, [moyennes], [écarts-types])."""
    largeur = 0.8 / len(series)
    for k, (nom, moyennes, ecarts) in enumerate(series):
        x = [i + (k - (len(series) - 1) / 2) * largeur
             for i in range(len(etiquettes))]
        ax.bar(x, moyennes, largeur, yerr=ecarts, capsize=2.5,
               label=nom, color=GRIS[k % 4], hatch=HACHURES[k % 4],
               edgecolor="black", linewidth=0.6,
               error_kw={"elinewidth": 0.8})
    ax.set_xticks(range(len(etiquettes)))
    ax.set_xticklabels(etiquettes, rotation=30, ha="right")
    ax.set_title(titre)
    ax.set_ylabel(ylabel)
    # Place libre en haut pour la légende, au-dessus des barres
    haut = max(m + e for _, ms, es in series for m, e in zip(ms, es))
    if log:
        ax.set_yscale("log")
        bas = min(m for _, ms, _ in series for m in ms)
        ax.set_ylim(bas / 3, haut * 8)
    else:
        ax.set_ylim(0, haut * 1.3)
    ax.grid(axis="y", linestyle=":", linewidth=0.5)
    ax.legend(loc="upper left", ncol=2, fontsize=8)


def courbe(ax, x, groupes, k, nom):
    moyennes = [moyenne_ecart(g)[0] for g in groupes]
    ecarts = [moyenne_ecart(g)[1] for g in groupes]
    ax.errorbar(x, moyennes, yerr=ecarts, label=nom, color="black",
                marker=MARQUEURS[k % 5], linestyle=TRAITS[k % 5],
                markerfacecolor=["white", "0.5", "black"][k % 3],
                capsize=2.5, linewidth=1)
    return moyennes, ecarts


def enregistrer(fig, nom):
    FIGURES.mkdir(parents=True, exist_ok=True)
    fig.savefig(FIGURES / nom)
    plt.close(fig)
    print(f"  écrit : rapport/figures/{nom}")


def fmt(m, e):
    return f"{m:.3f} ± {e:.3f} ms"


# ==================================================
# a. KMP contre automate une passe
# ==================================================

def fig_kmp(jvm):
    lignes = [l for l in jvm if l["experience"] == "a"]
    recherche = grouper(lignes, ["regex", "algo"], "t_recherche_ms")
    construction = grouper(lignes, ["regex", "algo"], "t_construction_ms")
    motifs = sorted({l["regex"] for l in lignes}, key=len)
    etiquettes = [f"m = {len(m)}" for m in motifs]

    fig, (ax1, ax2) = plt.subplots(1, 2, figsize=(11, 4))
    series_r, series_c = [], []
    for algo, nom in [("kmp", "KMP"), ("automate", "automate une passe")]:
        r = [moyenne_ecart(recherche[(m, algo)]) for m in motifs]
        c = [moyenne_ecart(construction[(m, algo)]) for m in motifs]
        series_r.append((nom, [v[0] for v in r], [v[1] for v in r]))
        series_c.append((nom, [v[0] for v in c], [v[1] for v in c]))
        print(f"  a {nom:20} recherche "
              + ", ".join(f"m={len(m)} {fmt(*v)}" for m, v in zip(motifs, r)))
        print(f"  a {nom:20} construction "
              + ", ".join(f"m={len(m)} {fmt(*v)}" for m, v in zip(motifs, c)))
    fichier = lignes[0]["fichier"]
    barres(ax1, etiquettes, series_r,
           f"Recherche ({fichier}, "
           f"{taille_lisible(int(lignes[0]['taille_octets']))} en mémoire)",
           "temps de recherche (ms)")
    barres(ax2, etiquettes, series_c, "Construction (analyse + table)",
           "temps de construction (ms, échelle log)", log=True)
    ax1.set_xlabel("longueur du motif littéral")
    ax2.set_xlabel("longueur du motif littéral")
    fig.suptitle("KMP contre automate une passe (moyenne ± écart-type, "
                 "20 mesures)")
    enregistrer(fig, "fig_kmp_vs_automate.png")


# ==================================================
# b. Naïf contre une passe
# ==================================================

def fig_naif_textes(jvm):
    lignes = [l for l in jvm if l["experience"] == "b_textes"]
    recherche = grouper(lignes, ["regex", "fichier", "matcher"],
                        "t_recherche_ms")
    fichiers = list(dict.fromkeys(l["fichier"] for l in lignes))
    regex = list(dict.fromkeys(l["regex"] for l in lignes))
    tailles = {l["fichier"]: int(l["taille_octets"]) for l in lignes}

    fig, axes = plt.subplots(1, len(fichiers), figsize=(14, 4.2))
    for ax, f in zip(axes, fichiers):
        series = []
        for matcher, nom in [("naif", "naïf (R)"),
                             ("unepasse", "une passe (.*R)")]:
            v = [moyenne_ecart(recherche[(r, f, matcher)]) for r in regex]
            series.append((nom, [x[0] for x in v], [x[1] for x in v]))
            print(f"  b {f:26} {nom:16} "
                  + ", ".join(f"{r} {fmt(*x)}" for r, x in zip(regex, v)))
        barres(ax, [abreger(r) for r in regex], series,
               f"{f} ({taille_lisible(tailles[f])})",
               "temps de recherche (ms)")
    fig.suptitle("Matcher naïf contre une passe sur les textes réels "
                 "(moyenne ± écart-type, 20 mesures)")
    enregistrer(fig, "fig_naif_vs_unepasse_textes.png")


def fig_pire_cas(jvm):
    lignes = [l for l in jvm if l["experience"] == "b_pire_cas"]
    recherche = grouper(lignes, ["taille_octets", "matcher"],
                        "t_recherche_ms")
    tailles = sorted({int(l["taille_octets"]) for l in lignes})

    fig, ax = plt.subplots(figsize=(7, 4.5))
    resultats = {}
    for k, (matcher, nom) in enumerate([("naif", "naïf (R)"),
                                        ("unepasse", "une passe (.*R)")]):
        x = [n for n in tailles if (str(n), matcher) in recherche]
        m, e = courbe(ax, x, [recherche[(str(n), matcher)] for n in x],
                      k, nom)
        resultats[matcher] = (x, m)
        print(f"  b pire cas {nom:16} "
              + ", ".join(f"n={n} {fmt(a, b)}" for n, a, b in zip(x, m, e)))
    # Pentes de référence, ancrées sur le premier point de chaque
    # courbe : traits gris épais sous les mesures
    for matcher, pente, nom in [("naif", 2, "pente 2 (n²)"),
                                ("unepasse", 1, "pente 1 (n)")]:
        x, m = resultats[matcher]
        ax.plot(x, [m[0] * (n / x[0]) ** pente for n in x], color="0.8",
                linestyle="-", linewidth=6, zorder=0,
                label=f"référence {nom}")
    ax.set_xscale("log")
    ax.set_yscale("log")
    ax.set_xlabel("longueur de la ligne (caractères, échelle log)")
    ax.set_ylabel("temps de recherche (ms, échelle log)")
    ax.set_title("Pire cas : une ligne de « a », regex a.*b\n"
                 "(moyenne ± écart-type, 20 mesures)")
    ax.grid(which="both", linestyle=":", linewidth=0.5)
    ax.legend()
    enregistrer(fig, "fig_pire_cas.png")


# ==================================================
# c. Passage à l'échelle
# ==================================================

def fig_echelle(jvm):
    lignes = [l for l in jvm if l["experience"] == "c_echelle"]
    recherche = grouper(lignes, ["regex", "taille_octets", "algo", "matcher"],
                        "t_recherche_ms")
    regex = list(dict.fromkeys(l["regex"] for l in lignes))

    fig, axes = plt.subplots(1, len(regex), figsize=(12, 4.5))
    for ax, r in zip(axes, regex):
        configs = list(dict.fromkeys((l["algo"], l["matcher"])
                                     for l in lignes if l["regex"] == r))
        for k, (algo, matcher) in enumerate(configs):
            cles = sorted({int(l["taille_octets"]) for l in lignes
                           if l["regex"] == r and l["algo"] == algo
                           and l["matcher"] == matcher})
            x = [t / MIO for t in cles]
            nom = {"kmp": "KMP", "unepasse": "automate une passe",
                   "naif": "automate naïf"}[algo if algo == "kmp" else matcher]
            m, e = courbe(ax, x, [recherche[(r, str(t), algo, matcher)]
                                  for t in cles], k, nom)
            print(f"  c « {r} » {nom:18} "
                  + ", ".join(f"{a:.0f} Mo {fmt(b, c)}"
                              for a, b, c in zip(x, m, e)))
        ax.set_xlabel("taille du fichier (Mio)")
        ax.set_ylabel("temps de recherche (ms)")
        ax.set_title(f"« {r} »")
        ax.grid(linestyle=":", linewidth=0.5)
        ax.legend()
    fig.suptitle("Passage à l'échelle, lignes en mémoire "
                 "(moyenne ± écart-type, 20 mesures)")
    enregistrer(fig, "fig_passage_echelle.png")


# ==================================================
# d. Automates
# ==================================================

def etats_par_regex(jvm, experience):
    """{regex: (afn, afd, min, min .*R)}, une fois par regex."""
    etats = {}
    for l in jvm:
        if l["experience"] == experience and l["regex"] not in etats:
            etats[l["regex"]] = tuple(int(l[c]) for c in (
                "etats_afn", "etats_afd", "etats_min",
                "etats_min_pointetoile"))
    return etats


def fig_etats(jvm):
    regex = etats_par_regex(jvm, "d_regex")
    explosion = etats_par_regex(jvm, "d_explosion")
    noms = ["AFN-ε (R)", "AFD (R)", "AFD minimal (R)", "AFD minimal (.*R)"]

    fig, (ax1, ax2) = plt.subplots(
        1, 2, figsize=(13, 10), gridspec_kw={"width_ratios": [1.3, 1]})

    # Regex du banc : barres horizontales groupées
    etiquettes = [abreger(r, 28) for r in regex]
    hauteur = 0.8 / 4
    for k in range(4):
        y = [i + (k - 1.5) * hauteur for i in range(len(regex))]
        ax1.barh(y, [e[k] for e in regex.values()], hauteur, label=noms[k],
                 color=GRIS[k], hatch=HACHURES[k], edgecolor="black",
                 linewidth=0.4)
    ax1.set_yticks(range(len(regex)))
    ax1.set_yticklabels(etiquettes, fontsize=7.5, family="monospace")
    ax1.invert_yaxis()
    ax1.set_xscale("log")
    ax1.set_xlabel("nombre d'états (échelle log)")
    ax1.set_title("Regex de tests/regex.txt")
    ax1.grid(axis="x", which="both", linestyle=":", linewidth=0.5)
    ax1.legend(loc="lower right", fontsize=8)

    # Explosion des sous-ensembles
    n = list(range(1, len(explosion) + 1))
    for k in range(4):
        ax2.plot(n, [e[k] for e in explosion.values()], color="black",
                 marker=MARQUEURS[k], linestyle=TRAITS[k], label=noms[k],
                 markerfacecolor=["white", "0.5", "black", "white"][k])
    ax2.plot(n, [2 ** (i + 1) for i in n], color="0.5", linestyle=":",
             linewidth=1.2, label="référence 2^(n+1)")
    ax2.set_yscale("log", base=2)
    ax2.set_xlabel("n = nombre de (a|b) après le « a »")
    ax2.set_ylabel("nombre d'états (échelle log₂)")
    ax2.set_title("Explosion des sous-ensembles : (a|b)*a(a|b)ⁿ")
    ax2.grid(which="both", linestyle=":", linewidth=0.5)
    ax2.legend(fontsize=8)
    for i, e in zip(n, explosion.values()):
        print(f"  d explosion n={i} : AFN {e[0]}, AFD {e[1]}, min {e[2]}, "
              f"min .*R {e[3]}")
    plus_grand = max(regex.items(), key=lambda kv: kv[1][1])
    print(f"  d regex : plus gros AFD « {plus_grand[0]} » {plus_grand[1]}")
    fig.suptitle("Nombre d'états des automates")
    enregistrer(fig, "fig_etats_automates.png")


def fig_thompson(jvm):
    lignes = [l for l in jvm if l["experience"] == "d_thompson"]
    construction = grouper(lignes, ["regex", "algo"], "t_construction_ms")
    motifs = sorted({l["regex"] for l in lignes}, key=len)
    k = [len(m) for m in motifs]

    fig, ax = plt.subplots(figsize=(7, 4.5))
    series = {}
    for i, (algo, nom) in enumerate([
            ("thompson", "Thompson seul (analyse + AFN-ε)"),
            ("automate", "construction complète de R (jusqu'à la table)")]):
        m, e = courbe(ax, k, [construction[(mo, algo)] for mo in motifs],
                      i, nom)
        series[algo] = m
        print(f"  d thompson {algo:9} "
              + ", ".join(f"k={a} {fmt(b, c)}" for a, b, c in zip(k, m, e)))
    # Pentes de référence ancrées sur le dernier point de Thompson
    for pente, trait in [(1, ":"), (2, "--"), (3, "-.")]:
        ref = series["thompson"][-1]
        ax.plot(k, [ref * (x / k[-1]) ** pente for x in k], color="0.6",
                linestyle=trait, linewidth=0.9, label=f"référence k^{pente}")
    ax.set_xscale("log")
    ax.set_yscale("log")
    ax.set_xlabel("longueur k du motif littéral (lettres, échelle log)")
    ax.set_ylabel("temps de construction (ms, échelle log)")
    ax.set_title("Coût de construction selon la longueur du motif\n"
                 "(moyenne ± écart-type, 20 mesures)")
    ax.grid(which="both", linestyle=":", linewidth=0.5)
    ax.legend(fontsize=8)
    enregistrer(fig, "fig_construction_thompson.png")


# ==================================================
# Bout en bout
# ==================================================

def fig_bout_en_bout(bout):
    temps = grouper(bout, ["regex", "fichier", "programme"], "t_ms")
    fichiers = list(dict.fromkeys(l["fichier"] for l in bout))
    regex = list(dict.fromkeys(l["regex"] for l in bout))
    tailles = {l["fichier"]: int(l["taille_octets"]) for l in bout}

    fig, axes = plt.subplots(1, len(fichiers), figsize=(15, 4.5))
    for ax, f in zip(axes, fichiers):
        series = []
        for prog in ["egrep-clone", "grep"]:
            v = [moyenne_ecart(temps[(r, f, prog)]) for r in regex]
            nom = "java -jar egrep-clone.jar -c" if prog != "grep" \
                else "grep -E -c"
            series.append((nom, [x[0] for x in v], [x[1] for x in v]))
            print(f"  bout {f:26} {prog:12} "
                  + ", ".join(f"{r} {x[0]:.1f} ± {x[1]:.1f} ms"
                              for r, x in zip(regex, v)))
        barres(ax, [abreger(r, 18) for r in regex], series,
               f"{f} ({taille_lisible(tailles[f])})",
               "temps total (ms, échelle log)", log=True)
    fig.suptitle("Programme complet contre GNU grep, de bout en bout "
                 "(moyenne ± écart-type, 10 lancements)")
    enregistrer(fig, "fig_bout_en_bout.png")


def main():
    for chemin in (JVM, BOUT):
        if not chemin.exists():
            sys.exit(f"ERREUR : {chemin} absent (lancez « ant bench »)")
    jvm = lire(JVM)
    print("Figures :")
    fig_kmp(jvm)
    fig_naif_textes(jvm)
    fig_pire_cas(jvm)
    fig_echelle(jvm)
    fig_etats(jvm)
    fig_thompson(jvm)
    fig_bout_en_bout(lire(BOUT))


if __name__ == "__main__":
    main()

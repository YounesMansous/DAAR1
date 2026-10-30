#!/usr/bin/env python3
"""Figure « livre sur une seule ligne », à partir de
resultats/bench_une_ligne.csv (produit par BenchUneLigne.java).

Écrit rapport/figures/fig_une_ligne.png, dans le même style noir et
blanc que graphiques.py (dont il reprend les fonctions), et affiche
les moyennes. Échelle logarithmique : les temps vont de 0,16 ms à
plusieurs centaines de ms.

Usage (depuis n'importe où) : python3 bench/graphique_une_ligne.py
"""

import sys

# Pas de bench/__pycache__/ à cause de l'import de graphiques
sys.dont_write_bytecode = True

import graphiques as g  # noqa: E402

CSV = g.RACINE / "resultats" / "bench_une_ligne.csv"


def main():
    if not CSV.exists():
        sys.exit(f"ERREUR : {CSV} absent (lancez BenchUneLigne)")
    lignes = [l for l in g.lire(CSV) if l["statut"] == "ok"]
    recherche = g.grouper(lignes, ["regex", "forme", "matcher"],
                          "t_recherche_ms")
    regex = list(dict.fromkeys(l["regex"] for l in lignes))
    formes = [("lignes", "1 227 lignes"), ("une_ligne", "une seule ligne")]
    cas = [(r, f) for r in regex for f, _ in formes]
    noms = dict(formes)

    series = []
    for matcher, nom in [("naif", "naïf (R)"),
                         ("unepasse", "une passe (.*R)")]:
        v = [g.moyenne_ecart(recherche[(r, f, matcher)]) for r, f in cas]
        series.append((nom, [x[0] for x in v], [x[1] for x in v]))
        for (r, f), x in zip(cas, v):
            print(f"  {r:12} {f:10} {nom:16} {g.fmt(*x)}")

    fig, ax = g.plt.subplots(figsize=(6.5, 4))
    g.barres(ax, [f"{r}\n{noms[f]}" for r, f in cas], series,
             "The Yellow Wallpaper (50 833 caractères) : lignes ou "
             "une seule ligne",
             "temps de recherche (ms, échelle log)", log=True)
    ax.set_xticklabels([f"{r}\n{noms[f]}" for r, f in cas],
                       rotation=0, ha="center", fontsize=8)
    g.enregistrer(fig, "fig_une_ligne.png")


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""Banc de bout en bout : notre programme complet contre grep.

Pour 8 regex et 3 fichiers de tailles différentes, lance 10 fois
« java -jar egrep-clone.jar -c R F » et « grep -E -c R F », chronométrés
avec time.perf_counter (démarrage de la JVM, lecture et décodage du
fichier compris). Un lancement non chronométré de chaque commande
précède les mesures, pour que le fichier soit dans le cache du système.
Vérifie que les deux programmes comptent le même nombre de lignes.

Écrit resultats/bench_bout_en_bout.csv.

Usage (depuis n'importe où) : python3 bench/bench_bout_en_bout.py
"""

import csv
import os
import shutil
import statistics
import subprocess
import sys
import time
from pathlib import Path

RACINE = Path(__file__).resolve().parent.parent
JAR = RACINE / "egrep-clone.jar"
DATA = RACINE / "data"
GENERE = RACINE / "bench" / "genere"
SORTIE = RACINE / "resultats" / "bench_bout_en_bout.csv"

LANCEMENTS = 10
MIO = 1 << 20

REGEX = [
    "the",
    "Elizabeth",
    "S(a|g|r)*on",
    "(Mr|Mrs) (Darcy|Bennet)",
    "a.c",
    "h.*e.*r",
    "((a|b)*c)*d",
    "zzzq",
]


def choisir_locale():
    """Même choix que tests/verifier.sh : C.UTF-8, sinon en_US.UTF-8.

    Sans locale UTF-8, la JVM décode les arguments en ASCII et un motif
    non ASCII arrive abîmé dans le programme.
    """
    locales = subprocess.run(["locale", "-a"], capture_output=True,
                             text=True).stdout.split()
    normalisees = {l.lower().replace("-", "") for l in locales}
    for locale in ("C.UTF-8", "en_US.UTF-8"):
        if locale.lower().replace("-", "") in normalisees:
            return locale
    sys.exit("ERREUR : ni C.UTF-8 ni en_US.UTF-8 dans « locale -a »")


def generer(mio):
    """Fichier de mio Mio, construit comme Benchmark.generer : lignes
    des textes de data/ par ordre de nom, répétées sans couper de ligne."""
    fichier = GENERE / f"genere-{mio:02d}Mio.txt"
    if fichier.exists():
        return fichier
    GENERE.mkdir(parents=True, exist_ok=True)
    textes = []
    for chemin in sorted(DATA.glob("*.txt")):
        lignes = chemin.read_text(encoding="utf-8").split("\n")
        if lignes and lignes[-1] == "":
            lignes.pop()
        textes.append(lignes)
    cible, total = mio * MIO, 0
    with open(fichier, "w", encoding="utf-8", newline="\n") as f:
        while True:
            for lignes in textes:
                for ligne in lignes:
                    octets = len(ligne.encode("utf-8")) + 1
                    if total + octets > cible:
                        return fichier
                    f.write(ligne + "\n")
                    total += octets


def lancer(commande, env):
    """Lance la commande ; renvoie (durée en ms, nombre de lignes)."""
    debut = time.perf_counter()
    resultat = subprocess.run(commande, capture_output=True, env=env)
    duree = (time.perf_counter() - debut) * 1000
    if resultat.returncode not in (0, 1):
        sys.exit(f"ERREUR : {commande} a renvoyé {resultat.returncode} :\n"
                 + resultat.stderr.decode("utf-8", "replace"))
    return duree, int(resultat.stdout)


def main():
    locale = choisir_locale()
    env = dict(os.environ, LC_ALL=locale)
    print(f"locale utilisée pour java et grep : {locale}")

    grep = shutil.which("grep", path="/usr/bin:/bin")
    if grep is None or not JAR.exists():
        sys.exit("ERREUR : grep introuvable, ou egrep-clone.jar absent "
                 "(lancez « ant jar »)")

    fichiers = [DATA / "yellow-wallpaper-1952.txt",
                DATA / "don-quixote-996.txt",
                generer(32)]

    programmes = {
        "egrep-clone": lambda r, f: ["java", "-jar", str(JAR), "-c",
                                     "--", r, str(f)],
        "grep": lambda r, f: [grep, "-E", "-c", "--", r, str(f)],
    }

    SORTIE.parent.mkdir(parents=True, exist_ok=True)
    debut = time.perf_counter()

    with open(SORTIE, "w", encoding="utf-8", newline="") as sortie:
        ecrivain = csv.writer(sortie)
        ecrivain.writerow(["regex", "fichier", "taille_octets", "programme",
                           "iteration", "t_ms", "nb_lignes"])

        for fichier in fichiers:
            taille = fichier.stat().st_size
            for regex in REGEX:
                comptes = {}
                for nom, commande in programmes.items():
                    cmd = commande(regex, fichier)
                    lancer(cmd, env)
                    temps = []
                    for i in range(1, LANCEMENTS + 1):
                        duree, n = lancer(cmd, env)
                        temps.append(duree)
                        comptes[nom] = n
                        ecrivain.writerow([regex, fichier.name, taille, nom,
                                           i, f"{duree:.3f}", n])
                    print(f"  {fichier.name:26} {regex:26} {nom:12} "
                          f"{statistics.mean(temps):9.1f} ± "
                          f"{statistics.stdev(temps):6.1f} ms  {n} lignes")
                if len(set(comptes.values())) > 1:
                    sys.exit(f"ERREUR : nombres de lignes différents pour "
                             f"« {regex} », {fichier.name} : {comptes}")

    print(f"Terminé en {time.perf_counter() - debut:.0f} s : {SORTIE}")


if __name__ == "__main__":
    main()

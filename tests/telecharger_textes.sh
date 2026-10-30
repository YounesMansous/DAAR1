#!/bin/bash
# Télécharge dans data/ les textes du banc de vérification
# (projet Gutenberg), supprime les \r et vérifie l'UTF-8.
#
# Relançable : les fichiers déjà présents ne sont pas retéléchargés.
# data/text1.txt et data/text2.txt sont conservés tels quels.
#
# Usage : tests/telecharger_textes.sh

set -eu

cd "$(dirname "$0")/.."

DATA=data
LIMITE_OCTETS=$((6 * 1024 * 1024))   # data/ < 6 Mo (archive < 10 Mo)

# nom de fichier | URL
LIVRES=(
    # Exemple du sujet (S(a|g|r)*on doit y trouver des lignes)
    "babylon-56667.txt|https://www.gutenberg.org/files/56667/56667-0.txt"
    "yellow-wallpaper-1952.txt|https://www.gutenberg.org/cache/epub/1952/pg1952.txt"
    "metamorphosis-5200.txt|https://www.gutenberg.org/cache/epub/5200/pg5200.txt"
    "alice-11.txt|https://www.gutenberg.org/cache/epub/11/pg11.txt"
    # Textes en français, pour les caractères accentués
    "candide-4650.txt|https://www.gutenberg.org/cache/epub/4650/pg4650.txt"
    "tour-du-monde-800.txt|https://www.gutenberg.org/cache/epub/800/pg800.txt"
    "don-quixote-996.txt|https://www.gutenberg.org/cache/epub/996/pg996.txt"
)

mkdir -p "$DATA"

for entree in "${LIVRES[@]}"; do

    nom=${entree%%|*}
    url=${entree#*|}
    cible="$DATA/$nom"

    if [ -s "$cible" ]; then
        echo "déjà présent : $cible"
        continue
    fi

    echo "téléchargement : $cible"

    brut=$(mktemp)
    trap 'rm -f "$brut"' EXIT

    curl -fsSL --retry 3 --max-time 120 -o "$brut" "$url"

    # Gutenberg est souvent en CRLF : grep compterait le \r comme
    # un caractère de la ligne, alors que Java le retire
    tr -d '\r' < "$brut" > "$cible.part"
    rm -f "$brut"

    if ! iconv -f UTF-8 -t UTF-8 "$cible.part" > /dev/null 2>&1; then
        echo "ERREUR : $url n'est pas en UTF-8" >&2
        rm -f "$cible.part"
        exit 1
    fi

    mv "$cible.part" "$cible"
done

# Vérifications sur tous les textes de data/
erreur=0

for f in "$DATA"/*.txt; do

    if ! iconv -f UTF-8 -t UTF-8 "$f" > /dev/null 2>&1; then
        echo "ERREUR : $f n'est pas en UTF-8" >&2
        erreur=1
    fi

    if LC_ALL=C command grep -q "$(printf '\r')" "$f"; then
        echo "ERREUR : $f contient des \\r" >&2
        erreur=1
    fi
done

total=$(cat "$DATA"/*.txt | wc -c)

echo
printf '%10s  %s\n' "octets" "fichier"
for f in "$DATA"/*.txt; do
    printf '%10s  %s\n' "$(wc -c < "$f" | tr -d ' ')" "$f"
done
printf '%10s  %s\n' "$total" "total"

if [ "$total" -ge "$LIMITE_OCTETS" ]; then
    echo "ERREUR : data/ dépasse 6 Mo" >&2
    erreur=1
fi

exit $erreur

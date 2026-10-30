#!/bin/bash
# Banc de vérification : compare egrep-clone.jar à la commande de
# référence (grep -E dans une locale UTF-8) pour chaque regex de
# tests/regex.txt et chaque texte de data/.
#
# Usage : tests/verifier.sh [-v]
#   -v : affiche aussi les comparaisons réussies
#
# Configurations (notre commande / commande de référence) :
#   défaut    java -jar egrep-clone.jar R F                   / grep -E R F
#   naif      ... --algo=automate --matcher=naif R F         / grep -E R F
#   unepasse  ... --algo=automate --matcher=unepasse R F     / grep -E R F
#   kmp       ... --algo=kmp R F (motifs littéraux seuls)    / grep -E R F
#   -n        ... -n R F (options par défaut)                / grep -n -E R F
#
# Une comparaison est OK si la sortie standard est identique octet
# par octet et si le code de sortie (0 ou 1) est le même que grep.
# Seuls les échecs sont affichés au fil de l'eau, puis un bilan.
#
# Code de sortie : 0 si tout est OK, 1 s'il y a au moins un KO,
# 2 en cas d'erreur (jar impossible à construire, option inconnue).

set -u

cd "$(dirname "$0")/.."

VERBEUX=0

case "${1:-}" in
    -v|--verbeux) VERBEUX=1 ;;
    "") ;;
    *) echo "usage : $0 [-v]" >&2; exit 2 ;;
esac

JAR=egrep-clone.jar
REGEX=tests/regex.txt

# Locale UTF-8 de la commande de référence : C.UTF-8 sous Linux,
# en_US.UTF-8 sous macOS
if locale -a 2>/dev/null | command grep -qiE '^c\.utf-?8$'; then
    LOCALE=C.UTF-8
else
    LOCALE=en_US.UTF-8
fi

if [ ! -f "$JAR" ]; then
    echo "construction de $JAR..." >&2
    if ! ant -q jar >&2; then
        echo "ERREUR : impossible de construire $JAR" >&2
        exit 2
    fi
fi

TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT
mkdir "$TMP/regex" "$TMP/ref" "$TMP/sortie"

# Une regex par fichier, pour la passer telle quelle aux processus
# parallèles (# = commentaire, ligne vide = regex vide)
nombre=0

while IFS= read -r ligne || [ -n "$ligne" ]; do

    case "$ligne" in
        \#*) continue ;;
    esac

    printf '%s' "$ligne" > "$TMP/regex/$nombre"
    nombre=$((nombre + 1))
done < "$REGEX"

FICHIERS=(data/*.txt)
PARALLELE=$(getconf _NPROCESSORS_ONLN 2>/dev/null || echo 4)

# Motif littéral (KMP) : non vide, sans | * . ( )
est_litteral() {
    [ -n "$1" ] || return 1

    case "$1" in
        *[\|\*.\(\)]*) return 1 ;;
    esac

    return 0
}

# Sortie de référence de grep pour (regex, fichier, mode),
# calculée une seule fois. $3 = E (grep -E) ou nE (grep -n -E)
reference() {
    local regex sortie
    regex=$(cat "$TMP/regex/$1")
    sortie="$TMP/ref/$1.$(basename "$2").$3"

    if [ "$3" = nE ]; then
        LC_ALL=$LOCALE command grep -n -E -- "$regex" "$2" > "$sortie" 2>&1
    else
        LC_ALL=$LOCALE command grep -E -- "$regex" "$2" > "$sortie" 2>&1
    fi

    echo $? > "$sortie.code"
}

# Compare notre sortie à la référence.
# $1 = configuration, $2 = indice de la regex, $3 = fichier
comparer() {
    local config=$1 indice=$2 fichier=$3
    local regex mode ref sortie code attendu
    local options=()

    regex=$(cat "$TMP/regex/$indice")
    mode=E

    case "$config" in
        defaut)   ;;
        naif)     options=(--algo=automate --matcher=naif) ;;
        unepasse) options=(--algo=automate --matcher=unepasse) ;;
        kmp)      options=(--algo=kmp) ;;
        n)        options=(-n); mode=nE ;;
    esac

    ref="$TMP/ref/$indice.$(basename "$fichier").$mode"
    sortie="$TMP/sortie/$config.$indice.$(basename "$fichier")"

    java -jar "$JAR" ${options[@]+"${options[@]}"} -- "$regex" "$fichier" \
        > "$sortie" 2> "$sortie.err"
    code=$?
    attendu=$(cat "$ref.code")

    if [ "$code" = "$attendu" ] && cmp -s "$sortie" "$ref"; then
        printf 'OK\t%s\t%s\t%s\n' "$config" "$fichier" "$regex"
    else
        printf 'KO\t%s\t%s\t%s\tcode %s, grep %s, %s lignes, grep %s lignes\n' \
            "$config" "$fichier" "$regex" "$code" "$attendu" \
            "$(wc -l < "$sortie" | tr -d ' ')" "$(wc -l < "$ref" | tr -d ' ')"
    fi

    rm -f "$sortie" "$sortie.err"
}

export TMP JAR LOCALE
export -f reference comparer

debut=$SECONDS

# Phase 1 : sorties de référence (grep -E et grep -n -E)
for ((i = 0; i < nombre; i++)); do
    for f in "${FICHIERS[@]}"; do
        echo "$i $f E"
        echo "$i $f nE"
    done
done | xargs -P "$PARALLELE" -n 3 bash -c 'reference "$@"' _

# Phase 2 : nos sorties, comparées au fil de l'eau
for ((i = 0; i < nombre; i++)); do

    regex=$(cat "$TMP/regex/$i")

    for f in "${FICHIERS[@]}"; do

        for config in defaut naif unepasse n; do
            echo "$config $i $f"
        done

        if est_litteral "$regex"; then
            echo "kmp $i $f"
        fi
    done
done | xargs -P "$PARALLELE" -n 3 bash -c 'comparer "$@"' _ \
     | while IFS= read -r ligne; do

    echo "$ligne" >> "$TMP/resultats"

    IFS=$'\t' read -r etat config fichier regex detail <<< "$ligne"

    if [ "$etat" = KO ] || [ "$VERBEUX" = 1 ]; then
        printf '%s  %-9s %-32s «%s»  %s\n' \
            "$etat" "$config" "$fichier" "$regex" "${detail:-}"
    fi
done

duree=$((SECONDS - debut))

# Bilan par configuration
echo
echo "Bilan : $nombre regex × ${#FICHIERS[@]} fichiers, en ${duree} s"

awk -F '\t' '
    { total[$2]++; if ($1 == "OK") ok[$2]++; else ko[$2]++ }
    END {
        split("defaut naif unepasse kmp n", ordre, " ")
        libelle["defaut"]   = "defaut"
        libelle["naif"]     = "--algo=automate --matcher=naif"
        libelle["unepasse"] = "--algo=automate --matcher=unepasse"
        libelle["kmp"]      = "--algo=kmp (litteraux)"
        libelle["n"]        = "-n / grep -n -E"
        printf "  %-38s %6s %6s\n", "configuration", "OK", "KO"
        for (k = 1; k <= 5; k++) {
            c = ordre[k]
            printf "  %-38s %6d %6d\n", libelle[c], ok[c], ko[c]
            tok += ok[c]; tko += ko[c]
        }
        printf "  %-38s %6d %6d\n", "total", tok, tko
    }' "$TMP/resultats"

if command grep -q '^KO' "$TMP/resultats"; then
    exit 1
fi

exit 0

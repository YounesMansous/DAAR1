import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import kmp.Kmp;

/**
 * Trace de KMP pour les exemples du rapport. Affiche la fonction
 * d'échec π calculée par {@link Kmp}, la table CarryOver sous la forme
 * du cours (m + 1 cases : CarryOver[0] = -1 ; pour 1 ≤ i ≤ m,
 * f = π[i-1], et CarryOver[i] = CarryOver[f] si i &lt; m et
 * P[f] = P[i], sinon f), et, pour chaque fichier donné, les retours
 * q = π[q-1] faits par la recherche.
 *
 * <p>Un retour est dit utile quand il mène à un q' &gt; 0 et que le
 * caractère courant prolonge ce bord (c = P[q']) : KMP continue alors
 * sans repartir de zéro, et un KMP faux, qui remettrait q à 0, se
 * tromperait. La boucle de recherche reproduit celle de
 * {@link Kmp#contientMotif(String)}, et chaque ligne est contrôlée
 * contre elle.</p>
 *
 * <p>Lancement (depuis la racine) :
 * {@code java -cp bin:bin-bench TraceKmp <motif> [fichier...]}.</p>
 */
public class TraceKmp {

    // Un pas de la recherche : position, caractère, q avant et après,
    // et les retours q -> π[q-1] faits avant de comparer
    private static final class Pas {
        final int position;
        final char c;
        final int avant;
        final int apres;
        final List<Integer> retours;
        final boolean utile;

        Pas(int position, char c, int avant, int apres,
                List<Integer> retours, boolean utile) {
            this.position = position;
            this.c = c;
            this.avant = avant;
            this.apres = apres;
            this.retours = retours;
            this.utile = utile;
        }
    }

    private TraceKmp() {
    }

    /**
     * Affiche les tables du motif, puis la trace sur chaque fichier.
     *
     * @param args le motif, puis zéro ou plusieurs fichiers
     * @throws IOException si un fichier ne peut pas être lu
     */
    public static void main(String[] args) throws IOException {

        if (args.length < 1) {
            System.err.println("usage : TraceKmp <motif> [fichier...]");
            System.exit(2);
        }

        String motif = args[0];
        Kmp kmp = new Kmp(motif);
        int[] pi = kmp.getEchec();
        char[] p = motif.toCharArray();
        int m = p.length;

        // Forme du cours, de gauche à droite : carry[f] est déjà
        // calculé quand on traite i, puisque f < i
        int[] carry = new int[m + 1];
        carry[0] = -1;
        for (int i = 1; i <= m; i++) {
            int f = pi[i - 1];
            carry[i] = i < m && p[f] == p[i] ? carry[f] : f;
        }

        System.out.println("Motif : " + motif + " (m = " + m + ")");
        System.out.println();
        System.out.println("| i | " + colonnes(m + 1, i -> String.valueOf(i)));
        System.out.println("|---|" + "---|".repeat(m + 1));
        System.out.println("| P[i] | " + colonnes(m + 1,
            i -> i < m ? String.valueOf(p[i]) : "—"));
        System.out.println("| π[i] | " + colonnes(m + 1,
            i -> i < m ? String.valueOf(pi[i]) : "—"));
        System.out.println("| CarryOver[i] | "
            + colonnes(m + 1, i -> String.valueOf(carry[i])));
        System.out.println();

        for (int a = 1; a < args.length; a++) {
            parcourir(Path.of(args[a]), kmp, p, pi);
        }
    }

    private interface Cellule {
        String valeur(int i);
    }

    private static String colonnes(int m, Cellule cellule) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < m; i++) {
            sb.append(cellule.valeur(i)).append(" | ");
        }
        return sb.toString().trim();
    }

    // Recherche sur toutes les lignes d'un fichier, avec le décompte
    // des retours et la trace de la première ligne où un retour sert
    private static void parcourir(Path fichier, Kmp kmp, char[] p, int[] pi)
            throws IOException {

        List<String> lignes =
            Files.readAllLines(fichier, StandardCharsets.UTF_8);
        int m = p.length;

        long[] retoursParQ = new long[m + 1];
        long utiles = 0;
        long trouvees = 0;
        long faussesSiRemiseAZero = 0;
        int premiereUtile = -1;
        List<Pas> tracePremiere = null;

        for (int n = 0; n < lignes.size(); n++) {

            String ligne = lignes.get(n);
            List<Pas> pas = new ArrayList<>();
            boolean trouve = false;
            boolean utileDansLigne = false;
            int q = 0;

            for (int i = 0; i < ligne.length() && !trouve; i++) {

                char c = ligne.charAt(i);
                int avant = q;
                List<Integer> retours = new ArrayList<>();

                while (q > 0 && c != p[q]) {
                    retoursParQ[q]++;
                    q = pi[q - 1];
                    retours.add(q);
                }

                boolean utile = !retours.isEmpty() && q > 0 && c == p[q];

                if (c == p[q]) {
                    q++;
                }

                if (utile) {
                    utiles++;
                    utileDansLigne = true;
                }

                pas.add(new Pas(i, c, avant, q, retours, utile));

                if (q == m) {
                    trouve = true;
                }
            }

            if (trouve != kmp.contientMotif(ligne)) {
                throw new IllegalStateException(
                    "trace différente de Kmp, ligne " + (n + 1));
            }

            if (trouve) {
                trouvees++;
            }

            if (trouve != faux(p, ligne)) {
                faussesSiRemiseAZero++;
            }

            if (utileDansLigne && premiereUtile < 0) {
                premiereUtile = n + 1;
                tracePremiere = pas;
            }
        }

        System.out.println("== " + fichier + " : " + lignes.size()
            + " lignes, " + trouvees + " trouvées");
        StringBuilder detail = new StringBuilder();
        long total = 0;
        for (int q = 1; q <= m; q++) {
            if (retoursParQ[q] > 0) {
                detail.append(String.format(Locale.ROOT, " q=%d→%d : %d ;",
                    q, pi[q - 1], retoursParQ[q]));
                total += retoursParQ[q];
            }
        }
        System.out.println("Retours q = π[q-1] : " + total
            + (total > 0 ? " (" + detail.toString().trim() + ")" : ""));
        System.out.println("Retours utiles (q' > 0 et c = P[q']) : " + utiles);
        System.out.println("Lignes où un KMP qui remet q à 0 se trompe : "
            + faussesSiRemiseAZero);

        if (premiereUtile > 0) {
            System.out.println();
            System.out.println("Première ligne avec un retour utile : "
                + premiereUtile);
            System.out.println("« " + lignes.get(premiereUtile - 1) + " »");
            System.out.println();
            imprimerTrace(tracePremiere, p);
        }

        System.out.println();
    }

    // Trace autour du premier retour utile : depuis le dernier q = 0
    // qui le précède jusqu'à la fin de la recherche dans la ligne
    private static void imprimerTrace(List<Pas> pas, char[] p) {

        int utile = 0;
        while (!pas.get(utile).utile) {
            utile++;
        }

        int debut = utile;
        while (debut > 0 && pas.get(debut).avant > 0) {
            debut--;
        }
        debut = Math.max(0, debut - 1);

        int fin = Math.min(pas.size() - 1, utile + p.length);

        System.out.println("| position | caractère | q avant | retour(s) "
            + "par π | q après |");
        System.out.println("|---|---|---|---|---|");

        for (int k = debut; k <= fin; k++) {
            Pas s = pas.get(k);
            StringBuilder r = new StringBuilder();
            int q = s.avant;
            for (int cible : s.retours) {
                r.append(r.length() == 0 ? "" : ", ")
                    .append("q = π[").append(q - 1).append("] = ")
                    .append(cible);
                q = cible;
            }
            System.out.printf(Locale.ROOT, "| %d | %s | %d | %s | %d%s |%n",
                s.position, s.c == ' ' ? "(espace)" : String.valueOf(s.c),
                s.avant, r.length() == 0 ? "" : r.toString(),
                s.apres, s.apres == p.length ? " (trouvé)" : "");
        }
    }

    // KMP volontairement faux : q remis à 0 au lieu de π[q-1]
    private static boolean faux(char[] p, String ligne) {
        int q = 0;
        for (int i = 0; i < ligne.length(); i++) {
            char c = ligne.charAt(i);
            if (q > 0 && c != p[q]) {
                q = 0;
            }
            if (c == p[q]) {
                q++;
            }
            if (q == p.length) {
                return true;
            }
        }
        return false;
    }
}

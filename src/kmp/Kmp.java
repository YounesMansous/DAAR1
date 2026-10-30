package kmp;

import ast.AstChar;
import ast.AstConcat;
import ast.interfaces.AstI;

import experimentation.Matcher;

/**
 * Recherche d'un motif littéral P (concaténation de caractères)
 * par l'algorithme de Knuth-Morris-Pratt.
 *
 * Table d'échec : {@code echec[i]} = π[i] = longueur du plus long bord
 * propre de P[0..i], c'est-à-dire du plus long préfixe de P[0..i],
 * différent de P[0..i] lui-même, qui en est aussi un suffixe.
 * Exemple : P = « ababc » donne π = [0, 0, 1, 2, 0].
 *
 * On ne recule jamais dans le texte : quand le caractère lu ne
 * prolonge pas les q caractères déjà reconnus, ces q caractères
 * sont P[0..q-1]. On sait donc sans relire le texte que la prochaine
 * occurrence possible commence au plus long bord de P[0..q-1] :
 * on passe à q = π[q-1] et on compare de nouveau le même caractère.
 *
 * Complexité : O(m) pour la table et O(n) pour une ligne,
 * soit O(n + m). Dans les deux boucles, q augmente au plus d'une
 * unité par caractère lu et chaque passage dans le « while »
 * le fait diminuer : le nombre total de retours est donc borné
 * par le nombre de caractères lus.
 *
 * Lien avec les automates : l'AFD minimal de .*P (matcher une passe)
 * est exactement l'automate de recherche de KMP. Il a m + 1 états :
 * l'état q signifie que le plus long suffixe du texte lu qui est
 * un préfixe de P a pour longueur q. KMP simule cet automate sans
 * le construire : la table π calcule à la demande les transitions
 * de repli, que l'AFD précalcule pour chaque caractère.
 */
public class Kmp implements Matcher {

    private final String motif;
    private final char[] p;
    private final int[] echec;

    /**
     * Construit la table d'échec du motif, en O(m).
     *
     * @param motif le motif littéral P
     */
    public Kmp(String motif) {
        this.motif = motif;
        this.p = motif.toCharArray();
        this.echec = calculerEchec(p);
    }

    // π[i] = longueur du plus long bord propre de p[0..i]
    private static int[] calculerEchec(char[] p) {

        int[] pi = new int[p.length];

        // k = longueur du bord courant de p[0..i-1]
        int k = 0;

        for (int i = 1; i < p.length; i++) {

            // Le bord ne se prolonge pas : on essaie le bord du bord
            while (k > 0 && p[i] != p[k]) {
                k = pi[k - 1];
            }

            if (p[i] == p[k]) {
                k++;
            }

            pi[i] = k;
        }

        return pi;
    }

    /**
     * Cherche le motif dans la ligne en O(n), sans reculer
     * dans la ligne.
     *
     * @param ligne une ligne du texte
     * @return vrai si la ligne contient le motif
     */
    @Override
    public boolean contientMotif(String ligne) {

        int m = p.length;

        if (m == 0) {
            return true;
        }

        // q = nombre de caracteres du motif deja reconnus
        int q = 0;

        for (int i = 0; i < ligne.length(); i++) {

            char c = ligne.charAt(i);

            while (q > 0 && c != p[q]) {
                q = echec[q - 1];
            }

            if (c == p[q]) {
                q++;
            }

            if (q == m) {
                return true;
            }
        }

        return false;
    }

    /** @return le motif recherché */
    public String getMotif() {
        return motif;
    }

    /** @return une copie de la table d'échec π */
    public int[] getEchec() {
        return echec.clone();
    }

    /**
     * Détecte si la regex est un motif littéral : l'AST ne contient
     * que des AstChar et des AstConcat. AstVide, AstDot, AstOr et
     * AstRepeat excluent KMP. Les parenthèses n'apparaissent pas
     * dans l'AST : « (ab)c » est donc le motif littéral « abc ».
     *
     * @param ast l'AST de la regex
     * @return le mot si la regex est littérale, sinon {@code null}
     */
    public static String motifLitteral(AstI ast) {

        StringBuilder mot = new StringBuilder();

        return collecter(ast, mot) ? mot.toString() : null;
    }

    // Ajoute les lettres de l'arbre a mot, de gauche a droite ;
    // renvoie faux des qu'un noeud n'est ni AstChar ni AstConcat
    private static boolean collecter(AstI noeud, StringBuilder mot) {

        if (noeud instanceof AstChar) {
            mot.append(((AstChar) noeud).getValeur());
            return true;
        }

        if (noeud instanceof AstConcat) {
            return collecter(noeud.getFilsGauche(), mot)
                && collecter(noeud.getFilsDroit(), mot);
        }

        return false;
    }
}

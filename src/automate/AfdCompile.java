package automate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cinquième étape du pipeline : l'AFD minimal compilé en table de
 * transitions, pour les matchers. Aucune String ni HashMap n'est
 * utilisée pendant la lecture du texte.
 *
 * <p>{@code table[etat][c]} donne l'état atteint en lisant le caractère c :
 * <ul>
 *   <li>colonnes 0 à 255 : caractères de code inférieur à 256 ;</li>
 *   <li>colonne 256 ({@link #COLONNE_AUTRE}) : tout caractère absent
 *       de l'alphabet de la regex (symbole {@link Afd#AUTRE}) ;</li>
 *   <li>{@link #AUCUNE} (-1) : pas de transition.</li>
 * </ul>
 * <p>Un caractère de code supérieur ou égal à 256 qui fait partie de
 * l'alphabet (cas rare, par exemple « ’ » dans la regex) garde sa
 * propre transition dans une petite table de secours triée.</p>
 *
 * <p>Complexité : construction en O(n · 256 · log s) pour n états et
 * un alphabet de taille s ; mémoire n × 257 entiers. Une transition
 * coûte O(1) pour un caractère de code inférieur à 256, et
 * O(log h) sinon, où h est le nombre de caractères de code
 * supérieur ou égal à 256 dans la regex.</p>
 */
public class AfdCompile {

    /** Colonne du symbole AUTRE dans la table. */
    public static final int COLONNE_AUTRE = 256;

    /** Valeur de la table quand il n'y a pas de transition. */
    public static final int AUCUNE = -1;

    private final int[][] table;
    private final boolean[] finaux;
    private final int initial;

    // Table de secours : caracteres >= 256 de l'alphabet (tries),
    // et pour chacun la transition depuis chaque etat
    private final char[] caracteresHauts;
    private final int[][] transitionsHautes;

    /**
     * Compile un AFD (en général l'AFD minimal).
     * Les états sont numérotés dans l'ordre de {@code afd.getEtats()}.
     * Pour chaque état, la colonne d'un caractère absent de l'alphabet
     * reçoit la transition sur AUTRE (ou AUCUNE si la regex n'a pas
     * de point).
     *
     * @param afd l'automate à compiler
     */
    public AfdCompile(Afd afd) {

        List<Etat> etats = afd.getEtats();
        int nombreEtats = etats.size();

        Map<Etat, Integer> indices = new HashMap<>();

        for (int i = 0; i < nombreEtats; i++) {
            indices.put(etats.get(i), i);
        }

        Set<String> alphabet = afd.getAlphabet();

        // Caracteres de code >= 256 presents dans la regex
        List<Character> hauts = new ArrayList<>();

        for (String symbole : alphabet) {

            if (!symbole.equals(Afd.AUTRE) && symbole.charAt(0) >= 256) {
                hauts.add(symbole.charAt(0));
            }
        }

        Collections.sort(hauts);

        caracteresHauts = new char[hauts.size()];

        for (int k = 0; k < hauts.size(); k++) {
            caracteresHauts[k] = hauts.get(k);
        }

        table = new int[nombreEtats][COLONNE_AUTRE + 1];
        finaux = new boolean[nombreEtats];
        transitionsHautes = new int[caracteresHauts.length][nombreEtats];

        for (int i = 0; i < nombreEtats; i++) {

            Etat etat = etats.get(i);

            finaux[i] = afd.getFinaux().contains(etat);

            int autre = indice(afd.transition(etat, Afd.AUTRE), indices);

            for (int c = 0; c < COLONNE_AUTRE; c++) {

                String symbole = String.valueOf((char) c);

                table[i][c] = alphabet.contains(symbole)
                    ? indice(afd.transition(etat, symbole), indices)
                    : autre;
            }

            table[i][COLONNE_AUTRE] = autre;

            for (int k = 0; k < caracteresHauts.length; k++) {

                String symbole = String.valueOf(caracteresHauts[k]);

                transitionsHautes[k][i] =
                    indice(afd.transition(etat, symbole), indices);
            }
        }

        initial = indices.get(afd.getInitial());
    }

    // Indice d'un etat, ou AUCUNE s'il n'y a pas de transition
    private static int indice(Etat etat, Map<Etat, Integer> indices) {
        return etat == null ? AUCUNE : indices.get(etat);
    }

    /**
     * Transition depuis un état en lisant un caractère, en O(1)
     * (O(log h) pour un caractère de code supérieur ou égal à 256).
     *
     * @param etat indice de l'état courant
     * @param c    caractère lu
     * @return indice de l'état atteint, ou {@link #AUCUNE}
     */
    public int suivant(int etat, char c) {

        if (c < COLONNE_AUTRE) {
            return table[etat][c];
        }

        if (caracteresHauts.length > 0) {

            int k = Arrays.binarySearch(caracteresHauts, c);

            if (k >= 0) {
                return transitionsHautes[k][etat];
            }
        }

        return table[etat][COLONNE_AUTRE];
    }

    /**
     * Indique si un état est final.
     *
     * @param etat indice d'un état
     * @return vrai si l'état est final
     */
    public boolean estFinal(int etat) {
        return finaux[etat];
    }

    /**
     * Renvoie l'indice de l'état initial.
     *
     * @return l'indice de l'état initial
     */
    public int getInitial() {
        return initial;
    }
}

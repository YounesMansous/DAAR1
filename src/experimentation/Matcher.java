package experimentation;

/**
 * Stratégie de recherche du motif dans une ligne.
 */
public interface Matcher {

    /**
     * @param ligne une ligne du texte, sans fin de ligne
     * @return vrai si la ligne contient au moins une occurrence du motif
     */
    boolean contientMotif(String ligne);
}

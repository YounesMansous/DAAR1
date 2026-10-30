package experimentation;

/**
 * Dernière étape du pipeline : stratégie de recherche du motif dans
 * une ligne. Trois implémentations : {@link MatcherNaif} et
 * {@link MatcherUnePasse} (automates), et {@code kmp.Kmp} (motifs
 * littéraux). {@link FileSearcher} ne dépend que de cette interface.
 */
public interface Matcher {

    /**
     * Teste si la ligne contient une occurrence du motif, n'importe
     * où (comme grep, sans ancrage).
     *
     * @param ligne une ligne du texte, sans fin de ligne
     * @return vrai si la ligne contient au moins une occurrence du motif
     */
    boolean contientMotif(String ligne);
}

package experimentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Résultat d'une recherche : lignes trouvées (avec leur numéro)
 * et nombre de lignes et de caractères lus.
 */
public class ResultatRecherche {

    /**
     * Ligne qui contient le motif.
     */
    public static class LigneTrouvee {

        private final long numero;
        private final String texte;

        /**
         * @param numero numéro de la ligne (à partir de 1)
         * @param texte  contenu de la ligne, sans fin de ligne
         */
        public LigneTrouvee(long numero, String texte) {
            this.numero = numero;
            this.texte = texte;
        }

        /** @return le numéro de la ligne (à partir de 1) */
        public long getNumero() {
            return numero;
        }

        /** @return le contenu de la ligne, sans fin de ligne */
        public String getTexte() {
            return texte;
        }
    }

    private final List<LigneTrouvee> lignes = new ArrayList<>();
    private long lignesLues = 0;
    private long caracteresLus = 0;

    /**
     * Enregistre une ligne lue, trouvée ou non.
     *
     * @param ligne   contenu de la ligne
     * @param trouvee vrai si la ligne contient le motif
     */
    void ajouterLigneLue(String ligne, boolean trouvee) {
        lignesLues++;
        caracteresLus += ligne.length();

        if (trouvee) {
            lignes.add(new LigneTrouvee(lignesLues, ligne));
        }
    }

    /** @return les lignes trouvées, dans l'ordre du fichier */
    public List<LigneTrouvee> getLignes() {
        return Collections.unmodifiableList(lignes);
    }

    /** @return le nombre de lignes trouvées */
    public long getNombreTrouvees() {
        return lignes.size();
    }

    /** @return le nombre de lignes lues */
    public long getLignesLues() {
        return lignesLues;
    }

    /** @return le nombre de caractères lus (hors fins de ligne) */
    public long getCaracteresLus() {
        return caracteresLus;
    }
}

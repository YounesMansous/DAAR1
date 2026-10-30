package experimentation;

import automate.Afd;
import automate.Etat;

public class AfdMatcher {

    private final Afd afd;

    public AfdMatcher(Afd afd) {
        this.afd = afd;
    }

    // Verifier si une ligne contient le motif
    public boolean contientMotif(String ligne) {

        for (int debut = 0; debut <= ligne.length(); debut++) {

            Etat courant = afd.getInitial();

            // Cas des expressions acceptant le mot vide
            if (afd.getFinaux().contains(courant)) {
                return true;
            }

            for (int i = debut; i < ligne.length(); i++) {

                String symbole = String.valueOf(ligne.charAt(i));

                // Un caractere absent de la regex ne peut etre
                // lu que par un point : il devient AUTRE
                if (!afd.getAlphabet().contains(symbole)) {
                    symbole = Afd.AUTRE;
                }

                courant = afd.transition(courant, symbole);

                if (courant == null) {
                    break;
                }

                if (afd.getFinaux().contains(courant)) {
                    return true;
                }
            }
        }

        return false;
    }
}

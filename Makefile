# Raccourcis vers les cibles Ant de build.xml, pour ceux qui tapent make.
# make        : jar exécutable egrep-clone.jar (cible par défaut)
# make test   : TestKmp, puis tests/verifier.sh s'il existe

ANT ?= ant

.PHONY: all compile jar test clean bench

all: jar

compile jar test clean bench:
	$(ANT) $@

# Budget du jour

Application Android simple pour savoir **combien tu peux dépenser par jour** jusqu'à ta prochaine paie.

## Utilisation

1. Dans ⚙️ **Réglages**, indique ton **jour de paie** (ex. 1 ou 28) et, si tu veux, une **somme à garder de côté**.
2. Appuie sur **Mettre à jour mon solde** et entre ce qu'il reste sur ton compte. C'est tout !
3. L'app affiche :
   - ce que tu peux **encore dépenser aujourd'hui** ;
   - ton **budget par jour** = (solde − réserve) ÷ jours restants avant la paie (aujourd'hui inclus) ;
   - combien tu auras par jour **à partir de demain** si tu ne dépenses plus rien.

Tu peux aussi saisir une **dépense** (elle est retirée du solde) ou un **revenu** comme ton salaire (il est ajouté),
mais mettre à jour le solde de temps en temps suffit : l'app déduit toute seule ce que tu as dépensé dans la journée.
Le budget du jour est figé à partir du solde en début de journée, donc il ne baisse pas au fil de tes achats.

Toutes les données restent sur le téléphone.

## Télécharger l'APK

Chaque push déclenche le workflow GitHub Actions **Build APK**, qui publie l'APK dans l'onglet
**Releases** du dépôt (et en artefact du workflow). Sur le téléphone, télécharge le `.apk`, ouvre-le et
autorise l'installation depuis cette source si Android le demande.

Les APK sont signés avec la clé `app/dailybudget.keystore` du dépôt, ce qui permet d'installer chaque nouvelle
version par-dessus l'ancienne sans perdre les données.

## Compiler soi-même

Avec le SDK Android installé (JDK 17+) :

```
./gradlew assembleRelease
```

L'APK se trouve dans `app/build/outputs/apk/release/`.

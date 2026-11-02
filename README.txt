Meine Arbeitszeiten Pro - Android-Projekt

Neues GitHub-Repository anlegen, Inhalt dieses Ordners inklusive .github hochladen.
GitHub Actions -> Android Pro APK erstellen -> grüner Haken -> Artifacts -> ZIP -> APK.

Wichtig: Vor produktivem Einsatz testen. Automatische Erfassung läuft mit Android-Vordergrunddienst und persistierten Zeitstempeln. Android kann den Dienst bei bestimmten Energiesparbedingungen beenden; beim erneuten Öffnen werden Zeitstempel rekonstruiert. Start/Pause/Ende müssen in der App gedrückt werden.
Die Benachrichtigung zeigt den Status, nicht sekundengenau die Zeit. Nach Feierabend wird ein Datensatz gespeichert. Keine Schichten >=24h.
Die bestehende App bleibt wegen eigener applicationId separat installiert.

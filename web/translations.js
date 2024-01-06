/* 
 * (c) 2023 by Matthias Thiele
 * GNU General Public License v3.0
 */

var TxtDe = {};
TxtDe.home = "Aufgabenliste";
TxtDe.editusers = "Anwenderverwaltung";
TxtDe.editsystem = "Systemeinstellungen";
TxtDe.editlocks = "Sperrliste verwalten";
TxtDe.editpwreq = "Passwortanfragen";
TxtDe.about = "Über das Programm";
TxtDe.logout = "Abmelden";
TxtDe.username = "Anwendername";
TxtDe.pasword = "Passwort";
TxtDe.newpwd = "Passwort Neu";
TxtDe.pwdagain = "Passwort Neu";
TxtDe.pwdsend = "Absenden";
TxtDe.pwdreq = "Passwort vergessen?";
TxtDe.wait = "Warte auf Serverantwort...";
TxtDe.dologin = "Anmelden";
TxtDe.pwdupdateheader = "TaMM - Neues Passwort vergeben";
TxtDe.pwdnotequal = "Passwortwiederholung nicht identisch.";
TxtDe.pwdreqheader = "Passwort anfordern";
TxtDe.pwdreqmessagehdr = "Über diesen Dialog können Sie ein neues Passwort anfordern.";
TxtDe.pwdreqmessage = "Der Name und die registrierte Mailadresse müssen korrekt eingetragen werden. Sie erhalten dann per EMail einen Link zur Eingabe eines neuen Passworts.";
TxtDe.mailaddr = "Mailadresse";
TxtDe.sendreq = "Anfordern";
TxtDe.reqlistheader = "TaMM Passwortanfragen";
TxtDe.clearrequest = "Anfrage löschen";
TxtDe.validthru = "Gültig bis";
TxtDe.ipaddress = "IP Adresse";
TxtDe.accesskey = "Zugriffsschlüssel";

var TxtFr = {};
TxtFr.home = "Liste des tâches" ;
TxtFr.editusers = "Gestion des utilisateurs";
TxtFr.editsystem = "Paramètres système" ;
TxtFr.editlocks = "Gérer la liste noire";
TxtFr.editpwreq = "Demandes de mot de passe" ;
TxtFr.about = "A propos du programme";
TxtFr.logout = "Déconnexion" ;
TxtFr.username = "Nom d'utilisateur" ;
TxtFr.password = "Mot de passe" ;
TxtFr.pwdreq = "Mot de passe oublié ?";
TxtFr.wait = "En attente de réponse du serveur...";
TxtFr.dologin = "Registre";
TxtFr.newpwd = "Nouveau mot de passe" ;
TxtFr.pwdagain = "Nouveau mot de passe" ;
TxtFr.pwdsend = "Envoyer" ;
TxtFr.pwdupdateheader = "TaMM - Attribuer un nouveau mot de passe";
TxtFr.pwdnotequal = "Répétition du mot de passe non identique.";
TxtFr.pwdreqheader = "Demander le mot de passe";
TxtFr.pwdreqmessagehdr = "Vous pouvez demander un nouveau mot de passe en utilisant cette boîte de dialogue.";
TxtFr.pwdreqmessage = "Le nom et l'adresse email enregistrée doivent être saisis correctement. Vous recevrez alors un email avec un lien pour saisir un nouveau mot de passe.";
TxtFr.mailaddr = "Adresse mail";
TxtFr.sendreq = "Demande" ;
TxtFr.reqlistheader = "Demandes de mot de passe TaMM";
TxtFr.clearrequest = "Effacer la demande";
TxtFr.validthru = "Valide jusqu'à" ;
TxtFr.ipaddress = "Adresse IP" ;
TxtFr.accesskey = "Clé d'accès" ;

var TxtEn = {};
TxtEn.home = "Task List";
TxtEn.editusers = "User management";
TxtEn.editsystem = "System Settings";
TxtEn.editlocks = "Manage blacklist";
TxtEn.editpwreq = "Password Requests";
TxtEn.about = "About the program";
TxtEn.logout = "Logout";
TxtEn.username = "User name";
TxtEn.password = "Password";
TxtEn.newpwd = "New password";
TxtEn.pwdagain = "New password";
TxtEn.pwdsend = "Send";
TxtEn.pwdupdateheader = "TaMM - Enter new password";
TxtEn.pwdnotequal = "Password repetition not identical.";
TxtEn.pwdreqheader = "Request password";
TxtEn.pwdreqmessagehdr = "You can request a new password using this dialog.";
TxtEn.pwdreqmessage = "The name and registered email address must be entered correctly. You will then receive an email with a link to enter a new password.";
TxtEn.mailaddr = "Mail address";
TxtEn.sendreq = "Send Request";
TxtEn.reqlistheader = "TaMM password requests";
TxtEn.clearrequest = "Clear request";
TxtEn.validthru = "Valid until";
TxtEn.ipaddress = "IP Address";
TxtEn.accesskey = "Access Key";
TxtEn.pwdreq = "Password forgotten?";
TxtEn.wait = "Wait for server response...";
TxtEn.dologin = "Login";

var Txt;

function translateForm() {
    var userLang = navigator.language || navigator.userLanguage;
userLang = "en";
    switch (userLang) {
        case "de": Txt = TxtDe; break;
        case "fr": Txt = TxtFr; break;
        default: Txt = TxtEn;
    }
    
    if (userLang === "de") {
        return;
    }
    
    var textElems = document.querySelectorAll("[data-key]");
    textElems.forEach((elem) => {
        var key = elem.getAttribute("data-key");
        var translate = Txt[key];
        if (translate) {
            elem.innerText = translate;
        }
    });
}
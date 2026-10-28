/** Translate known codes only. Never display server payloads or credentials.
 * @param {unknown} error @param {'room' | 'sync' | 'submit'} context
 */
export function apiErrorText(error, context) {
  if (error instanceof Error && error.message === 'REQUEST_ABORTED') return ''
  if (error instanceof Error && error.message === 'HTTP:INVALID_RESPONSE_BODY' && 'responseBodyState' in error) {
    if (error.responseBodyState === 'empty') return 'Le serveur a renvoyé une réponse vide. Actualisez avant de réessayer.'
    if (error.responseBodyState === 'unreadable') return 'La réponse du serveur est illisible. Actualisez avant de réessayer.'
  }
  const code = error instanceof Error ? error.message.replace(/^(ENGINE|RULESET|HTTP):/, '') : ''
  const messages = {
    ROOM_RATE_LIMITED: 'Trop de tentatives rapprochées. Patientez un moment avant de réessayer.',
    ROOM_CODE_INVALID: 'Code d’accès incorrect.',
    ROOM_FULL: 'Cette partie a déjà deux joueurs.',
    ROOM_NOT_FOUND: 'Cette partie est introuvable ou n’est plus disponible. Les parties sont supprimées après 7 jours sans action de jeu.',
    MATCH_NOT_FOUND: 'Cette partie est introuvable ou n’est plus disponible. Les parties sont supprimées après 7 jours sans action de jeu.',
    ROOM_ACCESS_DENIED: context === 'room' ? 'Ce code de reprise n’est plus valide.' : 'Vous n’avez plus accès à cette place. Utilisez votre code de reprise.',
    ACTOR_NOT_AUTHORIZED: 'Vous n’avez plus accès à cette place. Utilisez votre code de reprise.',
    ROOM_RESUME_INVALID: 'Ce code de reprise est incorrect ou n’est plus valide.',
    ROOM_NAME_REQUIRED: 'Choisissez un nom de partie.',
    PSEUDO_REQUIRED: 'Choisissez un pseudo.',
    FIELD_TOO_LONG: 'Le nom ou le code est trop long.',
    ROOM_SERVER_ERROR: 'Le serveur n’a pas pu ouvrir cette partie.',
    SERVER_ERROR: 'Le serveur rencontre un problème. Actualisez le plateau avant de réessayer.',
    UNAUTHORIZED: 'L’accès au serveur est refusé. Rechargez la page.',
    REQUEST_TIMEOUT: 'Le serveur met trop de temps à répondre. Actualisez avant de réessayer.',
    INVALID_ROOM_RESPONSE: 'La réponse du serveur est incompatible. Réessayez ou rechargez la page.',
    INVALID_ADMISSION_RESPONSE: 'La réponse du serveur est incompatible. Votre place n’a pas été remplacée.',
    INVALID_MATCH_RESPONSE: 'Le plateau reçu est incompatible. Le dernier plateau valide reste affiché.',
    WRONG_MATCH_RESPONSE: 'Le plateau reçu ne correspond pas à cette partie.',
    INVALID_RESPONSE_BODY: 'La réponse du serveur est incompatible. Actualisez avant de renvoyer votre action.',
    INVALID_ERROR_RESPONSE: 'La réponse du serveur est incompatible. Actualisez avant de réessayer.',
    VERSION_CONFLICT: 'La partie a changé. Vérifiez le plateau actualisé avant de réessayer.',
    IDEMPOTENCY_CONFLICT: 'Cette action ne peut pas être renvoyée. Actualisez le plateau.',
    ROOM_WAITING_FOR_PLAYER: 'En attente du second joueur.',
    MATCH_ALREADY_TERMINAL: 'Cette manche est terminée. Actualisez le plateau.',
    INVALID_STATE_FOR_READY_SECRET: 'Cette action n’est pas disponible dans la phase actuelle. Actualisez le plateau.',
    INVALID_STATE_FOR_PLAY_GUESS: 'Cette action n’est pas disponible dans la phase actuelle. Actualisez le plateau.',
    INVALID_STATE_FOR_SEND_FEEDBACK: 'Cette action n’est pas disponible dans la phase actuelle. Actualisez le plateau.',
    SECRET_ALREADY_SET: 'Votre combinaison secrète est déjà enregistrée.',
    PLAYER_ALREADY_SOLVED: 'Vous avez déjà trouvé le code. Continuez à donner les indices à votre adversaire.',
    GUESS_ALREADY_SUBMITTED: 'Votre proposition est déjà enregistrée pour ce tour.',
    FEEDBACK_NOT_REQUIRED: 'Aucun indice n’est attendu de votre part pour ce tour.',
    FEEDBACK_ALREADY_SUBMITTED: 'Vos indices sont déjà enregistrés pour ce tour.',
    FEEDBACK_ALREADY_CONTESTED: 'Vous avez déjà demandé une vérification de ces indices.',
    FEEDBACK_NOT_EDITABLE: 'Seuls les indices du dernier tour terminé peuvent être corrigés ou contestés. Actualisez le plateau.',
    ABANDON_REQUIRES_STARTED_GAME: 'L’abandon sera disponible après la préparation des deux secrets.',
    REMATCH_REQUIRES_FINISHED_MATCH: 'La manche doit être terminée pour demander une revanche.',
    REMATCH_ALREADY_REQUESTED: 'Votre demande de revanche est déjà enregistrée.',
    INVALID_CODE: 'Choisissez quatre couleurs pour votre combinaison.',
    INVALID_FEEDBACK: 'Les indices doivent être des nombres entiers de zéro à quatre, avec un total maximal de quatre.',
    INVALID_FEEDBACK_TARGET: 'Ces indices ne peuvent plus être modifiés. Actualisez le plateau.'
  }
  if (Object.hasOwn(messages, code)) return messages[code]
  if (error instanceof TypeError) return 'Le serveur est inaccessible. Vérifiez votre connexion.'
  return context === 'room' ? 'Impossible de charger cette partie. Réessayez.'
    : context === 'sync' ? 'Actualisation impossible. Le dernier plateau reçu reste affiché.'
      : 'L’action n’a pas pu être confirmée. Actualisez le plateau avant de réessayer.'
}

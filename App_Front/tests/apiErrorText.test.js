import test from 'node:test'
import assert from 'node:assert/strict'
import { apiErrorText } from '../src/lib/utils/apiErrorText.js'
import { createSubmitActionClient } from '../src/lib/api/submitActionClient.js'

test('missing games explain availability and retention without claiming that this game expired', () => {
  const expected = 'Cette partie est introuvable ou n’est plus disponible. Les parties sont supprimées après 7 jours sans action de jeu.'
  for (const context of ['room', 'sync', 'submit']) {
    for (const code of ['ROOM_NOT_FOUND', 'MATCH_NOT_FOUND', 'HTTP:ROOM_NOT_FOUND', 'ENGINE:MATCH_NOT_FOUND']) {
      const error = Object.freeze(Object.assign(new Error(code), { status: 404 }))
      assert.equal(apiErrorText(error, context), expected)
      assert.equal(error.message, code)
      assert.equal(error.status, 404)
    }
  }
  assert.equal(apiErrorText(new Error('ROOM_RESUME_INVALID'), 'room'), 'Ce code de reprise est incorrect ou n’est plus valide.')
})

test('messages distinguish network, timeout, access, contract and business errors without exposing payloads', () => {
  const errors = [new TypeError('NetworkError'), new Error('REQUEST_TIMEOUT'), new Error('ROOM_ACCESS_DENIED'),
    new Error('HTTP:INVALID_MATCH_RESPONSE'), new Error('ENGINE:VERSION_CONFLICT'), new Error('RULESET:INVALID_FEEDBACK')]
  assert.equal(new Set(errors.map(error => apiErrorText(error, 'sync'))).size, errors.length)
  for (const context of ['room', 'sync', 'submit']) {
    for (const message of ['private token: secret', '__proto__', 'constructor', 'ENGINE:private token: secret']) {
      assert.equal(apiErrorText(new Error(message), context), apiErrorText(null, context))
    }
    assert.equal(apiErrorText(new Error('REQUEST_ABORTED'), context), '')
  }
  const conflict = Object.assign(new Error('ENGINE:VERSION_CONFLICT'), {code:'VERSION_CONFLICT',version:7})
  apiErrorText(conflict, 'submit')
  assert.equal(conflict.message, 'ENGINE:VERSION_CONFLICT')
  assert.equal(conflict.code, 'VERSION_CONFLICT')
  assert.equal(conflict.version, 7)
  assert.match(apiErrorText(new Error('unknown'), 'submit'), /confirmée/)
  assert.match(apiErrorText(Object.assign(new Error('HTTP:INVALID_RESPONSE_BODY'), { responseBodyState: 'empty' }), 'submit'), /réponse vide/)
  assert.match(apiErrorText(Object.assign(new Error('HTTP:INVALID_RESPONSE_BODY'), { responseBodyState: 'unreadable' }), 'submit'), /illisible/)
})

test('validated refusals explain the reason without changing retry metadata or issuing another request', async t => {
  const cases = [
    ['ROOM_WAITING_FOR_PLAYER', 409, /second joueur/],
    ['MATCH_ALREADY_TERMINAL', 409, /manche est terminée/],
    ['INVALID_STATE_FOR_READY_SECRET', 400, /phase actuelle/],
    ['INVALID_STATE_FOR_PLAY_GUESS', 400, /phase actuelle/],
    ['INVALID_STATE_FOR_SEND_FEEDBACK', 400, /phase actuelle/],
    ['SECRET_ALREADY_SET', 400, /secrète est déjà enregistrée/],
    ['PLAYER_ALREADY_SOLVED', 400, /Continuez à donner les indices/],
    ['GUESS_ALREADY_SUBMITTED', 409, /proposition est déjà enregistrée/],
    ['FEEDBACK_NOT_REQUIRED', 400, /Aucun indice n’est attendu/],
    ['FEEDBACK_ALREADY_SUBMITTED', 409, /indices sont déjà enregistrés/],
    ['FEEDBACK_ALREADY_CONTESTED', 400, /déjà demandé une vérification/],
    ['FEEDBACK_NOT_EDITABLE', 400, /dernier tour terminé peuvent être corrigés ou contestés/],
    ['ABANDON_REQUIRES_STARTED_GAME', 400, /préparation des deux secrets/],
    ['REMATCH_REQUIRES_FINISHED_MATCH', 400, /manche doit être terminée/],
    ['REMATCH_ALREADY_REQUESTED', 400, /demande de revanche est déjà enregistrée/]
  ]
  let code, status
  const fetch = t.mock.method(globalThis, 'fetch', async () => Response.json(code === 'ROOM_WAITING_FOR_PLAYER'
    ? { code }
    : { accepted: false, rejectionOrigin: 'ENGINE', rejectionCode: code,
      matchId: null, version: null, status: null, emittedEvents: [] }, { status }))
  const client = createSubmitActionClient({ baseUrl: 'http://test', apiKeyValue: 'test-only-strong-api-key' })
  for (const [nextCode, nextStatus, expected] of cases) {
    code = nextCode
    status = nextStatus
    await assert.rejects(client.submitAction({ matchId: 'board', actorId: 'p1', expectedVersion: 0,
      idempotencyKey: 'unchanged-operation', actionPayload: { actionType: 'READY_SECRET', payload: '["A","B","E","F"]' } }), error => {
      const before = { message: error.message, ...error }
      Object.freeze(error)
      const message = apiErrorText(error, 'submit')
      assert.match(message, expected)
      assert.notEqual(message, apiErrorText(null, 'submit'))
      assert.equal(error.code, code)
      assert.deepEqual({ message: error.message, ...error }, before)
      return true
    })
  }
  assert.equal(fetch.mock.callCount(), cases.length)
})

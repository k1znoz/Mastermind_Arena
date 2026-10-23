import test from 'node:test'
import assert from 'node:assert/strict'
import { sanitizeDebug, debugJson } from '../src/lib/utils/diagnostics.js'

test('diagnostics preserve useful state without retaining private fields, regardless of nesting or type', () => {
  const input = { matchId:'board', version:7, headers:{Authorization:'test-bearer', 'X-API-Key':'test-key'},
    roomToken:'test-seat', recoveryCode:'test-recovery', access_code:1234,
    visibleSecretCode:['A','B','C','D'], nested:[{ password:{ value:'test-password' }, state:'PREPARATION' }] }
  const snapshot = sanitizeDebug(input)
  assert.equal(snapshot.matchId, 'board')
  assert.equal(snapshot.version, 7)
  assert.equal(snapshot.nested[0].state, 'PREPARATION')
  for (const privateValue of ['test-bearer','test-key','test-seat','test-recovery','1234','test-password','"A"']) {
    assert.ok(!debugJson(snapshot).includes(privateValue))
  }
  assert.deepEqual(input.visibleSecretCode, ['A','B','C','D'])
  input.nested[0].state = 'CHANGED'
  assert.equal(snapshot.nested[0].state, 'PREPARATION')
})

test('diagnostic formatting handles empty snapshots and prototype-like field names safely', () => {
  assert.equal(debugJson(null), '{}')
  const value = sanitizeDebug(JSON.parse('{"__proto__":{"token":"test-private"},"version":0}'))
  assert.ok(!debugJson(value).includes('test-private'))
  assert.equal(Object.hasOwn(value, '__proto__'), true)
  assert.equal({}.token, undefined)
})

import test from 'node:test'
import assert from 'node:assert/strict'
import { createPhaseFocus } from '../src/lib/state/phaseFocus.js'

function harness(afterRender = async () => {}) {
  const context = { matchId:'board', actorId:'p1', roomToken:'test-seat', gameNumber:1, state:'PREPARATION', view:'game', activeTab:'partie' }
  const calls = []
  const elements = new Map([
    ['.secretActionActive', {}], ['.secretActionActive .secret-panel', { getBoundingClientRect: () => ({ top:200 }) }],
    ['.topbar', { getBoundingClientRect: () => ({ height:44 }) }]
  ])
  const surface = { height:844, scrollHeight:1200, scrollY:100, reducedMotion:false,
    find: selector => elements.get(selector), scrollTo: options => calls.push(options) }
  const focus = createPhaseFocus({ getContext: () => context, afterRender, getSurface: () => surface })
  return { context, calls, elements, surface, ...focus }
}

test('phase scrolling preserves header offset, avoids repeated polling scrolls and resets for a new game', async () => {
  const h = harness()
  await h.focus(); await h.focus()
  assert.deepEqual(h.calls, [{ top:248, behavior:'smooth' }])
  h.context.gameNumber = 2
  h.surface.reducedMotion = true
  await h.focus()
  assert.deepEqual(h.calls.at(-1), { top:248, behavior:'auto' })
  h.reset(); await h.focus()
  assert.equal(h.calls.length, 3)
})

test('waiting render cannot scroll a different screen, identity or phase', async () => {
  for (const [key, value] of [['view','lobby'], ['activeTab','historique'], ['matchId','another'], ['actorId','p2'], ['roomToken','rotated'], ['state','WAITING_GUESS']]) {
    let finish
    const h = harness(() => new Promise(resolve => { finish = resolve }))
    const pending = h.focus()
    h.context[key] = value
    finish()
    await pending
    assert.equal(h.calls.length, 0)
  }
})

test('short pages, missing targets and disposal cause no scrolling or consumed phase', async () => {
  const h = harness()
  h.surface.scrollHeight = 850
  await h.focus()
  h.surface.scrollHeight = 1200
  const target = h.elements.get('.secretActionActive .secret-panel')
  h.elements.delete('.secretActionActive .secret-panel')
  await h.focus()
  h.elements.set('.secretActionActive .secret-panel', target)
  await h.focus()
  assert.equal(h.calls.length, 1)
  h.dispose(); h.reset(); await h.focus()
  assert.equal(h.calls.length, 1)
})

test('finished matches target the result and never scroll above the page origin', async () => {
  const h = harness()
  h.context.state = 'FINISHED'
  h.elements.set('.match-overview', { getBoundingClientRect: () => ({ top:-200 }) })
  await h.focus()
  assert.equal(h.calls[0].top, 0)
})

test('navigation away and back, disposal and newer requests cancel a pending render', async () => {
  for (const action of ['cancel', 'dispose']) {
    let finish
    const h = harness(() => new Promise(resolve => { finish = resolve }))
    const pending = h.focus()
    h[action]()
    finish()
    await pending
    assert.equal(h.calls.length, 0)
  }
  const renders = []
  const h = harness(() => new Promise(resolve => renders.push(resolve)))
  const old = h.focus(), latest = h.focus()
  renders[0](); await old
  assert.equal(h.calls.length, 0)
  renders[1](); await latest
  assert.equal(h.calls.length, 1)
})

test('guess and feedback transitions target the corresponding active panel', async () => {
  const h = harness()
  h.elements.delete('.secretActionActive')
  for (const [state, marker, selector] of [
    ['WAITING_GUESS','.guessActionActive','.guessActionActive .own-board'],
    ['WAITING_FEEDBACK','.feedbackActionActive','.feedbackActionActive .response-panel']
  ]) {
    h.context.state = state
    h.elements.set(marker, {})
    h.elements.set(selector, { getBoundingClientRect: () => ({ top:150 }) })
    await h.focus()
    assert.equal(h.calls.at(-1).top, 198)
    h.elements.delete(marker)
  }
  assert.equal(h.calls.length, 2)
})

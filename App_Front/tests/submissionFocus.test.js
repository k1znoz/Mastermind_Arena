import test from 'node:test'
import assert from 'node:assert/strict'
import { createSubmissionFocus } from '../src/lib/state/submissionFocus.js'

function harness(afterRender = async () => {}) {
  let context = 'game:first-seat', observer, focused = true
  const origin = { isConnected: true }, body = {}, heading = { focus() { active = heading } }
  let active = origin
  const focus = createSubmissionFocus({ getContext: () => context, afterRender,
    getSurface: () => ({ active: () => active, hasFocus: () => focused, isBodyActive: () => active === body,
      find: selector => selector === 'phase' ? heading : null,
      watch: callback => { observer = callback; return () => { observer = null } } }) })
  return { focus, origin, heading, body, active: () => active,
    move(target) { active = target; observer?.(target) }, detach() { origin.isConnected = false; active = body },
    pointer(target) { observer?.(target) },
    context(value) { context = value }, blur() { focused = false } }
}

test('a refused action keeps its trigger; a confirmed phase can replace the removed trigger', async () => {
  const h = harness()
  await h.focus.begin()()
  assert.equal(h.active(), h.origin)
  const complete = h.focus.begin()
  h.detach()
  await complete('phase')
  assert.equal(h.active(), h.heading)
})

test('moving focus away and back prevents an action from stealing it after completion', async () => {
  const h = harness()
  const complete = h.focus.begin()
  h.move({})
  h.move(h.origin)
  await complete('phase')
  assert.equal(h.active(), h.origin)
})

test('context changes, cancellation and newer actions invalidate old focus work', async () => {
  const h = harness()
  let complete = h.focus.begin()
  h.context('game:other-seat')
  await complete('phase')
  assert.equal(h.active(), h.origin)
  complete = h.focus.begin()
  h.focus.cancel()
  await complete('phase')
  assert.equal(h.active(), h.origin)
  const old = h.focus.begin(), current = h.focus.begin()
  await old('phase')
  assert.equal(h.active(), h.origin)
  await current('phase')
  assert.equal(h.active(), h.heading)
})

test('admission can name its new context but must not cross a later navigation or browser blur', async () => {
  const h = harness()
  const complete = h.focus.begin()
  h.context('waiting:new-seat')
  h.detach()
  await complete('phase', 'waiting:new-seat')
  assert.equal(h.active(), h.heading)
  const other = harness()
  const blurred = other.focus.begin()
  other.blur()
  await blurred('phase')
  assert.equal(other.active(), other.origin)
})

test('focus movement while rendering also cancels the final transfer', async () => {
  let release
  const h = harness(() => new Promise(resolve => { release = resolve }))
  const complete = h.focus.begin()
  const pending = complete('phase')
  const elsewhere = {}
  h.move(elsewhere)
  release()
  await pending
  assert.equal(h.active(), elsewhere)
})

test('clicking elsewhere cancels completion even if the trigger is subsequently removed', async () => {
  const h = harness()
  const complete = h.focus.begin()
  h.pointer({})
  h.detach()
  await complete('phase')
  assert.equal(h.active(), h.body)
})

import test from 'node:test'
import assert from 'node:assert/strict'
import { referencedTurnNumber } from '../src/lib/utils/presentation.js'

test('feedback notifications show the target round even when requested during a later round',()=>{
  const actions=[{version:5,turnNumber:2}]
  assert.equal(referencedTurnNumber({turnNumber:3,feedbackVersion:5},actions),2)
  assert.equal(referencedTurnNumber({turnNumber:3,feedbackVersion:99},actions),3)
  assert.equal(referencedTurnNumber({turnNumber:3},actions),3)
})

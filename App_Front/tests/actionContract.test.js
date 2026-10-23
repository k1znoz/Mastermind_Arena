import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { decodeTurnContent, validateSubmitError } from '../src/lib/api/responseContract.js'
import { createSubmitActionClient } from '../src/lib/api/submitActionClient.js'
import { createMatchStateClient } from '../src/lib/api/matchStateClient.js'

const feedback = counts => ({actionType:'SEND_FEEDBACK', guess:null, feedback:JSON.stringify(counts), version:5})
const conflict = () => ({accepted:false, rejectionOrigin:'ENGINE', rejectionCode:'VERSION_CONFLICT', matchId:null, version:null, status:null, emittedEvents:[]})

test('manual hints are decoded exactly, never truncated, clamped or replaced by zero', () => {
  assert.deepEqual(decodeTurnContent(feedback({bienPlaces:0,malPlaces:0})).feedback, [])
  assert.deepEqual(decodeTurnContent(feedback({bienPlaces:2,malPlaces:1})).feedback, ['BLACK','BLACK','WHITE'])
  for (const counts of [null, [], {}, {bienPlaces:1}, {bienPlaces:'2',malPlaces:0}, {bienPlaces:-1,malPlaces:0},
    {bienPlaces:1.5,malPlaces:0}, {bienPlaces:3,malPlaces:2}, {bienPlaces:0,malPlaces:5}]) {
    assert.throws(() => decodeTurnContent(feedback(counts)), /INVALID_MATCH_RESPONSE/)
  }
  assert.throws(() => decodeTurnContent({...feedback({}),feedback:'{'}), /INVALID_MATCH_RESPONSE/)
})

test('guesses and notification targets obey their own wire shapes', () => {
  const guess = {actionType:'PLAY_GUESS',guess:'["F","F","E","A"]',feedback:null,version:4}
  assert.deepEqual(decodeTurnContent(guess).guess, ['F','F','E','A'])
  for (const raw of ['{}','null','["A"]','["A","B","C","G"]','["A",["B"],"C","D"]','not json']) {
    assert.throws(() => decodeTurnContent({...guess,guess:raw}), /INVALID_MATCH_RESPONSE/)
  }
  const notice = {actionType:'CONTEST_FEEDBACK',guess:'{"feedbackVersion":5}',feedback:null,version:8}
  assert.equal(decodeTurnContent(notice).feedbackVersion, 5)
  for (const target of [0,-1,1.5,'5',8,9]) {
    assert.throws(() => decodeTurnContent({...notice,guess:JSON.stringify({feedbackVersion:target})}), /INVALID_MATCH_RESPONSE/)
  }
  assert.throws(() => decodeTurnContent({...guess,actionType:'READY_SECRET'}), /INVALID_MATCH_RESPONSE/)
})

test('malformed nested feedback rejects the entire snapshot, rather than rendering empty hints', async t => {
  const state = JSON.parse(await readFile(new URL('../../deduction-engine/src/test/resources/contracts/match-preparation.json', import.meta.url),'utf8'))
  t.mock.method(globalThis,'fetch',async()=>Response.json({...state,version:5,turns:[{...feedback({bienPlaces:9,malPlaces:0}),actorId:'p1',turnNumber:1,timestamp:1000}]}))
  const client = createMatchStateClient({baseUrl:'http://test',apiKeyValue:'test-only-strong-api-key'})
  await assert.rejects(client.getMatchState('contract-room','p1'), /INVALID_MATCH_RESPONSE/)
})

test('reject metadata must have a stable code and belong to this game when state is supplied', () => {
  assert.deepEqual(validateSubmitError(conflict(),'board'), {code:'VERSION_CONFLICT',rejectionOrigin:'ENGINE'})
  assert.deepEqual(validateSubmitError({code:'ROOM_ACCESS_DENIED'},'board'), {code:'ROOM_ACCESS_DENIED',rejectionOrigin:'HTTP'})
  assert.deepEqual(validateSubmitError({...conflict(),matchId:'board',version:7,status:'WAITING_GUESS'},'board'),
    {code:'VERSION_CONFLICT',rejectionOrigin:'ENGINE',version:7,matchStatus:'WAITING_GUESS'})
  for (const body of [null, [], {}, {code:'private arbitrary payload'}, {...conflict(),accepted:true},
    {...conflict(),rejectionOrigin:'invalid'}, {...conflict(),matchId:'other',version:7,status:'WAITING_GUESS'},
    {...conflict(),matchId:'board',version:-1,status:'WAITING_GUESS'}, {...conflict(),matchId:'board',version:7,status:'UP'}]) {
    assert.throws(()=>validateSubmitError(body,'board'), /INVALID_ERROR_RESPONSE/)
  }
})

test('transport only exposes validated errors and never promotes an invalid conflict into retry metadata', async t => {
  let body = conflict(), status = 409
  t.mock.method(globalThis,'fetch',async()=>Response.json(body,{status}))
  const client = createSubmitActionClient({baseUrl:'http://test',apiKeyValue:'test-only-strong-api-key'})
  const request = {matchId:'board',actorId:'p1',expectedVersion:0,actionPayload:{actionType:'PLAY_GUESS'}}
  await assert.rejects(client.submitAction(request), error=>error.code==='VERSION_CONFLICT' && error.rejectionOrigin==='ENGINE')
  for (const malformed of [{...conflict(),matchId:'other',version:7,status:'WAITING_GUESS'},{code:'VERSION_CONFLICT'},{code:'arbitrary payload'}]) {
    body = malformed
    await assert.rejects(client.submitAction(request), /INVALID_ERROR_RESPONSE/)
  }
  body = conflict(); status = 500
  await assert.rejects(client.submitAction(request), /INVALID_ERROR_RESPONSE/)
})

test('correction and version-conflict JSON fixtures agree with the Java wire records and mapper', async t => {
  const fixture = async name=>JSON.parse(await readFile(new URL(`../../deduction-engine/src/test/resources/contracts/${name}.json`,import.meta.url),'utf8'))
  const action = await fixture('turn-correction')
  assert.deepEqual(decodeTurnContent(action), {guess:[],feedback:['BLACK','BLACK','WHITE'],feedbackVersion:5})
  assert.deepEqual(validateSubmitError(await fixture('action-version-conflict'),'board'), {code:'VERSION_CONFLICT',rejectionOrigin:'ENGINE'})
  const preparation = await fixture('match-preparation')
  const state = {...preparation,version:8,state:'WAITING_GUESS',roundNumber:1,turns:[
    {actorId:'p1',actionType:'PLAY_GUESS',guess:'["A","B","E","F"]',feedback:null,version:3,turnNumber:1,timestamp:100},
    {actorId:'p1',turnNumber:1,timestamp:200,...feedback({bienPlaces:2,malPlaces:1})},action]}
  t.mock.method(globalThis,'fetch',async()=>Response.json(state))
  const client = createMatchStateClient({baseUrl:'http://test',apiKeyValue:'test-only-strong-api-key'})
  const decoded = await client.getMatchState('contract-room','p1')
  assert.deepEqual(decoded.turns[0].guess,['A','B','E','F'])
  assert.deepEqual(decoded.turns[1].feedback,['BLACK','BLACK','WHITE'])
  assert.equal(decoded.turns[2].feedbackVersion,5)
})

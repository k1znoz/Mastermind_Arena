import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { validateRoomInfo, validateRoomList, validateAdmission, validateSubmitResponse } from '../src/lib/api/responseContract.js'
import { createRoomClient } from '../src/lib/api/roomClient.js'
import { createSubmitActionClient } from '../src/lib/api/submitActionClient.js'
import { createRoomConnection, roomErrorText } from '../src/lib/state/roomConnection.js'

const room = () => ({roomId:'board', name:'Duel', hostPseudo:'Alice', guestPseudo:null, joined:false, locked:false, createdAt:1})

test('room metadata refuses wrong identities and inconsistent types instead of changing the waiting screen', () => {
  assert.equal(validateRoomInfo(room(), 'board').roomId, 'board')
  for (const patch of [{roomId:'other'}, {name:null}, {locked:'false'}, {createdAt:-1}, {createdAt:1.5},
    {guestPseudo:'Bob'}, {joined:true}, {hostPseudo:''}, {guestPseudo:[]}]) {
    assert.throws(() => validateRoomInfo({...room(), ...patch}, 'board'), /INVALID_ROOM_RESPONSE/)
  }
})

test('empty room lists are valid, malformed, duplicate or already joined entries are rejected', () => {
  assert.deepEqual(validateRoomList({rooms:[]}), [])
  for (const body of [null, {}, {rooms:[{}]}, {rooms:[room(), room()]}, {rooms:[{...room(), joined:true, guestPseudo:'Bob'}]}]) {
    assert.throws(() => validateRoomList(body), /INVALID_ROOM_RESPONSE/)
  }
})

test('admission binds the returned seat to the requested identity', () => {
  const admission = { room:room(), actorId:'p1', token:'test-seat' }
  for (const expected of [{roomId:'other'}, {actorId:'p2'}, {token:'different'}]) {
    assert.throws(() => validateAdmission(admission, expected), /INVALID_ADMISSION/)
  }
})

test('submission success must belong to this game and advance its version', () => {
  const value = {accepted:true, matchId:'board', version:3, status:'WAITING_GUESS', rejectionCode:null, rejectionOrigin:null, emittedEvents:[]}
  assert.equal(validateSubmitResponse(value, 'board', 2), value)
  for (const patch of [{matchId:'other'}, {version:2}, {version:-1}, {status:'UP'}, {accepted:false}, {rejectionCode:'ERROR'}, {emittedEvents:null}]) {
    assert.throws(() => validateSubmitResponse({...value, ...patch}, 'board', 2), /INVALID_RESPONSE_BODY/)
  }
})

test('clients reject incompatible successful HTTP bodies rather than treating them as an empty lobby', async t => {
  const client = createRoomClient({baseUrl:'http://test'})
  t.mock.method(globalThis, 'fetch', async () => Response.json({ ...room(), roomId:'other' }))
  await assert.rejects(client.status('board'), /INVALID_ROOM_RESPONSE/)
  await assert.rejects(client.list(), /INVALID_ROOM_RESPONSE/)
})

test('front clients consume the admission and accepted-action fixtures asserted by Java', async t => {
  const admission = JSON.parse(await readFile(new URL('../../deduction-engine/src/test/resources/contracts/room-admission.json', import.meta.url), 'utf8'))
  const accepted = JSON.parse(await readFile(new URL('../../deduction-engine/src/test/resources/contracts/action-accepted.json', import.meta.url), 'utf8'))
  t.mock.method(globalThis, 'fetch', async url => Response.json(url.endsWith('/local/submit-action') ? accepted : admission))
  const rooms = createRoomClient({baseUrl:'http://test'})
  assert.deepEqual(await rooms.resume('contract-room.p1.example-test-token'), admission)
  const client = createSubmitActionClient({baseUrl:'http://test', apiKeyValue:'test-only-strong-api-key'})
  assert.deepEqual(await client.submitAction({matchId:'contract-room',actorId:'p1',expectedVersion:0,
    actionPayload:{actionType:'READY_SECRET'},idempotencyKey:'fixture-operation'}), accepted)
})

test('incompatible status cannot replace the last room and stays distinct from an absent room', async t => {
  let updates = 0, error = ''
  t.mock.method(globalThis, 'fetch', async () => Response.json({...room(), roomId:'other'}))
  const connection = createRoomConnection({getScope:()=> 'waiting:board:p1',
    getContext:()=>({matchId:'board',roomToken:'test-seat'}),
    createClient:()=>createRoomClient({baseUrl:'http://test'}),
    onRooms:()=>{},onStatus:()=>{updates++},onAdmission:()=>{},onBusy:()=>{},
    onError:message=>{error=message}})
  await connection.refreshStatus()
  assert.equal(updates, 0)
  assert.match(error, /incompatible/)
  assert.notEqual(error, roomErrorText(new Error('ROOM_NOT_FOUND')))
  assert.match(roomErrorText(new Error('ROOM_RESUME_INVALID')), /code de reprise/)
})

test('room clients bind join and recovery responses to their original request', async t => {
  const admission = {room:{...room(), joined:true, guestPseudo:'Bob'}, actorId:'p2', token:'test-seat'}
  t.mock.method(globalThis, 'fetch', async () => Response.json(admission))
  const client = createRoomClient({baseUrl:'http://test'})
  await assert.rejects(client.join('other', 'Bob', ''), /INVALID_ADMISSION/)
  await assert.rejects(client.resume('board.p1.test-seat'), /INVALID_ADMISSION/)
  await assert.rejects(client.resume('board.p2.different'), /INVALID_ADMISSION/)
  await assert.rejects(client.create('Duel', 'Alice', ''), /INVALID_ADMISSION/)
  assert.deepEqual(await client.join('board', 'Bob', ''), admission)
})

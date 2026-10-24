import assert from 'node:assert/strict'
import { spawn, spawnSync } from 'node:child_process'
import { readFile } from 'node:fs/promises'
import { delimiter, resolve } from 'node:path'
import { randomUUID } from 'node:crypto'
import { createRoomClient } from '../App_Front/src/lib/api/roomClient.js'
import { createMatchStateClient } from '../App_Front/src/lib/api/matchStateClient.js'
import { createSubmitActionClient } from '../App_Front/src/lib/api/submitActionClient.js'

// Opt-in disposable PostgreSQL only. Never load the development .env profiles.
const dbUrl = process.env.ARENA_TEST_DB_URL
assert.ok(dbUrl?.startsWith('jdbc:postgresql://127.0.0.1:'), 'Explicit loopback PostgreSQL test URL required')
assert.ok(process.env.ARENA_TEST_DB_USER && process.env.ARENA_TEST_DB_PASSWORD, 'Explicit test credentials required')
const jdbc = new URL(dbUrl.slice(5))
assert.ok(!jdbc.username && !jdbc.password && !jdbc.search && !jdbc.hash, 'Use a simple isolated JDBC test URL')
const restore = process.argv.includes('--restore')
const container = 'mastermind-restore-validation'
function docker(args) {
  const result = spawnSync('docker', args, { encoding: 'utf8', windowsHide: true })
  assert.ok(result.status === 0, 'Disposable PostgreSQL backup/restore command failed')
  return result.stdout.trim()
}
if (restore) {
  assert.ok(docker(['port', container, '5432/tcp']) === `127.0.0.1:${jdbc.port}`,
    'Restore requires the dedicated disposable container on the explicit test port')
}
const dependencies = (await readFile('deduction-engine/target/runtime-classpath.txt', 'utf8')).trim()
const config = { baseUrl: 'http://127.0.0.1:8082', apiKeyValue: randomUUID() }
const env = { ...process.env, APP_DB_URL: dbUrl, APP_DB_USER: process.env.ARENA_TEST_DB_USER,
  APP_DB_PASSWORD: process.env.ARENA_TEST_DB_PASSWORD, APP_DB_SCHEMA: `restart_${randomUUID().replaceAll('-', '')}`,
  APP_HTTP_PORT: '8082', SUBMIT_ACTION_PATH: '/local/submit-action', APP_API_KEY: config.apiKeyValue,
  APP_DB_MIGRATE_ON_START: 'true',
  APP_API_KEY_HEADER: 'X-API-Key' }
let child
async function start() {
  child = spawn('java', ['-cp', resolve('deduction-engine/target/classes') + delimiter + dependencies,
    'io.mastermindarena.deduction.api.submitaction.LocalSubmitActionHttpServerMain'], { env, windowsHide: true })
  child.stderr.resume() // Do not publish arbitrary exception details or credentials.
  await new Promise((resolveReady, reject) => {
    const timer = setTimeout(() => finish(new Error('API startup timed out')), 15000)
    let output = ''
    const ready = data => {
      output = (output + data.toString()).slice(-4096)
      if (output.includes('SubmitAction local HTTP server started')) finish()
    }
    const exited = () => finish(new Error('API exited before startup'))
    const failed = () => finish(new Error('Cannot launch Java API'))
    function finish(error) {
      clearTimeout(timer)
      child.stdout.off('data', ready)
      child.off('exit', exited)
      child.off('error', failed)
      if (error) reject(error)
      else resolveReady()
    }
    child.stdout.on('data', ready)
    child.once('exit', exited)
    child.once('error', failed)
  })
  child.stdout.resume()
}
async function stop() {
  if (!child || child.exitCode !== null || child.signalCode !== null) return
  await new Promise((done, reject) => {
    const timer = setTimeout(() => reject(new Error('API did not stop')), 10000)
    child.once('exit', () => { clearTimeout(timer); done() })
    child.kill()
  })
}
try {
  await start()
  const firstPid = child.pid
  const rooms = createRoomClient(config)
  const host = await rooms.create('Restart validation', 'Alice test', '')
  const guest = await rooms.join(host.room.roomId, 'Bob test', '')
  const id = host.room.roomId
  const state = admission => createMatchStateClient({ ...config, roomToken: admission.token }).getMatchState(id, admission.actorId)
  const action = async (admission, actionType, payload = null) => {
    const request = { matchId: id, actorId: admission.actorId, expectedVersion: (await state(admission)).version,
      idempotencyKey: randomUUID(), actionPayload: { actionType, payload, feedback: null } }
    const result = await createSubmitActionClient({ ...config, roomToken: admission.token }).submitAction(request)
    return { request, result }
  }
  await action(host, 'READY_SECRET', '["A","B","E","F"]')
  await action(guest, 'READY_SECRET', '["D","D","C","C"]')
  const accepted = await action(guest, 'ABANDON')
  const before = await state(host)
  assert.ok(before.scores.p1 === 1 && before.state === 'FINISHED', 'Nonzero score required before restart')
  await stop()
  if (restore) {
    const user = process.env.ARENA_TEST_DB_USER
    const source = jdbc.pathname.slice(1)
    const target = `restored_${randomUUID().replaceAll('-', '')}`
    const archive = `/tmp/${env.APP_DB_SCHEMA}.dump`
    const countsSql = ['rooms', 'match_state', 'idempotency', 'workflow_event', 'schema_version']
      .map(table => `SELECT count(*) FROM "${env.APP_DB_SCHEMA}"."${table}";`).join(' ')
    const counts = database => docker(['exec', container, 'psql', '-U', user, '-d', database, '-At', '-c', countsSql])
    const originalCounts = counts(source)
    docker(['exec', container, 'pg_dump', '-U', user, '-d', source, '--schema', env.APP_DB_SCHEMA,
      '--format=custom', '--file', archive])
    docker(['exec', container, 'createdb', '-U', user, target])
    docker(['exec', container, 'pg_restore', '-U', user, '--dbname', target, '--no-owner', '--exit-on-error', archive])
    assert.ok(counts(target) === originalCounts, 'Restore changed table row counts')
    const restoredPurge = docker(['exec', container, 'psql', '-U', user, '-d', target, '-At', '-v', 'ON_ERROR_STOP=1',
      '-c', `CALL "${env.APP_DB_SCHEMA}".purge_expired_matches()`])
    assert.ok(restoredPurge === '0', 'Restored procedure must retain recently active matches')
    assert.ok(counts(target) === originalCounts, 'Restored purge changed recent match data')
    const restoredUrl = new URL(jdbc)
    restoredUrl.pathname = '/' + target
    env.APP_DB_URL = 'jdbc:' + restoredUrl.toString()
  }
  await start()
  assert.ok(child.pid !== firstPid, 'Restart must use a different Java process')
  for (const seat of [host, guest]) {
    const resumed = await rooms.resume(`${id}.${seat.actorId}.${seat.token}`)
    assert.ok(resumed.actorId === seat.actorId && resumed.token === seat.token, 'Private recovery changed seat')
  }
  assert.ok(JSON.stringify(await state(host)) === JSON.stringify(before), 'Restart changed persisted state')
  const replay = await createSubmitActionClient({ ...config, roomToken: guest.token }).submitAction(accepted.request)
  assert.ok(JSON.stringify(replay) === JSON.stringify(accepted.result), 'Restart lost idempotent response')
  assert.ok(JSON.stringify(await state(host)) === JSON.stringify(before), 'Replay changed state or score')
  await action(host, 'REQUEST_REMATCH')
  await action(guest, 'REQUEST_REMATCH')
  const after = await state(host)
  assert.ok(after.gameNumber === 2 && after.scores.p1 === 1 && after.state === 'PREPARATION', 'Rematch after restart failed')
  const oldReplay = await createSubmitActionClient({ ...config, roomToken: guest.token }).submitAction(accepted.request)
  assert.ok(JSON.stringify(oldReplay) === JSON.stringify(accepted.result), 'Rematch changed the restored action receipt')
  assert.ok(JSON.stringify(await state(host)) === JSON.stringify(after), 'Old receipt replay changed the new game')
  process.stdout.write(restore
    ? 'PASS: pg_dump/pg_restore to a new database; table counts, private seats, state/score, exact replay and rematch.\n'
    : 'PASS: new Java process; private seats, persisted state/score, exact replay and rematch.\n')
} finally {
  await stop()
}

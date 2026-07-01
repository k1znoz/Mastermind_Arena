import { CODE_LENGTH, SYMBOLS } from './constants'

export function pickSecretCode() {
  return Array.from({ length: CODE_LENGTH }, () => {
    const index = Math.floor(Math.random() * SYMBOLS.length)
    return SYMBOLS[index].id
  })
}

export function getFeedback(secret, guess) {
  let exact = 0
  const secretRemainder = []
  const guessRemainder = []

  for (let i = 0; i < CODE_LENGTH; i += 1) {
    if (secret[i] === guess[i]) {
      exact += 1
    } else {
      secretRemainder.push(secret[i])
      guessRemainder.push(guess[i])
    }
  }

  let close = 0
  for (const symbolId of guessRemainder) {
    const at = secretRemainder.indexOf(symbolId)
    if (at !== -1) {
      close += 1
      secretRemainder.splice(at, 1)
    }
  }

  return { exact, close }
}

export function getFeedbackPegs(feedback) {
  const pegs = []

  for (let i = 0; i < feedback.exact; i += 1) {
    pegs.push('exact')
  }

  for (let i = 0; i < feedback.close; i += 1) {
    pegs.push('close')
  }

  while (pegs.length < CODE_LENGTH) {
    pegs.push('none')
  }

  return pegs
}
